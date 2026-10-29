package lt.oneman.sync;

import com.google.gson.Gson;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
    static final String VERSION = "0.11.0-alarms-test";
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
    @Inject private FarmingRunTracker farmingTracker;
    @Inject private ClueHub clueHub;
    @Inject private MonstersHub monstersHub;
    @Inject private UnlocksHub unlocksHub;
    @Inject private CombatAchievementsHub combatHub;
    @Inject private AchievementAlarms achievementAlarms;
    @Inject private OneManSyncConfig config;
    @Inject private CompanionScreenshots screenshots;
    private CompanionPlanner planner;
    private Panel panel;
    private NavigationButton navigation;
    private volatile boolean active;
    private String profile = "", selected = "";
    private JComboBox<String> hubSelector;
    private boolean focusedGoal;
    private Set<String> observedUnlocks=new HashSet<>();
    private Set<String> manual = new HashSet<>(), completed = new HashSet<>();
    private Map<String,Integer> levels = new LinkedHashMap<>();
    private long ticks;
    private int loginDelay;
    private boolean baseline;
    private List<CompanionPlanner.Step> route = Collections.emptyList();
    private int qp;
    private String player = "";

    void startUp(CompanionScreenshots.Directory directory, Runnable openSettings) {
        ticks=0;loginDelay=10;levels.clear();completed.clear();
        try (Reader r = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("efficiency-catalog.json")), StandardCharsets.UTF_8)) {
            planner = new CompanionPlanner(gson.fromJson(r, CompanionPlanner.Catalog.class));
        } catch (IOException | RuntimeException error) { throw new IllegalStateException("Cannot load bundled OneMan roadmap", error); }
        active = true; baseline = false; profile = "";
        screenshots.start(directory);
        farmingTracker.startUp();
        overlays.add(overlay);
        SwingUtilities.invokeLater(() -> {
            if (!active) return;
            panel = new Panel();
            BufferedImage icon = OneManIcon.toolbar();
            PluginPanel root=new OneManSidebarPanel();
            JComboBox<String> sections=new JComboBox<>(new String[]{"Tools","Slayer","Farming Hub","Clue Scroll Hub","Monsters Hub","Unlocks Hub","Combat Achievements","Collection / Combat alarms"});
            hubSelector=sections;
            JPanel cards=new JPanel(new CardLayout());cards.add(toolsPanel(sections),"Tools");cards.add(panel,"Roadmap");
            if(config.companionRoadmapVisible())sections.addItem("Roadmap");
            if(slayerHelper.companionPanel()!=null)cards.add(slayerHelper.companionPanel(),"Slayer");
            cards.add(farmingTracker.companionPanel(),"Farming Hub");
            cards.add(clueHub.companionPanel(),"Clue Scroll Hub");
            cards.add(monstersHub.companionPanel(),"Monsters Hub");
            cards.add(unlocksHub.companionPanel(),"Unlocks Hub");
            cards.add(combatHub.companionPanel(),"Combat Achievements");
            cards.add(achievementAlarms.companionPanel(),"Collection / Combat alarms");
            sections.setName("oneManHubSelector");
            sections.addActionListener(event->((CardLayout)cards.getLayout()).show(cards,(String)sections.getSelectedItem()));
            JPanel hubNavigation=new JPanel(new BorderLayout(0,4));
            JLabel buildLabel=new JLabel("OneMan • "+VERSION); buildLabel.setFont(OneManCards.BODY);
            hubNavigation.add(buildLabel,BorderLayout.NORTH); hubNavigation.add(sections,BorderLayout.CENTER);
            JButton settings=new JButton("OneMan Sync settings");
            settings.setToolTipText("Open the original RuneLite configuration, including Sync Key and all Slayer options");
            settings.addActionListener(event->openSettings.run());
            hubNavigation.add(settings,BorderLayout.SOUTH);
            root.add(hubNavigation,BorderLayout.NORTH);root.add(cards,BorderLayout.CENTER);
            OneManTheme.apply(root);
            navigation = NavigationButton.builder().tooltip("OneMan tools").icon(icon).priority(6).panel(root).build();
            toolbar.addNavigation(navigation);
        });
    }
    void shutDown() {
        farmingTracker.shutDown();
        baseline=false; active=false; screenshots.stop(); overlays.remove(overlay); overlay.needed=Collections.emptySet();
        SwingUtilities.invokeLater(() -> { if(navigation!=null)toolbar.removeNavigation(navigation); navigation=null;panel=null;hubSelector=null; });
    }
    void onGameStateChanged(GameStateChanged e) {
        farmingTracker.onGameStateChanged(e);
        if(e.getGameState()==GameState.LOGGED_IN)loginDelay=10;
        if(e.getGameState()==GameState.LOGIN_SCREEN) { baseline=false; overlay.needed=Collections.emptySet(); baseline=false; levels.clear();completed.clear(); showLoggedOut(); }
    }
    void onGameTick(GameTick e) {
        farmingTracker.onGameTick(e);
        if(!active || client.getGameState()!=GameState.LOGGED_IN || client.getLocalPlayer()==null) return;
        if(loginDelay>0){loginDelay--;return;}
        if(++ticks%5!=0) return;
        String key=configs.getRSProfileKey(); if(key==null || key.isEmpty())return;
        if(!key.equals(profile)) {
            profile=key; baseline=false;
            manual.clear();observedUnlocks.clear();selected="";focusedGoal=false;
            try { String saved=configs.getConfiguration("one-man-sync",profile,"companionManual");
                if(saved!=null)manual.addAll(Arrays.asList(gson.fromJson(saved,String[].class))); } catch(RuntimeException ignored) { manual.clear(); }
            try{String unlocks=configs.getConfiguration("one-man-sync",profile,"companionObservedUnlocks");if(unlocks!=null)observedUnlocks.addAll(Arrays.asList(gson.fromJson(unlocks,String[].class)));}catch(RuntimeException ignored){observedUnlocks.clear();}
            focusedGoal=Boolean.parseBoolean(configs.getConfiguration("one-man-sync",profile,"companionFocus"));
            String goal=configs.getConfiguration("one-man-sync",profile,"companionGoal"); if(goal!=null)selected=goal;
        }
        player=client.getLocalPlayer().getName();
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
        baseline=true;
        if(CompanionSupplies.hasChronicle(container(InventoryID.INV),container(InventoryID.WORN),profile.equals(account.profile)?account.bank.items:Collections.emptyMap())) {
            if(observedUnlocks.add("CHRONICLE"))configs.setConfiguration("one-man-sync",profile,"companionObservedUnlocks",gson.toJson(observedUnlocks));
        }
        Set<String> confirmed=new HashSet<>(manual);confirmed.addAll(observedUnlocks);
        qp=Math.max(0,client.getVarpValue(VarPlayerID.QP));
        route=planner.plan(levels,completed,confirmed,qp);
        CompanionPlanner.Step chosen=goal();
        if(chosen!=null&&CompanionTraining.achieved(chosen,levels,completed,confirmed)) {
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
        Set<Integer> ids=new HashSet<>(); ItemContainer bank=client.getItemContainer(InventoryID.BANK);
        for(String need:required) {
            int carried=count(inv,need), worn=count(equipment,need), stored=same?count(account.bank.items,need):0;
            text.append(need).append("\nInventory: ").append(carried).append(" | equipped: ").append(worn)
                .append(" | bank: ").append(same&&account.bank.seen>0?stored:"unknown").append("\n");
            int quantity=CompanionSupplies.quantity(s,need);
            text.append(carried+worn>=quantity?"READY":stored+carried+worn>=quantity?"TAKE FROM BANK":"MISSING / bank unknown")
                .append(" — need ").append(quantity).append("\n\n");
            if(bank!=null)for(Item item:bank.getItems())if(item.getId()>=0&&item.getQuantity()>0&&CompanionSupplies.matches(items.getItemComposition(item.getId()).getName(),need))ids.add(item.getId());
        }
        overlay.needed=Collections.unmodifiableSet(ids);
        if(required.isEmpty()&&!CompanionTraining.isSkill(s))text.append("No verified supply checklist for this goal yet. Open the guide; no readiness is inferred.\n");
        if(!required.isEmpty())text.append("Starting items only. Guide: extra supplies and charges. Bank counts may be outdated.");
        return text.toString();
    }
    private String training(CompanionPlanner.Step s) {
        SlayerLabJournal.Session task=profile.equals(account.profile)?account.journal.active:null;
        return CompanionTraining.describe(s,levels,completed,client.getLocalPlayer().getCombatLevel(),client.getAccountType().toString(),task,
            client.getSkillExperience(Skill.SLAYER),planner.methods);
    }
    private static int count(Map<String,Integer> map,String need) { return map.entrySet().stream().filter(e->CompanionSupplies.matches(e.getKey(),need)).mapToInt(Map.Entry::getValue).sum(); }
    private void render() {
        if(!config.companionRoadmapVisible()){overlay.needed=Collections.emptySet();return;}
        CompanionPlanner.Step selectedGoal=goal();
        Set<String> confirmed=new HashSet<>(manual);confirmed.addAll(observedUnlocks);
        List<CompanionPlanner.Step> visible=focusedGoal&&selectedGoal!=null?planner.forGoal(selectedGoal,levels,completed,confirmed,qp):route;
        StringBuilder next=new StringBuilder(focusedGoal&&selectedGoal!=null?selectedGoal.name+"\n\n":"Next goals\n\n");
        if(focusedGoal&&CompanionTraining.isSkill(selectedGoal))next.append(training(selectedGoal)).append("\n\n");
        int index=0;for(CompanionPlanner.Step s:visible) {
            if(++index>2)break;next.append(index).append(". ").append(s.name).append("\n")
                .append(planner.requirements(s,levels,completed,qp)).append("\nWhy now: ")
                .append(s.why.isEmpty()?"Next in the selected route":s.why.get(config.companionLanguage().equalsIgnoreCase("LT")?0:Math.min(1,s.why.size()-1))).append("\n\n");
        }
        String prep=preparation(selectedGoal);
        List<CompanionPlanner.Step> choices=new ArrayList<>(route);for(CompanionPlanner.Step s:planner.ordered)if(choices.stream().noneMatch(n->n.id.equals(s.id)))choices.add(s);
        String nextText=next.toString(),goalId=selected;
        SwingUtilities.invokeLater(() -> { if(active&&panel!=null)panel.update(nextText,prep,choices,goalId); });
    }
    private void showLoggedOut() {
        SwingUtilities.invokeLater(()-> { if(active&&panel!=null) { panel.next.setText("Log in to calculate your next steps.");panel.prep.setText("Log in and open your bank."); } });
    }
    private JPanel toolsPanel(JComboBox<String> sections)
    {
        OneManCards tools=new OneManCards();
        String[][] choices={{"Slayer","Task preparation and locations"},{"Farming Hub","Patches, seeds and run timers"},
            {"Clue Scroll Hub","Clue items and STASH units"},{"Monsters Hub","Monster helpers"},{"Unlocks Hub","Account unlocks and requirements"},{"Combat Achievements","Tasks, bosses and progress"},{"Collection / Combat alarms","Legendary unlock notifications"}};
        tools.card(OneManCards.banner("OneMan tools","Choose a hub"));
        int[] icons={net.runelite.api.gameval.ItemID.SLAYER_HELM,net.runelite.api.gameval.ItemID.RAKE,
            net.runelite.api.gameval.ItemID.TRAIL_CLUE_EASY_SIMPLE001,net.runelite.api.gameval.ItemID.LIZARDMAN_FANG,
            net.runelite.api.gameval.ItemID.CRYSTAL_KEY,net.runelite.api.gameval.ItemID.BRONZE_SWORD,net.runelite.api.gameval.ItemID.CA_BOOK};
        for(int i=0;i<choices.length;i++) {
            String[] choice=choices[i];JPanel card=OneManCards.card();JPanel heading=new JPanel(new BorderLayout(6,0));heading.setOpaque(false);
            JLabel icon=new JLabel();icon.setPreferredSize(new Dimension(36,36));icon.setIcon(CombatHubStyle.fallback());
            net.runelite.client.util.AsyncBufferedImage image=items.getImage(icons[i]);if(image!=null)image.addTo(icon);
            JTextArea title=OneManCards.text(choice[0]);title.setFont(OneManCards.HEADING);title.setForeground(Color.WHITE);
            JButton open=new JButton("›");open.setToolTipText("Open "+choice[0]);open.setPreferredSize(new Dimension(26,30));
            if("Monsters Hub".equals(choice[0]))open.setName("monstersHubShortcut");
            open.addActionListener(event->sections.setSelectedItem(choice[0]));
            heading.add(icon,BorderLayout.WEST);heading.add(title);heading.add(open,BorderLayout.EAST);
            card.add(heading,BorderLayout.NORTH);card.add(OneManCards.text(choice[1]),BorderLayout.CENTER);tools.card(card);
        }
        tools.finish();JPanel home=new JPanel(new BorderLayout());home.add(OneManCards.scroll(tools));return home;
    }
    void refreshViews()
    {
        SwingUtilities.invokeLater(()->{
            if(!active||hubSelector==null)return;
            boolean present=false;for(int i=0;i<hubSelector.getItemCount();i++)if("Roadmap".equals(hubSelector.getItemAt(i)))present=true;
            if(config.companionRoadmapVisible()&&!present)hubSelector.addItem("Roadmap");
            if(!config.companionRoadmapVisible()&&present){if("Roadmap".equals(hubSelector.getSelectedItem()))hubSelector.setSelectedItem("Tools");hubSelector.removeItem("Roadmap");}
        });
    }
    private final class Panel extends JPanel {
        final JTextArea next=area(),prep=area();
        final JComboBox<CompanionPlanner.Step> goals=new JComboBox<>();boolean updating;
        Panel() {
            setLayout(new BorderLayout());JTabbedPane tabs=new JTabbedPane(JTabbedPane.TOP,JTabbedPane.SCROLL_TAB_LAYOUT);
            tabs.addTab("Next",new OneManTextCards(next));
            JPanel preparation=new JPanel(new BorderLayout());preparation.add(goals,BorderLayout.NORTH);preparation.add(new OneManTextCards(prep),BorderLayout.CENTER);
            JPanel actions=new JPanel(new GridLayout(0,1));JButton guide=new JButton("Open goal guide"),mark=new JButton("Confirm manual unlock");
            JButton reset=new JButton("Follow default roadmap");actions.add(reset);
            reset.addActionListener(e->clientThread.invokeLater(()->{focusedGoal=false;selected="";configs.setConfiguration("one-man-sync",profile,"companionFocus",false);}));
            actions.add(guide);actions.add(mark);preparation.add(actions,BorderLayout.SOUTH);tabs.addTab("Prep",preparation);
            add(tabs,BorderLayout.CENTER);
            goals.addActionListener(e-> { if(updating)return;CompanionPlanner.Step s=(CompanionPlanner.Step)goals.getSelectedItem();if(s!=null)clientThread.invokeLater(()-> { if(!baseline)return;selected=s.id;focusedGoal=true;configs.setConfiguration("one-man-sync",profile,"companionFocus",true);configs.setConfiguration("one-man-sync",profile,"companionGoal",selected);render(); }); });
            guide.addActionListener(e-> { CompanionPlanner.Step s=(CompanionPlanner.Step)goals.getSelectedItem();if(s!=null&&s.wiki!=null&&s.wiki.startsWith("https://oldschool.runescape.wiki/"))LinkBrowser.browse(s.wiki); });
            mark.addActionListener(e->clientThread.invokeLater(()-> { CompanionPlanner.Step s=goal();if(baseline&&s!=null&&"unlock".equals(s.type)) {manual.add(s.id);configs.setConfiguration("one-man-sync",profile,"companionManual",gson.toJson(manual));selected="";} }));
        }
        void update(String n,String p,List<CompanionPlanner.Step> choices,String id) {
            CompanionText.update(next,n);CompanionText.update(prep,p);
            CompanionPlanner.Step old=(CompanionPlanner.Step)goals.getSelectedItem();
            if(old==null||!old.id.equals(id)||goals.getItemCount()!=choices.size()) {
                updating=true;goals.removeAllItems();for(CompanionPlanner.Step choice:choices) {goals.addItem(choice);if(choice.id.equals(id))goals.setSelectedItem(choice);}updating=false;
            }
        }
    }
    private static JTextArea area() { JTextArea a=new JTextArea();a.setEditable(false);a.setLineWrap(true);a.setWrapStyleWord(true);a.setMargin(new Insets(8,8,8,8));return a; }
}
