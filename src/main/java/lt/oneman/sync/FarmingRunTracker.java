package lt.oneman.sync;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTabbedPane;
import java.util.Map;
import java.util.HashMap;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.timetracking.TimeTrackingConfig;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

@Singleton
public class FarmingRunTracker {
    @Inject
    private Client client;
    @Inject
    private ConfigManager configManager;
    @Inject
    private ClientThread clientThread;
    @Inject
    private Notifier notifier;
    @Inject
    private OneManSyncConfig config;
    @Inject private OneManMapLocations mapLocations;
    private FarmingPanel panel;
    private volatile boolean active;
    private volatile int nextFarmingRefresh = 0;
    private String profile = "";
    private boolean baseline;
    private boolean[] lastBirdhouseReady = new boolean[4];
    public static final List<TrackedPatchDef> TRACKED_PATCHES;

    void startUp() {
        this.active = true;
        this.nextFarmingRefresh = 0;
        this.profile = ""; this.baseline = false;
        SwingUtilities.invokeLater(() -> {
            if (!this.active) {
                return;
            }
            if (this.panel == null) {
                this.panel = new FarmingPanel();
            }
            this.panel.showStatus("Log in to see Farming Run status.");
        });
    }

    void shutDown() {
        this.active = false;
        SwingUtilities.invokeLater(() -> {
            this.panel = null;
        });
    }

    JPanel companionPanel() {
        if (this.panel == null) {
            this.panel = new FarmingPanel();
        }
        return this.panel;
    }

    void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN) {
            this.baseline = false; this.profile = "";
            this.nextFarmingRefresh = 0;
            SwingUtilities.invokeLater(() -> {
                if (this.active && this.panel != null) {
                    this.panel.showStatus("Log in to see Farming Run status.");
                }
            });
        }
        if (event.getGameState() == GameState.LOGGED_IN) {
            this.nextFarmingRefresh = 0;
        }
    }

    void onGameTick(GameTick event) {
        if (!this.active || this.client.getGameState() != GameState.LOGGED_IN || this.client.getLocalPlayer() == null) {
            return;
        }
        if (this.client.getTickCount() < this.nextFarmingRefresh) {
            return;
        }
        this.nextFarmingRefresh = this.client.getTickCount() + 20;
        this.refresh();
    }

    private void refresh() {
        String key = configManager.getRSProfileKey();
        if (key == null || key.isEmpty()) return;
        if (!key.equals(profile)) {
            profile = key; baseline = false;
            java.util.Arrays.fill(lastBirdhouseReady, false);
        }
        int[] birdVarps = new int[]{VarPlayerID.BIRDHOUSE_TRANSMIT_A, VarPlayerID.BIRDHOUSE_TRANSMIT_B, VarPlayerID.BIRDHOUSE_TRANSMIT_C, VarPlayerID.BIRDHOUSE_TRANSMIT_D};
        String[] birdNames = new String[]{"Mushroom Meadow N", "Mushroom Meadow S", "Verdant Valley NE", "Verdant Valley SW"};
        long now = System.currentTimeMillis() / 1000L;
        StringBuilder sb = new StringBuilder("=== BIRDHOUSE RUNS ===\n");
        for (int i = 0; i < birdVarps.length; ++i) {
            String stored = this.configManager.getRSProfileConfiguration(TimeTrackingConfig.CONFIG_GROUP, TimeTrackingConfig.BIRD_HOUSE + "." + birdVarps[i]);
            sb.append(birdNames[i]).append(": ");
            if (stored == null || stored.trim().isEmpty()) {
                sb.append("Unknown (visit Fossil Island to detect)\n");
                this.lastBirdhouseReady[i] = false;
                continue;
            }
            int varp = -1;
            long seen = 0L;
            String[] parts = stored.split(":");
            if (parts.length == 2) {
                try {
                    varp = Integer.parseInt(parts[0]);
                    seen = Long.parseLong(parts[1]);
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            }
            if (varp < 0 || seen <= 0L) {
                sb.append("Unknown (visit Fossil Island to detect)\n");
                this.lastBirdhouseReady[i] = false;
                continue;
            }
            BirdHouseDecoder.State state = BirdHouseDecoder.getState(varp);
            String houseName = BirdHouseDecoder.getHouseName(varp);
            if (houseName == null) {
                houseName = "Bird House";
            }
            if (state == BirdHouseDecoder.State.EMPTY) {
                sb.append("Empty (no birdhouse placed)\n");
                this.lastBirdhouseReady[i] = false;
                continue;
            }
            if (state == BirdHouseDecoder.State.BUILT) {
                sb.append(houseName).append(" \u2014 Built (needs seeds)\n");
                this.lastBirdhouseReady[i] = false;
                continue;
            }
            if (state == BirdHouseDecoder.State.SEEDED) {
                long readyAt = seen + 3000L;
                boolean ready = now >= readyAt;
                if (ready) {
                    sb.append(houseName).append(" \u2014 READY to collect!\n");
                    if (baseline && !this.lastBirdhouseReady[i] && this.config.notifyBirdhousesReady()) {
                        this.notifier.notify("OneMan: " + birdNames[i] + " (" + houseName + ") is ready!");
                    }
                } else {
                    long left = readyAt - now;
                    long mins = left / 60L;
                    long secs = left % 60L;
                    sb.append(houseName).append(" \u2014 Growing (").append(mins).append("m ").append(secs).append("s left)\n");
                }
                this.lastBirdhouseReady[i] = ready;
                continue;
            }
            sb.append("Visit this birdhouse to refresh its status.\n");
            this.lastBirdhouseReady[i] = false;
        }
        baseline = true;
        Map<String,String> patchStatuses=new HashMap<>();
        for (TrackedPatchDef def : TRACKED_PATCHES) {
            String raw=this.configManager.getRSProfileConfiguration(TimeTrackingConfig.CONFIG_GROUP, def.configKey());
            FarmingPatchDecoder.DecodedPatch decoded=FarmingPatchDecoder.decode(def.getImplementation(),raw);
            String status=decoded==null?"Unknown — visit patch with Time Tracking enabled.":decoded.getDisplayStatus(now);
            patchStatuses.put(def.getName(),status);
        }
        String contractId = this.configManager.getRSProfileConfiguration(TimeTrackingConfig.CONFIG_GROUP, "contract");
        sb.append("\n=== FARMING CONTRACT ===\n");
        if (contractId != null && !contractId.trim().isEmpty()) {
            boolean complete = this.client.getVarbitValue(VarbitID.FARMGUILD_CONTRACT_COMPLETE) > 0;
            sb.append("Contract active: ").append(complete ? "COMPLETE \u2713" : "In progress...").append("\n");
        } else {
            sb.append("No active contract detected.\n");
        }
        String text = sb.toString();
        SwingUtilities.invokeLater(() -> {
            if (this.active && this.panel != null) {
                this.panel.showStatus(text,patchStatuses);
            }
        });
    }

    static {
        ArrayList<TrackedPatchDef> list = new ArrayList<TrackedPatchDef>();
        list.add(new TrackedPatchDef("Herb: Ardougne", 10548, VarbitID.FARMING_TRANSMIT_D, FarmingPatchType.HERB));
        list.add(new TrackedPatchDef("Herb: Catherby", 11062, VarbitID.FARMING_TRANSMIT_D, FarmingPatchType.HERB));
        list.add(new TrackedPatchDef("Herb: Falador", 12083, VarbitID.FARMING_TRANSMIT_D, FarmingPatchType.HERB));
        list.add(new TrackedPatchDef("Herb: Farming Guild", 4922, VarbitID.FARMING_TRANSMIT_E, FarmingPatchType.HERB));
        list.add(new TrackedPatchDef("Herb: Harmony", 15148, VarbitID.FARMING_TRANSMIT_B, FarmingPatchType.HERB));
        list.add(new TrackedPatchDef("Herb: Hosidius", 6967, VarbitID.FARMING_TRANSMIT_D, FarmingPatchType.HERB));
        list.add(new TrackedPatchDef("Herb: Morytania", 14391, VarbitID.FARMING_TRANSMIT_D, FarmingPatchType.HERB));
        list.add(new TrackedPatchDef("Herb: Troll Stronghold", 11321, VarbitID.FARMING_TRANSMIT_A, FarmingPatchType.HERB));
        list.add(new TrackedPatchDef("Herb: Weiss", 11325, VarbitID.FARMING_TRANSMIT_A, FarmingPatchType.HERB));
        list.add(new TrackedPatchDef("Herb: Varlamore", 6192, VarbitID.FARMING_TRANSMIT_D, FarmingPatchType.HERB));
        list.add(new TrackedPatchDef("Tree: Falador", 11828, VarbitID.FARMING_TRANSMIT_A, FarmingPatchType.TREE));
        list.add(new TrackedPatchDef("Tree: Farming Guild", 4922, VarbitID.FARMING_TRANSMIT_G, FarmingPatchType.TREE));
        list.add(new TrackedPatchDef("Tree: Gnome Stronghold", 9781, VarbitID.FARMING_TRANSMIT_A, FarmingPatchType.TREE));
        list.add(new TrackedPatchDef("Tree: Lumbridge", 12594, VarbitID.FARMING_TRANSMIT_A, FarmingPatchType.TREE));
        list.add(new TrackedPatchDef("Tree: Taverley", 11573, VarbitID.FARMING_TRANSMIT_A, FarmingPatchType.TREE));
        list.add(new TrackedPatchDef("Tree: Varrock", 12854, VarbitID.FARMING_TRANSMIT_A, FarmingPatchType.TREE));
        list.add(new TrackedPatchDef("Tree: Auburnvale", 5427, VarbitID.FARMING_TRANSMIT_A, FarmingPatchType.TREE));
        TRACKED_PATCHES = Collections.unmodifiableList(list);
    }

    private final class FarmingPanel
    extends JPanel {
        private final JTextArea area = new JTextArea();
        private final JButton refresh = new JButton("Refresh");
        private final OneManMapPanel patches=new OneManMapPanel(mapLocations,OneManMapCatalog.Group.FARMING);

        FarmingPanel() {
            this.setLayout(new BorderLayout(0, 6));
            this.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            this.area.setEditable(false);
            this.area.setLineWrap(true);
            this.area.setWrapStyleWord(true);
            this.area.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            this.refresh.addActionListener(e -> FarmingRunTracker.this.clientThread.invokeLater(() -> {
                FarmingRunTracker.this.nextFarmingRefresh = 0;
            }));
            JTabbedPane tabs=new JTabbedPane(JTabbedPane.TOP,JTabbedPane.SCROLL_TAB_LAYOUT);tabs.addTab("Patches",patches);
            tabs.addTab("Seeds",new FarmingSeedPanel(mapLocations,category->{patches.chooseCategory(category);tabs.setSelectedIndex(0);}));
            JPanel timers=new JPanel(new BorderLayout(0,5));
            timers.add(new JScrollPane(this.area),BorderLayout.CENTER);timers.add(this.refresh,BorderLayout.SOUTH);
            tabs.addTab("Runs",timers);
            this.add(tabs,BorderLayout.CENTER);
        }

        void showStatus(String text) { showStatus(text,Collections.emptyMap()); }
        void showStatus(String text,Map<String,String> observed) {
            CompanionText.update(this.area, text);
            Map<String,String> status=new HashMap<>();
            observed.forEach((key,value)->status.put(key.replace(": ",":"),value));
            patches.setStatuses(status);
        }
    }

    public static class TrackedPatchDef {
        private final String name;
        private final int regionId;
        private final int varbitId;
        private final FarmingPatchType implementation;

        public TrackedPatchDef(String name, int regionId, int varbitId, FarmingPatchType implementation) {
            this.name = name;
            this.regionId = regionId;
            this.varbitId = varbitId;
            this.implementation = implementation;
        }

        public String getName() {
            return this.name;
        }

        public int getRegionId() {
            return this.regionId;
        }

        public int getVarbitId() {
            return this.varbitId;
        }

        public FarmingPatchType getImplementation() {
            return this.implementation;
        }

        public String configKey() {
            return this.regionId + "." + this.varbitId;
        }
    }
}

