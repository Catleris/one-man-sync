package lt.oneman.sync;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.inject.Inject;
import javax.swing.*;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import javax.inject.Singleton;
import net.runelite.client.Notifier;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.slayer.SlayerConfig;
import net.runelite.client.ui.*;
import net.runelite.client.util.LinkBrowser;

@Singleton
public class OneManSlayerHelper
{
    @Inject private Client client;
    @Inject private ConfigManager configManager;
    @Inject private ItemManager itemManager;
    @Inject private ClientToolbar toolbar;
    @Inject private ClientThread clientThread;
    @Inject private OverlayManager overlayManager;
    @Inject private WorldMapPointManager mapManager;
    @Inject private SlayerLabMinimapOverlay minimapOverlay;
    @Inject private SlayerLabAccount account;
    @Inject private OneManSyncConfig config;
    @Inject private Notifier notifier;
    private String variant = SlayerLabAdvice.REGULAR, goal = "", lastWarning = "";
    private final SlayerLabPrayer prayerMonitor = new SlayerLabPrayer();
    private String prayerView = "Log in to view Prayer resources.";
    private int lastXp = -1;
    private String currentTask = "";
    private final java.util.List<SlayerLabMapPoint> mapPoints = new ArrayList<>();
    private java.util.List<SlayerLabLocations.Destination> destinations = Collections.emptyList();
    private volatile SlayerLabLocations.Destination selectedDestination;
    private String taskSignature = "";
    private NavigationButton navigation;
    private LabPanel panel;
    private String lastView = "";
    private int nextRefresh;
    private volatile boolean active;

    void startUp()
    {
        prayerMonitor.reset();
        active = true;
        overlayManager.add(minimapOverlay);
        nextRefresh = 0;
        lastView = "";
        SwingUtilities.invokeLater(() -> {
            if (!active) return;
            panel = new LabPanel();
            BufferedImage icon = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = icon.createGraphics();
            g.setColor(new Color(218, 176, 85));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g.drawString("S", 6, 19);
            g.dispose();
            navigation = NavigationButton.builder().tooltip("OneMan Sync").icon(icon).priority(6).panel(panel).build();
            toolbar.addNavigation(navigation);
            panel.showView("Log in to detect your Slayer task.", "");
        });
    }
    void shutDown()
    {
        prayerMonitor.reset();
        account.pause("Plugin stopped", System.currentTimeMillis());
        active = false;
        overlayManager.remove(minimapOverlay);
        clearLocations();
        SwingUtilities.invokeLater(() -> {
            if (navigation != null) toolbar.removeNavigation(navigation);
            navigation = null;
            panel = null;
        });
        lastView = "";
    }
    public void onGameStateChanged(GameStateChanged event)
    {
        nextRefresh = 0;
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            prayerMonitor.reset(); prayerView="Log in to view Prayer resources.";
            account.pause("Logged out", System.currentTimeMillis());
            lastXp = -1; currentTask = ""; lastWarning = "";
            clearLocations();
            lastView = "";
            publish("Log in to detect your Slayer task.", "");
        }
    }
    public void onGameTick(GameTick event)
    {
        if (!active || client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) return;
        if (!bindAccount(System.currentTimeMillis())) return;
        refreshPrayer();
        for (int i = 0; i < mapPoints.size(); i++)
            mapPoints.get(i).highlight(destinations.get(i) == selectedDestination, (client.getTickCount() / 2) % 2 == 0);
        if (client.getTickCount() < nextRefresh) return;
        nextRefresh = client.getTickCount() + 5;
        long now = System.currentTimeMillis();
        goal = Optional.ofNullable(configManager.getConfiguration(SlayerLabConfig.GROUP, account.profile, "lootGoal")).orElse("");
        if (lastXp < 0) lastXp = client.getSkillExperience(Skill.SLAYER);
        // Built-in Slayer handles assignment DB decoding, messages and profile changes.
        String name = configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME, SlayerConfig.TASK_NAME_KEY);
        String location = configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME, SlayerConfig.TASK_LOC_KEY);
        int remaining = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
        if (remaining <= 0 || name == null || name.trim().isEmpty())
        {
            if (account.journal.active != null && remaining == 0)
                account.journal.observe(account.journal.active.task, account.journal.active.assignedArea, 0, account.journal.active.initial, now);
            account.pause("Task completed / cleared", now);
            currentTask = ""; lastWarning = "";
            clearLocations();
            publish("No active task detected. Ask a Slayer master or check your enchanted gem. Keep RuneLite's Slayer plugin enabled.", "");
            return;
        }
        // Do not display an old profile name while the built-in plugin catches up.
        String profileAmount = configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME, SlayerConfig.AMOUNT_KEY);
        if (!Integer.toString(remaining).equals(profileAmount)) return;
        String signature = SlayerLabCatalog.normalize(name) + "|" + SlayerLabCatalog.normalize(location);
        if (!signature.equals(taskSignature))
        {
            clearLocations();
            taskSignature = signature;
            variant = SlayerLabAdvice.REGULAR;
            destinations = SlayerLabLocations.forTask(name, location);
            for (SlayerLabLocations.Destination destination : destinations)
            {
                SlayerLabMapPoint point = new SlayerLabMapPoint(destination);
                mapPoints.add(point);
                mapManager.add(point);
            }
        }
        currentTask = name;
        String initialText=configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME,SlayerConfig.INIT_AMOUNT_KEY);
        int initial=0;
        try { initial=Integer.parseInt(initialText); } catch(NumberFormatException ignored) { }
        account.journal.observe(name, location, remaining, initial, now);
        SlayerLabJournal.Session session = account.journal.active;
        session.variant = variant;
        session.gear = String.join(", ", new TreeSet<>(itemNames(InventoryID.WORN)));
        session.chosenRoute = selectedDestination == null ? "" : selectedDestination.name;
        account.save(now, false);
        Set<String> inventory = itemNames(InventoryID.INV), equipment = itemNames(InventoryID.WORN);
        SlayerLabCatalog.Guide guide = SlayerLabCatalog.find(name);
        SlayerLabKnowledge.Entry knowledge = SlayerLabKnowledge.find(name);
        String summary = name + "\nRemaining: " + remaining + "\nAssigned area: " + (location == null || location.isEmpty() ? "any eligible location" : location);
        String prep = summary + "\n\n" + account.preparation(guide, inventory, equipment, now)
            + "\n\n" + (guide == null ? "Use Task Wiki for requirements." : guide.advice)
            + "\n\n" + (knowledge == null ? "" : knowledge.overview())
            + "\n\nYour Slayer level: " + client.getRealSkillLevel(Skill.SLAYER)
            + "\nA variant may require a higher level; protection and exemptions must be checked for that variant.";
        StringBuilder places = new StringBuilder(summary+"\n\nAssigned locations must be respected; generic alternatives are reference only.\n\n");
        for (SlayerLabLocations.Destination destination : destinations) places.append(SlayerLabAdvice.comparison(destination)).append("\n");
        if(destinations.isEmpty()) places.append("No verified entrance for this task/assigned area. Use Task Wiki.\n");
        if(guide != null) places.append("\nSource locations:\n").append(guide.locations);
        SlayerLabLocations.Destination selected=selectedDestination;
        if(selected!=null) places.append("\n\nTRACKING: ").append(selected.name).append("\nDirection: ")
            .append(SlayerLabLocations.direction(client.getLocalPlayer().getWorldLocation(), selected.entrance));
        places.append("\n\nMarkers and direction only; no calculated walking path or internal dungeon route.");
        Set<String> carried=new HashSet<>(inventory); carried.addAll(equipment);
        Set<String> quests = new HashSet<>();
        Quest[] checked={Quest.HEROES_QUEST,Quest.CABIN_FEVER,Quest.SONG_OF_THE_ELVES,Quest.HORROR_FROM_THE_DEEP,Quest.MONKEY_MADNESS_II,Quest.BONE_VOYAGE,Quest.PRIEST_IN_PERIL};
        String[] labels={"Heroes' Quest","Cabin Fever","Song of the Elves","Horror from the Deep","Monkey Madness II","Bone Voyage","Priest in Peril"};
        for(int i=0;i<checked.length;i++) if(checked[i].getState(client)==QuestState.FINISHED) quests.add(labels[i]);
        StringBuilder travel=new StringBuilder("Availability uses carried items and verified quest completion. Dated bank ownership does not make a route ready.\n\n");
        for(SlayerLabLocations.Destination d:destinations) travel.append(SlayerLabTravel.route(d,carried,quests));
        if(destinations.isEmpty()) travel.append("No verified travel route for this assigned area; see Task Wiki.");
        SlayerLabSupplies.Counts supplies = supplyCounts();
        String warning=supplies.warnings(config);
        if(config.notifySupplies() && !warning.isEmpty() && !warning.equals(lastWarning)) notifier.notify("Slayer Lab: " + warning);
        lastWarning=warning;
        final String sessionNote = session.note;
        final long sessionStarted = session.started;
        final String sessionProfile = account.profile;
        String[] views={prep,SlayerLabAdvice.tactics(name,variant),places.toString(),travel.toString(),supplies.describe(config),SlayerLabJournal.metrics(session,now),SlayerLabAdvice.loot(name,goal,account.bank,session),account.journal.historyText(),prayerView};
        java.util.List<SlayerLabLocations.Destination> snapshot=destinations;
        SwingUtilities.invokeLater(() -> {
            if(active && panel!=null) { panel.showTabs(views,name,sessionNote,sessionStarted,sessionProfile); panel.showLocations(snapshot); }
        });
    }
    private void refreshPrayer()
    {
        int points=client.getBoostedSkillLevel(Skill.PRAYER);
        int level=client.getRealSkillLevel(Skill.PRAYER);
        java.util.List<String> enabled=SlayerLabPrayer.active(client);
        prayerMonitor.observe(points,String.join("|",enabled));
        Map<String,Integer> inventory=new TreeMap<>();
        ItemContainer container=client.getItemContainer(InventoryID.INV);
        if(container!=null) for(Item item:container.getItems())
        {
            if(item.getId()<0 || item.getQuantity()<=0) continue;
            ItemComposition definition=itemManager.getItemComposition(item.getId());
            if(definition.getNote()!=-1) continue;
            inventory.merge(SlayerLabKnowledge.normalize(definition.getName()),item.getQuantity(),Integer::sum);
        }
        SlayerLabPrayer.Doses doses=SlayerLabPrayer.doses(inventory);
        java.util.List<String> alerts=prayerMonitor.crossed(points,doses,config);
        if(config.notifyPrayer()) for(String alert:alerts) notifier.notify("OneMan Prayer: "+alert);
        prayerView=prayerMonitor.describe(points,level,enabled,doses,config);
        String snapshot=prayerView;
        boolean low=!prayerMonitor.warnings(points,doses,config).isEmpty();
        SwingUtilities.invokeLater(() -> { if(active && panel!=null) panel.showPrayer(snapshot,low); });
    }
    private boolean bindAccount(long now)
    {
        String profileKey=configManager.getRSProfileKey();
        if(!Objects.equals(profileKey,account.profile))
        {
            prayerMonitor.reset(); prayerView="Loading Prayer resources…";
            lastXp=-1; currentTask=""; lastWarning=""; clearLocations();
            variant=SlayerLabAdvice.REGULAR;
            publish("Loading this account profile…", "");
        }
        return account.bind(profileKey,now);
    }
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        nextRefresh=0;
        if(event.getContainerId()==InventoryID.WORN) prayerMonitor.resetEstimate();
        if(event.getContainerId()!=InventoryID.BANK || client.getGameState()!=GameState.LOGGED_IN) return;
        if(!bindAccount(System.currentTimeMillis())) return;
        Map<String,Integer> items=new TreeMap<>();
        for(Item item:event.getItemContainer().getItems()) {
            if(item.getId()<0 || item.getQuantity()<=0) continue;
            ItemComposition d=itemManager.getItemComposition(item.getId());
            if(d.getPlaceholderTemplateId()!=-1) continue;
            if(d.getNote()!=-1) d=itemManager.getItemComposition(d.getLinkedNoteId());
            items.merge(SlayerLabKnowledge.normalize(d.getName()),item.getQuantity(),Integer::sum);
        }
        account.captureBank(items,System.currentTimeMillis());
    }
    public void onStatChanged(StatChanged event)
    {
        if(event.getSkill()!=Skill.SLAYER || client.getGameState()!=GameState.LOGGED_IN) return;
        if(lastXp>=0 && Objects.equals(account.profile,configManager.getRSProfileKey())) account.journal.xp(event.getXp()-lastXp);
        lastXp=event.getXp();
    }
    public void onNpcLootReceived(NpcLootReceived event)
    {
        if(account.journal.active==null || !Objects.equals(account.profile,configManager.getRSProfileKey())) return;
        SlayerLabKnowledge.Entry entry=SlayerLabKnowledge.find(currentTask);
        if(entry==null || !entry.matchesNpc(event.getNpc().getName())) return;
        Map<String,Integer> loot=new TreeMap<>();
        for(ItemStack stack:event.getItems()) {
            ItemComposition d=itemManager.getItemComposition(stack.getId());
            if(d.getNote()!=-1) d=itemManager.getItemComposition(d.getLinkedNoteId());
            loot.merge(SlayerLabKnowledge.normalize(d.getName()),stack.getQuantity(),Integer::sum);
        }
        account.journal.loot(loot); nextRefresh=0;
    }
    private SlayerLabSupplies.Counts supplyCounts()
    {
        Map<String,Integer> counts=new TreeMap<>(); Set<String> edible=new HashSet<>(),drinkable=new HashSet<>();
        for(int id:new int[]{InventoryID.INV,InventoryID.WORN}) {
            ItemContainer c=client.getItemContainer(id); if(c==null) continue;
            for(Item item:c.getItems()) {
                if(item.getId()<0 || item.getQuantity()<=0) continue;
                ItemComposition d=itemManager.getItemComposition(item.getId()); if(d.getNote()!=-1) continue;
                String n=SlayerLabKnowledge.normalize(d.getName()); counts.merge(n,item.getQuantity(),Integer::sum);
                String[] actions=d.getInventoryActions(); if(actions==null) continue;
                for(String action:actions) { if("Eat".equalsIgnoreCase(action)) edible.add(n); if("Drink".equalsIgnoreCase(action)) drinkable.add(n); }
            }
        }
        return SlayerLabSupplies.count(counts,edible,drinkable);
    }

    WorldPoint navigationTarget()
    {
        SlayerLabLocations.Destination selected = selectedDestination;
        return active && selected != null ? selected.entrance : null;
    }
    private void clearLocations()
    {
        for (SlayerLabMapPoint point : mapPoints) mapManager.remove(point);
        mapPoints.clear();
        destinations = Collections.emptyList();
        selectedDestination = null;
        taskSignature = "";
    }
    private void track(SlayerLabLocations.Destination destination, boolean centre)
    {
        clientThread.invokeLater(() -> {
            if (!active || !destinations.contains(destination)) return;
            if (centre)
                client.getWorldMap().setWorldMapPositionTarget(destination.entrance);
            else
                selectedDestination = destination;
            nextRefresh = 0;
        });
    }
    private Set<String> itemNames(int containerId)
    {
        Set<String> result = new HashSet<>();
        ItemContainer container = client.getItemContainer(containerId);
        if (container == null) return result;
        for (Item item : container.getItems())
        {
            if (item.getId() < 0 || item.getQuantity() <= 0) continue;
            ItemComposition definition = itemManager.getItemComposition(item.getId());
            if (definition.getNote() != -1) continue;
            result.add(SlayerLabCatalog.normalize(definition.getName()));
        }
        return result;
    }
    private void publish(String view, String url)
    {
        view = "Website sync: " + (config.enabled() ? "enabled" : "disabled (local tools still work)") + "\n\n" + view;
        if (view.equals(lastView)) return;
        lastView = view;
        final String displayedView = view;
        java.util.List<SlayerLabLocations.Destination> snapshot = destinations;
        SwingUtilities.invokeLater(() -> {
            if (active && panel != null)
            {
                panel.showView(displayedView, url);
                panel.showLocations(snapshot);
            }
        });
    }
    private final class LabPanel extends PluginPanel
    {
        private final JTabbedPane tabs = new JTabbedPane();
        private final JTextArea[] sections = new JTextArea[9];
        private final JComboBox<String> variants = new JComboBox<>(), goals = new JComboBox<>();
        private final JTextArea note = new JTextArea(3,20);
        private String shownTask = "", shownProfile = "";
        private long shownSession;
        private boolean updating;

        private final JButton wiki = new JButton("Task Wiki");
        private String url = "";
        private final JComboBox<SlayerLabLocations.Destination> choices = new JComboBox<>();
        private final JTextArea access = new JTextArea();
        private final JButton centre = new JButton("Centre map");
        private final JButton track = new JButton("Track entrance");
        private final JButton clear = new JButton("Stop tracking");
        private java.util.List<SlayerLabLocations.Destination> shown = Collections.emptyList();
        LabPanel()
        {
            setLayout(new BorderLayout(0, 10));
            setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
            JLabel title = new JLabel("ONEMAN SYNC • SLAYER / PRAYER");
            title.setForeground(new Color(218, 176, 85));

            String[] labels={"Prep","Tactics","Places","Travel","Supplies","Session","Loot","History","Prayer"};
            for(int i=0;i<labels.length;i++) {
                JTextArea area=new JTextArea(16,20); area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
                area.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13)); sections[i]=area;
                JPanel page=new JPanel(new BorderLayout(0,6)); page.add(area,BorderLayout.CENTER);
                if(i==1) {
                    JPanel options=new JPanel(new BorderLayout()); options.add(variants,BorderLayout.NORTH);
                    JButton variantWiki=new JButton("Selected variant Wiki"); options.add(variantWiki,BorderLayout.SOUTH); page.add(options,BorderLayout.NORTH);
                    variantWiki.addActionListener(e -> { String selected=(String)variants.getSelectedItem(); if(selected!=null && !shownTask.isEmpty()) LinkBrowser.browse(SlayerLabAdvice.strategy(SlayerLabAdvice.REGULAR.equals(selected)?shownTask:selected)); });
                }
                if(i==6) { goals.setEditable(true); JButton saveGoal=new JButton("Save loot goal"); JPanel options=new JPanel(new BorderLayout()); options.add(goals,BorderLayout.NORTH); options.add(saveGoal,BorderLayout.SOUTH); page.add(options,BorderLayout.NORTH);
                    saveGoal.addActionListener(e -> { String entered=String.valueOf(goals.getEditor().getItem()).trim(); String value=entered.substring(0,Math.min(120,entered.length())); String profile=shownProfile; clientThread.invokeLater(()->{ if(!profile.isEmpty() && Objects.equals(profile,account.profile) && Objects.equals(profile,configManager.getRSProfileKey())) { configManager.setConfiguration(SlayerLabConfig.GROUP,account.profile,"lootGoal",value); nextRefresh=0; } }); }); }
                if(i==5) { JPanel notes=new JPanel(new BorderLayout()); note.setLineWrap(true); note.setWrapStyleWord(true); JButton save=new JButton("Save session note"); notes.add(new JScrollPane(note),BorderLayout.CENTER); notes.add(save,BorderLayout.SOUTH); page.add(notes,BorderLayout.SOUTH);
                    save.addActionListener(e -> { String value=note.getText().substring(0, Math.min(1000,note.getText().length())); long started=shownSession; String profile=shownProfile; clientThread.invokeLater(()->{ if(account.journal.active!=null && account.journal.active.started==started && Objects.equals(profile,account.profile) && Objects.equals(profile,configManager.getRSProfileKey())) { account.journal.active.note=value; account.save(System.currentTimeMillis(),true); nextRefresh=0; } }); }); }
                tabs.addTab(labels[i],page);
            }
            add(tabs, BorderLayout.CENTER);
            variants.addActionListener(e -> { if(updating) return; String value=(String)variants.getSelectedItem(); if(value!=null) clientThread.invokeLater(()->{variant=value; nextRefresh=0;}); });
            wiki.addActionListener(e -> { if (!url.isEmpty()) LinkBrowser.browse(url); });
            JPanel controls = new JPanel();
            controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
            controls.add(new JLabel("DESTINATION / ENTRANCE"));
            choices.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            controls.add(choices);
            access.setColumns(20);
            access.setEditable(false);
            access.setLineWrap(true);
            access.setWrapStyleWord(true);
            access.setOpaque(false);
            controls.add(access);
            for (JButton button : new JButton[]{centre, track, clear, wiki})
            {
                button.setAlignmentX(Component.LEFT_ALIGNMENT);
                button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                controls.add(button);
            }
            choices.addActionListener(e -> {
                SlayerLabLocations.Destination choice = (SlayerLabLocations.Destination) choices.getSelectedItem();
                access.setText(choice == null ? "No mapped entrance available." : choice.access);
            });
            centre.addActionListener(e -> {
                SlayerLabLocations.Destination choice = (SlayerLabLocations.Destination) choices.getSelectedItem();
                if (choice != null) OneManSlayerHelper.this.track(choice, true);
            });
            track.addActionListener(e -> {
                SlayerLabLocations.Destination choice = (SlayerLabLocations.Destination) choices.getSelectedItem();
                if (choice != null) OneManSlayerHelper.this.track(choice, false);
            });
            clear.addActionListener(e -> clientThread.invokeLater(() -> { selectedDestination = null; nextRefresh = 0; }));
            JPanel header = new JPanel(new BorderLayout(0, 10));
            header.add(title, BorderLayout.NORTH);
            header.add(controls, BorderLayout.CENTER);
            add(header, BorderLayout.NORTH);
        }
        void showLocations(java.util.List<SlayerLabLocations.Destination> locations)
        {
            if (shown.equals(locations)) return;
            shown = locations;
            choices.removeAllItems();
            for (SlayerLabLocations.Destination destination : locations) choices.addItem(destination);
            boolean hasLocations = !locations.isEmpty();
            choices.setEnabled(hasLocations);
            centre.setEnabled(hasLocations);
            track.setEnabled(hasLocations);
            clear.setEnabled(hasLocations);
            if (!hasLocations) access.setText("No mapped entrance available.");
        }
        void showPrayer(String view,boolean low)
        {
            JTextArea section=sections[8];
            if(!section.getText().equals(view)) {
                int caret=section.getCaretPosition(); section.setText(view); section.setCaretPosition(Math.min(caret,view.length()));
            }
            tabs.setForegroundAt(8,low?new Color(255,120,90):UIManager.getColor("Label.foreground"));
        }
        void showTabs(String[] views,String task,String sessionNote,long started,String profile)
        {
            updating=true;
            shownProfile=profile;
            if(shownSession!=started) { shownSession=started; note.setText(sessionNote); }
            if(!shownTask.equals(task)) {
                shownTask=task; variants.removeAllItems(); for(String option:SlayerLabAdvice.variants(task)) variants.addItem(option);
                goals.removeAllItems(); for(SlayerLabAdvice.Goal option:SlayerLabAdvice.goals(task)) goals.addItem(option.item);
                goals.getEditor().setItem(goal); note.setText(sessionNote);
            }
            for(int i=0;i<sections.length;i++) { int caret=sections[i].getCaretPosition(); sections[i].setText(views[i]); sections[i].setCaretPosition(Math.min(caret,views[i].length())); }
            url=SlayerLabCatalog.wiki(task); wiki.setEnabled(true); updating=false;
        }
        void showView(String view, String link)
        {
            updating=true; shownTask=""; shownProfile=""; shownSession=0;
            for(int i=0;i<8;i++) sections[i].setText(view);
            showPrayer(prayerView,false);
            variants.removeAllItems(); goals.removeAllItems(); note.setText("");
            url = link;
            wiki.setEnabled(!link.isEmpty()); updating=false;
        }
    }
}
