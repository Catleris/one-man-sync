package lt.oneman.sync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.*;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;

/** Passive, profile-scoped companion. All game reads are on the client thread. */
@Singleton
final class OneManCompanion {
    static final String VERSION = "0.7.1-dev";
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ConfigManager configs;
    @Inject private Gson gson;
    @Inject private ItemManager items;
    @Inject private ClientToolbar toolbar;
    @Inject private OverlayManager overlays;
    @Inject private CompanionBankOverlay overlay;
    @Inject private SlayerLabAccount account;
    @Inject private OneManSlayerHelper slayerHelper;
    @Inject private OneManSyncConfig config;
    @Inject private CompanionScreenshots screenshots;
    private CompanionPlanner planner;
    private Panel panel;
    private NavigationButton navigation;
    private volatile boolean active;
    private String profile = "", selected = "", lastSummary = "No previous session recorded.";
    private boolean focusedGoal;
    private Set<String> observedUnlocks=new HashSet<>();
    private Set<String> manual = new HashSet<>(), completed = new HashSet<>(), startingQuests = new HashSet<>();
    private Map<String,Integer> levels = new LinkedHashMap<>(), startingLevels = new LinkedHashMap<>();
    private Map<String,Long> synced = new LinkedHashMap<>();
    private String syncStatus = "No successful upload in this session.";
    private long started, startingXp, ticks;
    private int loginDelay;
    private boolean baseline;
    private CompanionDay day=new CompanionDay();
    private long lastDaySave;
    private List<CompanionPlanner.Step> route = Collections.emptyList();
    private int qp;
    private String player = "";

    void startUp(CompanionScreenshots.Directory directory) {
        ticks=0;loginDelay=10;levels.clear();completed.clear();lastSession="No active session.";
        try (Reader r = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("efficiency-catalog.json")), StandardCharsets.UTF_8)) {
            planner = new CompanionPlanner(gson.fromJson(r, CompanionPlanner.Catalog.class));
        } catch (IOException | RuntimeException error) { throw new IllegalStateException("Cannot load bundled OneMan roadmap", error); }
        active = true; baseline = false; profile = ""; synced.clear();
        screenshots.start(directory);
        overlays.add(overlay);
        SwingUtilities.invokeLater(() -> {
            if (!active) return;
            panel = new Panel();
            BufferedImage icon = new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = icon.createGraphics(); g.setColor(new Color(218,176,85));
            g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,18)); g.fillPolygon(new int[]{4,20,20,12,4},new int[]{3,3,13,22,13},5);
            g.setColor(new Color(38,42,48));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,14));g.drawString("1",8,15);g.dispose();
            PluginPanel root=new PluginPanel();root.setLayout(new BorderLayout(0,6));
            JPanel cards=new JPanel(new CardLayout());cards.add(panel,"Overview");
            if(slayerHelper.companionPanel()!=null)cards.add(slayerHelper.companionPanel(),"Slayer & Prayer");
            JComboBox<String> sections=new JComboBox<>(new String[]{"Overview","Slayer & Prayer"});
            sections.addActionListener(event->((CardLayout)cards.getLayout()).show(cards,(String)sections.getSelectedItem()));
            root.add(sections,BorderLayout.NORTH);root.add(cards,BorderLayout.CENTER);
            navigation = NavigationButton.builder().tooltip("OneMan — Roadmap, Slayer & Prayer").icon(icon).priority(6).panel(root).build();
            toolbar.addNavigation(navigation);
        });
    }
    void shutDown() {
        finish(); active=false; screenshots.stop(); overlays.remove(overlay); overlay.needed=Collections.emptySet();
        SwingUtilities.invokeLater(() -> { if(navigation!=null)toolbar.removeNavigation(navigation); navigation=null;panel=null; });
    }
    void onGameStateChanged(GameStateChanged e) {
        if(e.getGameState()==GameState.LOGGED_IN)loginDelay=10;
        if(e.getGameState()==GameState.LOGIN_SCREEN) { finish(); overlay.needed=Collections.emptySet(); baseline=false; levels.clear();completed.clear(); showLoggedOut(); }
    }
    void onGameTick(GameTick e) {
        if(!active || client.getGameState()!=GameState.LOGGED_IN || client.getLocalPlayer()==null) return;
        if(loginDelay>0){loginDelay--;return;}
        if(++ticks%5!=0) return;
        String key=configs.getRSProfileKey(); if(key==null || key.isEmpty())return;
        if(!key.equals(profile)) {
            finish(); profile=key; baseline=false; synced.clear();syncStatus="No successful upload in this session.";
            manual.clear();observedUnlocks.clear();selected="";focusedGoal=false;
            try { String savedDay=configs.getConfiguration("one-man-sync",profile,"companionDay");day=savedDay==null?new CompanionDay():gson.fromJson(savedDay,CompanionDay.class);if(day==null||day.gainedLevels==null||day.lastLevels==null||day.quests==null)day=new CompanionDay(); } catch(RuntimeException ignored) {day=new CompanionDay();}
            day.resume();
            try { String saved=configs.getConfiguration("one-man-sync",profile,"companionManual");
                if(saved!=null)manual.addAll(Arrays.asList(gson.fromJson(saved,String[].class))); } catch(RuntimeException ignored) { manual.clear(); }
            try{String unlocks=configs.getConfiguration("one-man-sync",profile,"companionObservedUnlocks");if(unlocks!=null)observedUnlocks.addAll(Arrays.asList(gson.fromJson(unlocks,String[].class)));}catch(RuntimeException ignored){observedUnlocks.clear();}
            focusedGoal=Boolean.parseBoolean(configs.getConfiguration("one-man-sync",profile,"companionFocus"));
            String goal=configs.getConfiguration("one-man-sync",profile,"companionGoal"); if(goal!=null)selected=goal;
            String summary=configs.getConfiguration("one-man-sync",profile,"companionSummary");lastSummary=summary==null?"No previous session recorded.":summary;
        }
        player=client.getLocalPlayer().getName();
        if(!baseline)day.resume();
        Map<String,Integer> nextLevels=new LinkedHashMap<>();
        for(Skill skill:Skill.values())if(skill!=Skill.OVERALL)nextLevels.put(skill.getName(),client.getRealSkillLevel(skill));
        Set<String> nextQuests=new HashSet<>();
        for(Quest q:Quest.values())try { if(q.getState(client)==QuestState.FINISHED) {
            nextQuests.add(q.name());
            for(CompanionPlanner.Step n:planner.ordered)if(normal(n.name).equals(normal(q.getName())))nextQuests.add(n.id);
        }}catch(RuntimeException ignored) { /* Unknown state remains incomplete. */ }
        if(baseline) {
            nextQuests.addAll(completed);
            for(String q:nextQuests)if(!completed.contains(q)&&planner.nodes.containsKey(q))screenshots.capture(player,profile,"Quest: "+planner.nodes.get(q).name);
            nextLevels.forEach((skill,level)-> { if(level>=99 && levels.getOrDefault(skill,99)<99)screenshots.capture(player,profile,skill+" 99"); });
        }
        levels=nextLevels;completed=nextQuests;
        if(completed.contains(selected)||manual.contains(selected))selected="";
        if(!baseline) { baseline=true;started=System.currentTimeMillis();startingXp=client.getOverallExperience();startingLevels=new LinkedHashMap<>(levels);startingQuests=new HashSet<>(completed); }
        day.observe(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString(),levels,completed,client.getOverallExperience());
        if(System.currentTimeMillis()-lastDaySave>30000){saveDay();}
        if(CompanionSupplies.hasChronicle(container(InventoryID.INV),container(InventoryID.WORN),profile.equals(account.profile)?account.bank.items:Collections.emptyMap())) {
            if(observedUnlocks.add("CHRONICLE"))configs.setConfiguration("one-man-sync",profile,"companionObservedUnlocks",gson.toJson(observedUnlocks));
        }
        Set<String> confirmed=new HashSet<>(manual);confirmed.addAll(observedUnlocks);
        qp=Math.max(0,client.getVarpValue(VarPlayerID.QP));
        route=planner.plan(levels,completed,confirmed,qp);
        CompanionPlanner.Step chosen=goal();
        if(focusedGoal&&chosen!=null&&CompanionTraining.achieved(chosen,levels,completed,confirmed)) {
            focusedGoal=false;selected="";configs.setConfiguration("one-man-sync",profile,"companionFocus",false);
        }
        if(selected.isEmpty() && !route.isEmpty())selected=route.get(0).id;
        render();
    }
    private static String normal(String s) { return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]",""); }
    private CompanionPlanner.Step goal() {
        for(CompanionPlanner.Step s:route)if(s.id.equals(selected))return s;
        CompanionPlanner.Step catalog=planner.nodes.get(selected);
        return catalog!=null?catalog:planner.trainingFromId(selected);
    }
    private Map<String,Integer> container(int id) {
        Map<String,Integer> map=new LinkedHashMap<>();ItemContainer c=client.getItemContainer(id);
        if(c!=null)for(Item item:c.getItems())if(item.getId()>=0&&item.getQuantity()>0) {
            ItemComposition composition=items.getItemComposition(item.getId());
            // Noted items cannot be used as quest supplies until unnoted.
            if(composition.getNote()!=-1)continue;
            map.merge(composition.getName(),item.getQuantity(),Integer::sum);
        }
        return map;
    }
    private String preparation(CompanionPlanner.Step s) {
        Map<String,Integer> inv=container(InventoryID.INV),equipment=container(InventoryID.WORN);
        List<String> required=CompanionSupplies.forStep(s);StringBuilder text=new StringBuilder();
        boolean same=profile.equals(account.profile);
        if(CompanionTraining.isSkill(s))text.append(training(s)).append("\n\n");
        text.append(same?account.bankAge(System.currentTimeMillis()):"Bank unknown — open your bank.").append("\n\n");
        Set<Integer> ids=new HashSet<>(); ItemContainer bank=client.getItemContainer(InventoryID.BANK);
        for(String need:required) {
            int carried=count(inv,need), worn=count(equipment,need), stored=same?count(account.bank.items,need):0;
            text.append(need).append("\nInventory: ").append(carried).append(" | equipped: ").append(worn)
                .append(" | bank snapshot: ").append(same&&account.bank.seen>0?stored:"unknown").append("\n");
            int quantity=CompanionSupplies.quantity(s,need);
            text.append(carried+worn>=quantity?"READY":stored+carried+worn>=quantity?"TAKE FROM BANK":"MISSING / bank unknown")
                .append(" — need ").append(quantity).append("\n\n");
            if(bank!=null)for(Item item:bank.getItems())if(item.getId()>=0&&item.getQuantity()>0&&CompanionSupplies.matches(items.getItemComposition(item.getId()).getName(),need))ids.add(item.getId());
        }
        overlay.needed=Collections.unmodifiableSet(ids);
        if(required.isEmpty()&&!CompanionTraining.isSkill(s))text.append("No verified supply checklist for this goal yet. Open the guide; no readiness is inferred.\n");
        else text.append("Curated start items only. Open the guide for stage-specific extras, usable tool level, charges and encounters. Bank snapshots can be outdated.");
        return text.toString();
    }
    private String training(CompanionPlanner.Step s) {
        SlayerLabJournal.Session task=profile.equals(account.profile)?account.journal.active:null;
        return CompanionTraining.describe(s,levels,completed,client.getLocalPlayer().getCombatLevel(),client.getAccountType().toString(),task,
            client.getSkillExperience(Skill.SLAYER),planner.methods);
    }
    private static int count(Map<String,Integer> map,String need) { return map.entrySet().stream().filter(e->CompanionSupplies.matches(e.getKey(),need)).mapToInt(Map.Entry::getValue).sum(); }
    private String summary() {
        StringBuilder text=new StringBuilder(day.text(planner.nodes)+"\nSession started: "+Instant.ofEpochMilli(started)+"\n");
        text.append("Observed XP: ").append(Math.max(0,client.getOverallExperience()-startingXp)).append("\n");
        levels.forEach((s,l)-> { int before=startingLevels.getOrDefault(s,l);if(l>before)text.append(s).append(": ").append(before).append(" → ").append(l).append("\n"); });
        for(CompanionPlanner.Step n:planner.ordered)if(completed.contains(n.id)&&!startingQuests.contains(n.id))text.append("Quest: ").append(n.name).append("\n");
        CompanionPlanner.Step next=goal();text.append("\nContinue next time: ").append(next==null?"choose a goal":next.name);
        if(profile.equals(account.profile)) {
            List<SlayerLabJournal.Session> sessions=new ArrayList<>(account.journal.history);
            if(account.journal.active!=null)sessions.add(account.journal.active);
            int credits=0,kills=0,xp=0;Map<String,Integer> loot=new TreeMap<>();
            for(SlayerLabJournal.Session session:sessions)if(session.started>=started) {credits+=session.credits;kills+=session.kills;xp+=session.slayerXp;session.loot.forEach((k,v)->loot.merge(k,v,Integer::sum));}
            text.append("\nSession Slayer: ").append(credits).append(" credits, ").append(kills).append(" attributed kills, ").append(xp).append(" XP\nObserved loot: ").append(loot);
        }
        text.append("\nOnly progress observed while this plugin was running is counted.");return text.toString();
    }
    private void finish() {
        if(!baseline||profile.isEmpty())return;
        // Use last sampled XP on logout; the client may already be resetting.
        lastSummary=lastSession;
        configs.setConfiguration("one-man-sync",profile,"companionSummary",lastSummary);
        saveDay();day.resume();
        baseline=false;
    }
    private void saveDay(){if(!profile.isEmpty()){configs.setConfiguration("one-man-sync",profile,"companionDay",gson.toJson(day));lastDaySave=System.currentTimeMillis();}}
    private String lastSession="No active session.";
    void syncResult(String sentProfile,JsonObject snapshot,boolean success,String status) {
        clientThread.invokeLater(() -> {
            if(!active||!profile.equals(sentProfile))return;
            syncStatus=status;
            if(success) {
                long now=System.currentTimeMillis();
                if(snapshot.has("skills"))synced.put("Skills",now);
                if(snapshot.has("quests"))synced.put("Quests",now);
                if(snapshot.has("slayer")&&snapshot.get("slayer").isJsonObject()&&snapshot.getAsJsonObject("slayer").has("lastTaskSeenEpochMs")
                    && snapshot.getAsJsonObject("slayer").get("lastTaskSeenEpochMs").getAsLong()>0)synced.put("Slayer",now);
                if(snapshot.has("bankSnapshotComplete")&&snapshot.get("bankSnapshotComplete").getAsBoolean())synced.put("Bank",snapshot.get("bankSnapshotEpochMs").getAsLong());
            }
        });
    }
    private void render() {
        CompanionPlanner.Step selectedGoal=goal();
        Set<String> confirmed=new HashSet<>(manual);confirmed.addAll(observedUnlocks);
        List<CompanionPlanner.Step> visible=focusedGoal&&selectedGoal!=null?planner.forGoal(selectedGoal,levels,completed,confirmed,qp):route;
        StringBuilder next=new StringBuilder("Player: "+player+" · "+client.getAccountType()+"\nCombat: "+client.getLocalPlayer().getCombatLevel()+" · Live quest points: "+qp+"\n");
        next.append(focusedGoal&&selectedGoal!=null?"Selected goal: "+selectedGoal.name:"Default OneMan route").append("\n");
        if(observedUnlocks.contains("CHRONICLE"))next.append("Chronicle ownership observed — obtain step skipped. Check remaining charges separately.\n");
        next.append("\n");
        if(focusedGoal&&CompanionTraining.isSkill(selectedGoal))next.append(training(selectedGoal)).append("\n\n");
        int index=0;for(CompanionPlanner.Step s:visible) {
            if(++index>3)break;next.append(index).append(". ").append(s.name).append("\n")
                .append(planner.requirements(s,levels,completed,qp)).append("\nWhy now: ")
                .append(s.why.isEmpty()?"Next in the selected route":s.why.get(config.companionLanguage().equalsIgnoreCase("LT")?0:Math.min(1,s.why.size()-1))).append("\n\n");
        }
        String prep=preparation(selectedGoal);
        SlayerLabJournal.Session session=profile.equals(account.profile)?account.journal.active:null;
        if(session==null&&profile.equals(account.profile)&&!account.journal.history.isEmpty())session=account.journal.history.get(0);
        String slayer=SlayerLabJournal.metrics(session,System.currentTimeMillis());
        if(session!=null)slayer+="\nStatus: "+session.status+"\n";
        if(session!=null)slayer+="\nRemaining: "+session.remaining+"\nStarted: "+Instant.ofEpochMilli(session.started)+"\nObserved loot: "+session.loot;
        StringBuilder fresh=new StringBuilder("Plugin: "+VERSION+"\n"+syncStatus+"\n\n");
        for(String key:Arrays.asList("Skills","Slayer","Bank","Quests"))fresh.append(key).append(": ").append(synced.containsKey(key)?Instant.ofEpochMilli(synced.get(key)):"not uploaded / unknown").append("\n");
        fresh.append("\nBank time is when the bank snapshot was captured; other times are successful uploads. A successful sync is not a promise that every field changed.\n");
        lastSession=summary();String currentSummary=lastSession+"\n\nPrevious session:\n"+lastSummary;
        List<CompanionPlanner.Step> choices=new ArrayList<>(route);for(CompanionPlanner.Step s:planner.ordered)if(choices.stream().noneMatch(n->n.id.equals(s.id)))choices.add(s);
        String nextText=next.toString(),slayerText=slayer,freshText=fresh.toString(),goalId=selected;
        SwingUtilities.invokeLater(() -> { if(active&&panel!=null)panel.update(nextText,prep,slayerText,freshText,currentSummary,choices,goalId,screenshots.status()); });
    }
    private void showLoggedOut() {
        SwingUtilities.invokeLater(()-> { if(active&&panel!=null) { panel.next.setText("Log in to calculate your next steps.");panel.prep.setText("Log in and open your bank.");panel.summary.setText(lastSummary);panel.slayer.setText("Logged out. The last observed Slayer session is archived in OneMan → Slayer & Prayer → History."); } });
    }
    private final class Panel extends PluginPanel {
        final JTextArea next=area(),prep=area(),slayer=area(),fresh=area(),summary=area(),memories=area();
        final JComboBox<CompanionPlanner.Step> goals=new JComboBox<>();boolean updating;
        Panel() {
            setLayout(new BorderLayout());JTabbedPane tabs=new JTabbedPane();
            tabs.addTab("Next",new JScrollPane(next));
            JPanel preparation=new JPanel(new BorderLayout());preparation.add(goals,BorderLayout.NORTH);preparation.add(new JScrollPane(prep),BorderLayout.CENTER);
            JPanel actions=new JPanel(new GridLayout(0,1));JButton guide=new JButton("Open goal guide"),mark=new JButton("Confirm manual unlock"),capture=new JButton("Save progress screenshot");
            JButton reset=new JButton("Follow default roadmap");actions.add(reset);
            reset.addActionListener(e->clientThread.invokeLater(()->{focusedGoal=false;selected="";configs.setConfiguration("one-man-sync",profile,"companionFocus",false);}));
            actions.add(guide);actions.add(mark);preparation.add(actions,BorderLayout.SOUTH);tabs.addTab("Prep",preparation);
            tabs.addTab("Slayer",new JScrollPane(slayer));tabs.addTab("Sync",new JScrollPane(fresh));
            JPanel memoryPanel=new JPanel(new BorderLayout());memoryPanel.add(new JScrollPane(memories),BorderLayout.CENTER);memoryPanel.add(capture,BorderLayout.SOUTH);tabs.addTab("Memories",memoryPanel);
            tabs.addTab("Summary",new JScrollPane(summary));add(tabs,BorderLayout.CENTER);
            goals.addActionListener(e-> { if(updating)return;CompanionPlanner.Step s=(CompanionPlanner.Step)goals.getSelectedItem();if(s!=null)clientThread.invokeLater(()-> { if(!baseline)return;selected=s.id;focusedGoal=true;configs.setConfiguration("one-man-sync",profile,"companionFocus",true);configs.setConfiguration("one-man-sync",profile,"companionGoal",selected);render(); }); });
            guide.addActionListener(e-> { CompanionPlanner.Step s=(CompanionPlanner.Step)goals.getSelectedItem();if(s!=null&&s.wiki!=null&&s.wiki.startsWith("https://oldschool.runescape.wiki/"))LinkBrowser.browse(s.wiki); });
            mark.addActionListener(e->clientThread.invokeLater(()-> { CompanionPlanner.Step s=goal();if(baseline&&s!=null&&"unlock".equals(s.type)) {manual.add(s.id);configs.setConfiguration("one-man-sync",profile,"companionManual",gson.toJson(manual));selected="";} }));
            capture.addActionListener(e->clientThread.invokeLater(()-> { if(baseline)screenshots.capture(player,profile,"Progress"); }));
        }
        void update(String n,String p,String s,String f,String sum,List<CompanionPlanner.Step> choices,String id,String status) {
            next.setText(n);prep.setText(p);slayer.setText(s);fresh.setText(f);summary.setText(sum);memories.setText(status);
            CompanionPlanner.Step old=(CompanionPlanner.Step)goals.getSelectedItem();
            if(old==null||!old.id.equals(id)||goals.getItemCount()!=choices.size()) {
                updating=true;goals.removeAllItems();for(CompanionPlanner.Step choice:choices) {goals.addItem(choice);if(choice.id.equals(id))goals.setSelectedItem(choice);}updating=false;
            }
        }
    }
    private static JTextArea area() { JTextArea a=new JTextArea();a.setEditable(false);a.setLineWrap(true);a.setWrapStyleWord(true);a.setMargin(new Insets(8,8,8,8));return a; }
}
