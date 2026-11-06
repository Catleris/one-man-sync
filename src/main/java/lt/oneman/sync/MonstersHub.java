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
import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.HotkeyListener;
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
    @Inject private ElementalWeaknessOverlay weakness;
    @Inject private CombatInfoPage combatInfo;
    @Inject private GemCrabAlert gemCrab;
    @Inject private GuardiansFightGuide guardians;
    @Inject private KeyManager keys;
    private final HotkeyListener nextGuardianPhase = new HotkeyListener(() -> this.active && guardians.selected() && config.monstersGuardiansOverlay()
        ? config.monstersGuardiansNext() : Keybind.NOT_SET)
    {
        @Override public void hotkeyPressed()
        {
            clientThread.invokeLater(() -> {
                if (client.getGameState() == GameState.LOGGED_IN && config.monstersGuardiansOverlay())
                    moveGuardians(1);
            });
        }
    };
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
        overlays.add(weakness);
        overlays.add(combatInfo);
        active = true;
        scanPending = true;
        minions.clear();
        markers = Collections.emptyList();
        overlay.setHub(this);
        overlays.add(overlay);
        overlays.add(gemCrab);
        overlays.add(guardians);
        guardians.reset();
        keys.registerKeyListener(nextGuardianPhase);
        lastStatus = "";
    }

    void shutDown()
    {
        active = false;
        overlays.remove(weakness);
        overlays.remove(combatInfo);
        combatInfo.close();
        combatInfo.clearBank();
        weakness.clear();
        clear();
        slayerState = null;
        overlays.remove(overlay);
        overlays.remove(gemCrab);
        overlays.remove(guardians);
        keys.unregisterKeyListener(nextGuardianPhase);
        gemCrab.reset();
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
        gemCrab.appeared(event.getNpc());
        weakness.appeared(event.getNpc());
        if (enabled() && isMinion(event.getNpc().getId()))
        {
            minions.add(event.getNpc());
            publishMarkers();
        }
    }

    void onNpcDespawned(NpcDespawned event)
    {
        gemCrab.despawned(event.getNpc());
        weakness.despawned(event.getNpc());
        if (minions.remove(event.getNpc())) publishMarkers();
    }

    void onNpcChanged(NpcChanged event)
    {
        NPC npc = event.getNpc();
        gemCrab.appeared(npc);
        weakness.appeared(npc);
        if (enabled() && isMinion(npc.getId())) minions.add(npc);
        else minions.remove(npc);
        publishMarkers();
    }

    void onGameStateChanged(GameStateChanged event)
    {
        weakness.clear();
        combatInfo.close();
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING) combatInfo.clearBank();
        gemCrab.reset();
        clear();
        scanPending = true;
        suggestion = "Select a profile to use it without a Slayer task.";
        refresh();
    }

    void onConfigChanged()
    {
        if (!config.monstersTargetInfo()) weakness.clear();
        if (!config.monstersGemCrabAlert()) gemCrab.reset();
        if (!active) return;
        clear();
        scanPending = true;
        lastStatus = "";
        refresh();
    }

    void onMenuOpened(net.runelite.api.events.MenuOpened event)
    {
        if (!active) return;
        weakness.onMenuOpened(event);
        combatInfo.onMenuOpened(event);
    }

    void onBankChanged(net.runelite.api.Item[] items)
    {
        if (active) combatInfo.bankChanged(items);
    }

    void onPostMenuSort()
    {
        if (active && combatInfo.coversPointer()) client.getMenu().setMenuEntries(new net.runelite.api.MenuEntry[0]);
    }

    void onGameTick()
    {
        if (!active || client.getGameState() != GameState.LOGGED_IN) return;
        OneManSlayerState.LocalTask task = OneManSlayerState.localTask(slayerState.get(), client.getVarpValue(VarPlayerID.SLAYER_COUNT));
        gemCrab.tick();
        guardians.updateSupplies();
        suggestion = suggestsShaman(task)
            ? "Suggested for your task: Lizardman Shaman."
            : "Choose a profile above.";
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

    private void moveGuardians(int step)
    {
        if (!active || !guardians.selected()) return;
        if (step == 0) guardians.reset();
        else guardians.move(step);
        refresh();
    }

    private void refresh()
    {
        String status = suggestion + (markers.isEmpty()?"":"\nSpawns: "+markers.size());
        String key = guardians.selected() ? status + guardians.phase().title() + guardians.hammer() : status;
        if (key.equals(lastStatus)) return;
        lastStatus = key;
        SwingUtilities.invokeLater(() -> { if (active && panel != null) panel.refresh(status); });
    }

    private final class Panel extends JPanel
    {
        private final OneManCards guardiansCard = new OneManCards();
        private final OneManCards shamanCard = new OneManCards();
        private final JTextArea phaseText = OneManCards.text("");
        private final JCheckBox guardiansOverlay = new JCheckBox("Show fight card in game");
        private final JCheckBox targetEnabled = new JCheckBox("Elemental weakness checker");
        private final JComboBox<GemCrabSound> gemSound = new JComboBox<>(GemCrabSound.values());
        private final JCheckBox gemAlert = new JCheckBox("Gemstone Crab alert");
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
            cards.card(OneManCards.banner("Monsters Hub", "Weakness & monster tools"));
            targetEnabled.setOpaque(false);
            targetEnabled.setName("monstersTargetInfo");
            targetEnabled.addActionListener(event -> {
                if (!updating) configs.setConfiguration("one-man-sync", "monstersTargetInfo", targetEnabled.isSelected());
            });
            cards.card(targetEnabled);
            cards.card(OneManCards.text("Right-click a monster and select Check weakness. Right-click the Magic book for Info. Set duration to 0 to keep the marker across respawns."));
            OneManCards crabControls = new OneManCards();
            gemAlert.setName("monstersGemCrabAlert");
            gemAlert.setOpaque(false);
            gemAlert.addActionListener(event -> {
                if (!updating) configs.setConfiguration("one-man-sync", "monstersGemCrabAlert", gemAlert.isSelected());
            });
            crabControls.card(gemAlert);
            gemSound.setName("monstersGemCrabSound");
            gemSound.addActionListener(event -> {
                if (!updating) configs.setConfiguration("one-man-sync", "monstersGemCrabSound", gemSound.getSelectedItem());
            });
            JButton previewGemSound = new JButton("Preview sound");
            previewGemSound.addActionListener(event -> gemCrab.playSound((GemCrabSound) gemSound.getSelectedItem()));
            crabControls.card(gemSound);
            crabControls.card(previewGemSound);
            crabControls.card(OneManCards.text("Plays a melody and requests a desktop alert when your crab burrows. Click in the game to stop the green flash."));
            cards.card(crabControls);
            JPanel settings = OneManCards.card();
            OneManCards controls = new OneManCards();
            controls.setOpaque(false);
            profile.setName("monstersProfile");
            tiles.setName("monstersShamanTiles");
            tiles.setOpaque(false);
            controls.card(profile);
            guardiansCard.card(phaseText);
            JPanel phaseButtons = new JPanel(new java.awt.GridLayout(1, 3, 4, 0));
            String[] labels = {"Previous", "Reset", "Next"};
            for (int step = -1; step <= 1; step++)
            {
                int direction = step;
                JButton button = new JButton(labels[step + 1]);
                button.addActionListener(event -> clientThread.invokeLater(() -> moveGuardians(direction)));
                phaseButtons.add(button);
            }
            guardiansCard.card(phaseButtons);
            guardiansOverlay.setOpaque(false);
            guardiansOverlay.addActionListener(event -> {
                if (!updating) configs.setConfiguration("one-man-sync", "monstersGuardiansOverlay", guardiansOverlay.isSelected());
            });
            guardiansCard.card(guardiansOverlay);
            guardiansCard.card(OneManCards.text("Select phases manually. Set the optional next-phase hotkey in OneMan Sync settings."));
            controls.card(guardiansCard);
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
            shamanCard.card(looks);
            OneManCards tileControls=new OneManCards();tileControls.card(tiles);tileControls.card(color);
            tileControls.card(opacity("Tile visibility",tileOpacity,"monstersShamanTileOpacity"));
            shamanCard.card(OneManTheme.collapsible("Tile markers",tileControls));
            OneManCards modelControls=new OneManCards();
            for(JCheckBox box:new JCheckBox[]{outline,fill,label}){box.setOpaque(false);modelControls.card(box);}
            outline.setName("monstersShamanOutline");fill.setName("monstersShamanFill");label.setName("monstersShamanLabel");
            modelControls.card(modelColor);
            modelControls.card(opacity("Outline / label visibility",modelOpacity,"monstersShamanModelOpacity"));
            modelControls.card(opacity("Fill visibility",fillOpacity,"monstersShamanFillOpacity"));
            shamanCard.card(OneManTheme.collapsible("Spawn appearance",modelControls));
            controls.card(shamanCard);
            settings.add(controls, BorderLayout.NORTH);
            settings.add(status, BorderLayout.CENTER);
            cards.card(settings);
            JToggleButton help=new JToggleButton("Help");
            JTextArea helpText=OneManCards.text("Marks visible Shaman spawns. Pulsing is cosmetic, not an explosion timer. Chambers of Xeric is excluded.");
            helpText.setVisible(false);help.addActionListener(event->{helpText.setVisible(help.isSelected());revalidate();repaint();});
            cards.card(help);cards.card(helpText);
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
            targetEnabled.setSelected(config.monstersTargetInfo());
            gemAlert.setSelected(config.monstersGemCrabAlert());
            gemSound.setSelectedItem(config.monstersGemCrabSound());
            profile.setSelectedItem(config.monstersProfile());
            guardiansCard.setVisible(guardians.selected());
            shamanCard.setVisible(!guardians.selected());
            guardiansOverlay.setSelected(config.monstersGuardiansOverlay());
            GuardiansFightGuide.Phase phase = guardians.phase();
            phaseText.setText(phase.title() + "\n" + phase.style + "\n\n" + phase.reminder
                + (phase.diagram.isEmpty() ? "" : "\n" + phase.diagram) + "\n\n" + guardians.hammer());
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
