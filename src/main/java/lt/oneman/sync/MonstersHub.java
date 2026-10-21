package lt.oneman.sync;

import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import net.runelite.client.callback.ClientThread;
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

/** Client-thread NPC tracking; immutable tracking snapshots are published to the client-thread overlay. */
@Singleton
final class MonstersHub
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
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
        final NPC npc;
        final LocalPoint location;
        final int plane;
        Marker(NPC npc, LocalPoint location, int plane) { this.npc = npc; this.location = location; this.plane = plane; }
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
        return active && config.monstersProfile() == MonstersProfile.LIZARDMAN_SHAMAN && (config.monstersShamanTiles() || config.monstersShamanOutline()
                || config.monstersShamanFill() || config.monstersShamanLabel())
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
                next.add(new Marker(npc, location, npc.getWorldLocation().getPlane()));
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
        private final JCheckBox pulse = new JCheckBox("Gentle outline pulse");
        private final JCheckBox tiles = new JCheckBox("Mark current minion tiles");
        private final JCheckBox outline = new JCheckBox("Outline minion models");
        private final JCheckBox fill = new JCheckBox("Fill minion silhouettes");
        private final JCheckBox label = new JCheckBox("Show Spawn above heads");
        private final JButton color = new JButton("Tile colour");
        private final JButton modelColor = new JButton("Model / outline colour");
        private final JSlider tileOpacity = new JSlider(0,100);
        private final JSlider modelOpacity = new JSlider(0,100);
        private final JSlider fillOpacity = new JSlider(0,100);
        private final JTextArea status = OneManCards.text(suggestion);
        private boolean updating;

        Panel()
        {
            super(new BorderLayout());
            OneManCards cards = new OneManCards();
            cards.card(OneManCards.banner("Monsters Hub • Test", "Lizardman Shaman • local helper"));
            JPanel settings = OneManCards.card();
            OneManCards controls = new OneManCards();
            controls.setOpaque(false);
            profile.setName("monstersProfile");
            tiles.setName("monstersShamanTiles");
            tiles.setOpaque(false);
            controls.card(profile);
            JPanel looks = new JPanel(new BorderLayout(0,4)); looks.setOpaque(false);
            JToggleButton expandLooks = new JToggleButton("Appearance presets");
            expandLooks.setName("monstersAppearanceToggle");
            OneManCards lookControls = new OneManCards();
            JComboBox<ShamanAppearance> presets = new JComboBox<>(ShamanAppearance.values());
            presets.setName("monstersAppearancePreset");
            JButton apply = new JButton("Apply selected look"); apply.setName("monstersAppearanceApply");
            apply.setToolTipText("Replaces visual colours, visibility and pulse settings; leaves monster profile and Slayer settings unchanged");
            pulse.setOpaque(false); pulse.setName("monstersShamanPulse");
            lookControls.card(presets); lookControls.card(apply); lookControls.card(pulse);
            lookControls.card(OneManCards.text("Pulse stays at the same gentle tempo. It does not indicate when a spawn will explode."));
            lookControls.setVisible(false);
            expandLooks.addActionListener(event -> { lookControls.setVisible(expandLooks.isSelected()); revalidate(); repaint(); });
            apply.addActionListener(event -> {
                ShamanAppearance selected = (ShamanAppearance) presets.getSelectedItem();
                if (selected != null) clientThread.invokeLater(() -> { if (active) selected.apply(configs); });
            });
            pulse.addActionListener(event -> { if (!updating) configs.setConfiguration("one-man-sync", "monstersShamanPulse", pulse.isSelected()); });
            looks.add(expandLooks, BorderLayout.NORTH); looks.add(lookControls, BorderLayout.CENTER);
            controls.card(looks);
            controls.card(tiles); controls.card(color);
            controls.card(opacity("Tile visibility", tileOpacity, "monstersShamanTileOpacity"));
            for (JCheckBox box : new JCheckBox[]{outline, fill, label}) { box.setOpaque(false); controls.card(box); }
            outline.setName("monstersShamanOutline"); fill.setName("monstersShamanFill"); label.setName("monstersShamanLabel");
            controls.card(modelColor);
            controls.card(opacity("Outline / label visibility", modelOpacity, "monstersShamanModelOpacity"));
            controls.card(opacity("Fill visibility", fillOpacity, "monstersShamanFillOpacity"));
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
            outline.addActionListener(event -> { if (!updating) configs.setConfiguration("one-man-sync", "monstersShamanOutline", outline.isSelected()); });
            fill.addActionListener(event -> { if (!updating) configs.setConfiguration("one-man-sync", "monstersShamanFill", fill.isSelected()); });
            label.addActionListener(event -> { if (!updating) configs.setConfiguration("one-man-sync", "monstersShamanLabel", label.isSelected()); });
            modelColor.addActionListener(event -> {
                Color selected = JColorChooser.showDialog(this, "Shaman spawn model colour", config.monstersShamanModelColor());
                if (selected != null) configs.setConfiguration("one-man-sync", "monstersShamanModelColor", selected);
            });
            color.addActionListener(event -> {
                Color selected = JColorChooser.showDialog(this, "Shaman minion tile colour", config.monstersShamanColor());
                if (selected != null) configs.setConfiguration("one-man-sync", "monstersShamanColor", selected);
            });
            refresh(suggestion);
        }

        private JPanel opacity(String title, JSlider slider, String key)
        {
            JPanel row = new JPanel(new BorderLayout(0,2)); row.setOpaque(false);
            JLabel caption = new JLabel(title); caption.setFont(caption.getFont().deriveFont(11f));
            row.add(caption, BorderLayout.NORTH); row.add(slider, BorderLayout.CENTER);
            slider.setName(key); slider.setOpaque(false); slider.setMajorTickSpacing(50);
            slider.setPaintTicks(true); slider.setPaintLabels(true);
            slider.addChangeListener(event -> {
                if (!updating && !slider.getValueIsAdjusting()) configs.setConfiguration("one-man-sync", key, slider.getValue());
            });
            return row;
        }

        void refresh(String message)
        {
            updating = true;
            profile.setSelectedItem(config.monstersProfile());
            tiles.setSelected(config.monstersShamanTiles());
            pulse.setSelected(config.monstersShamanPulse());
            outline.setSelected(config.monstersShamanOutline());
            fill.setSelected(config.monstersShamanFill());
            label.setSelected(config.monstersShamanLabel());
            if (!tileOpacity.getValueIsAdjusting()) tileOpacity.setValue(config.monstersShamanTileOpacity());
            if (!modelOpacity.getValueIsAdjusting()) modelOpacity.setValue(config.monstersShamanModelOpacity());
            if (!fillOpacity.getValueIsAdjusting()) fillOpacity.setValue(config.monstersShamanFillOpacity());
            color.setForeground(config.monstersShamanColor());
            modelColor.setForeground(config.monstersShamanModelColor());
            status.setText(message);
            updating = false;
            revalidate(); repaint();
        }
    }
}
