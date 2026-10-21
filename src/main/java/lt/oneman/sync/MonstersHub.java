package lt.oneman.sync;

import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;

/** Client-thread NPC tracking; only immutable tile snapshots cross into rendering. */
@Singleton
final class MonstersHub
{
    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    @Inject private ConfigManager configs;
    @Inject private OverlayManager overlays;
    @Inject private ShamanMinionOverlay overlay;
    private final Set<NPC> minions = Collections.newSetFromMap(new IdentityHashMap<>());
    private volatile List<Marker> markers = Collections.emptyList();
    private volatile boolean active;
    private boolean scanPending;
    private Supplier<JsonObject> slayerState;
    private String suggestion = "Select a profile to use it without a Slayer task.";
    private String lastStatus = "";
    private Panel panel;

    static final class Marker
    {
        final LocalPoint location;
        final int plane;
        Marker(LocalPoint location, int plane) { this.location = location; this.plane = plane; }
    }

    void startUp(Supplier<JsonObject> slayerState)
    {
        this.slayerState = slayerState;
        active = true;
        scanPending = true;
        minions.clear();
        markers = Collections.emptyList();
        overlay.setHub(this);
        overlays.add(overlay);
        lastStatus = "";
    }

    void shutDown()
    {
        active = false;
        clear();
        slayerState = null;
        overlays.remove(overlay);
        overlay.setHub(null);
        SwingUtilities.invokeLater(() -> panel = null);
    }

    boolean enabled()
    {
        return active && config.monstersProfile() == MonstersProfile.LIZARDMAN_SHAMAN && config.monstersShamanTiles()
            && client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 0;
    }

    List<Marker> markers() { return markers; }

    static boolean isMinion(int id) { return id == NpcID.ZEAH_LIZARDSHAMAN_SPAWN; }

    static boolean suggestsShaman(OneManSlayerState.LocalTask task)
    {
        String name = task.name.toLowerCase(Locale.ROOT);
        return task.remaining > 0 && (name.equals("lizardmen") || name.equals("lizardman")
            || name.equals("lizardman shaman") || name.equals("lizardman shamans"));
    }

    void onNpcSpawned(NpcSpawned event)
    {
        if (enabled() && isMinion(event.getNpc().getId()))
        {
            minions.add(event.getNpc());
            publishMarkers();
        }
    }

    void onNpcDespawned(NpcDespawned event)
    {
        if (minions.remove(event.getNpc())) publishMarkers();
    }

    void onNpcChanged(NpcChanged event)
    {
        NPC npc = event.getNpc();
        if (enabled() && isMinion(npc.getId())) minions.add(npc);
        else minions.remove(npc);
        publishMarkers();
    }

    void onGameStateChanged(GameStateChanged event)
    {
        clear();
        scanPending = true;
        suggestion = "Select a profile to use it without a Slayer task.";
        refresh();
    }

    void onConfigChanged()
    {
        if (!active) return;
        clear();
        scanPending = true;
        lastStatus = "";
        refresh();
    }

    void onGameTick()
    {
        if (!active || client.getGameState() != GameState.LOGGED_IN) return;
        OneManSlayerState.LocalTask task = OneManSlayerState.localTask(slayerState.get(), client.getVarpValue(VarPlayerID.SLAYER_COUNT));
        suggestion = suggestsShaman(task)
            ? "Suggested: Lizardman Shaman. Your Lizardmen task can include Shamans; check any task location restriction. Select the profile below."
            : "No matching confirmed Slayer task. You can still select Lizardman Shaman manually.";
        if (!enabled())
        {
            clear();
            scanPending = true;
        }
        else
        {
            // One bounded NPC-list scan after enabling/login/scene transition, never each tick.
            if (scanPending)
            {
                scanPending = false;
                for (NPC npc : client.getTopLevelWorldView().npcs())
                    if (isMinion(npc.getId())) minions.add(npc);
            }
            minions.removeIf(npc -> !isMinion(npc.getId()));
            publishMarkers();
        }
        refresh();
    }

    private void clear() { minions.clear(); markers = Collections.emptyList(); }

    private void publishMarkers()
    {
        if (!enabled() || client.getGameState() != GameState.LOGGED_IN)
        {
            markers = Collections.emptyList();
            return;
        }
        List<Marker> next = new ArrayList<>();
        for (NPC npc : minions)
        {
            // Ignore entities in a different world view rather than project their coordinates here.
            LocalPoint location = npc.getLocalLocation();
            if (location != null && npc.getWorldView() == client.getTopLevelWorldView())
                next.add(new Marker(location, npc.getWorldLocation().getPlane()));
        }
        markers = Collections.unmodifiableList(next);
    }

    JPanel companionPanel() { if (panel == null) panel = new Panel(); return panel; }

    private void refresh()
    {
        String status = suggestion + "\n\nObserved minions: " + markers.size();
        if (status.equals(lastStatus)) return;
        lastStatus = status;
        SwingUtilities.invokeLater(() -> { if (active && panel != null) panel.refresh(status); });
    }

    private final class Panel extends JPanel
    {
        private final JComboBox<MonstersProfile> profile = new JComboBox<>(MonstersProfile.values());
        private final JCheckBox tiles = new JCheckBox("Mark current minion tiles");
        private final JButton color = new JButton("Tile colour");
        private final JTextArea status = OneManCards.text(suggestion);
        private boolean updating;

        Panel()
        {
            super(new BorderLayout());
            OneManCards cards = new OneManCards();
            cards.card(OneManCards.banner("Monsters Hub • Test", "Lizardman Shaman • local helper"));
            JPanel settings = OneManCards.card();
            JPanel controls = new JPanel(new GridLayout(0, 1, 0, 6));
            controls.setOpaque(false);
            profile.setName("monstersProfile");
            tiles.setName("monstersShamanTiles");
            tiles.setOpaque(false);
            controls.add(profile); controls.add(tiles); controls.add(color);
            settings.add(controls, BorderLayout.NORTH);
            settings.add(status, BorderLayout.CENTER);
            cards.card(settings);
            JPanel timing = OneManCards.card();
            timing.add(OneManCards.text("Explosion countdowns unavailable in this test. Exact per-minion timing and the server trigger could not be verified. No seconds/ticks switch is offered without a reliable countdown."));
            cards.card(timing);
            JPanel scope = OneManCards.card();
            scope.add(OneManCards.text("Tiles follow already visible spawns each game tick. Only their occupied tile is marked. Chambers of Xeric helper mechanics are outside this test's scope."));
            cards.card(scope);
            cards.finish(); add(OneManCards.scroll(cards), BorderLayout.CENTER);
            profile.addActionListener(event -> { if (!updating) configs.setConfiguration("one-man-sync", "monstersProfile", profile.getSelectedItem()); });
            tiles.addActionListener(event -> { if (!updating) configs.setConfiguration("one-man-sync", "monstersShamanTiles", tiles.isSelected()); });
            color.addActionListener(event -> {
                Color selected = JColorChooser.showDialog(this, "Shaman minion tile colour", config.monstersShamanColor());
                if (selected != null) configs.setConfiguration("one-man-sync", "monstersShamanColor", selected);
            });
            refresh(suggestion);
        }

        void refresh(String message)
        {
            updating = true;
            profile.setSelectedItem(config.monstersProfile());
            tiles.setSelected(config.monstersShamanTiles());
            color.setForeground(config.monstersShamanColor());
            status.setText(message);
            updating = false;
            revalidate(); repaint();
        }
    }
}
