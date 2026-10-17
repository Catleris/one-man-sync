package lt.oneman.sync;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.*;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;

/** Local-only hub. All item definition and scene access stays on the client thread. */
@Singleton
final class ClueHub
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ItemManager items;
    @Inject private OneManSyncConfig config;
    @Inject private ConfigManager configs;
    @Inject private OverlayManager overlays;
    @Inject private ClueGroundOverlay groundOverlay;
    @Inject private ClueWidgetOverlay widgetOverlay;
    private ClueItemCatalog catalog;
    private Panel panel;
    private volatile boolean active;
    private boolean scanPending;
    private String profile = "";
    private final ClueRewardCapture caskets = new ClueRewardCapture();
    private final Map<Integer, Set<ClueTier>> observedRewards = new HashMap<>();
    private final Map<TileItem, GroundItem> ground = new IdentityHashMap<>();
    private final Map<Integer, Seen> recent = new LinkedHashMap<>();
    static final class GroundItem
    {
        final Tile tile;
        final String name;
        final ClueItemCatalog.Tags tags;
        GroundItem(Tile tile, String name, ClueItemCatalog.Tags tags) { this.tile = tile; this.name = name; this.tags = tags; }
    }
    private static final class Seen
    {
        final String name, origin;
        final int tick;
        final ClueItemCatalog.Tags tags;
        Seen(String name, String origin, ClueItemCatalog.Tags tags, int tick) { this.name = name; this.origin = origin; this.tags = tags; this.tick = tick; }
    }
    void startUp()
    {
        catalog = new ClueItemCatalog(); active = true; profile = ""; scanPending = true;
        ground.clear(); recent.clear(); overlays.add(groundOverlay); overlays.add(widgetOverlay);
    }
    void shutDown()
    {
        active = false; caskets.clear(); observedRewards.clear(); ground.clear(); recent.clear(); overlays.remove(groundOverlay); overlays.remove(widgetOverlay);
        SwingUtilities.invokeLater(() -> panel = null);
    }
    PluginPanel companionPanel() { if (panel == null) panel = new Panel(); return panel; }
    boolean enabled() { return active && config.clueHubEnabled(); }
    Set<ClueTier> tiers(ClueItemCatalog.Tags tags) { return tags.visible(config.clueRequiredItems(), config.clueRewardItems(), config.clueCommonRewards()); }
    String kind(ClueItemCatalog.Tags tags) { return tags.kind(config.clueRequiredItems(), config.clueRewardItems(), config.clueCommonRewards()); }
    Collection<GroundItem> groundItems() { return ground.values(); }
    ClueItemCatalog.Tags classify(int id)
    {
        ItemComposition definition = items.getItemComposition(id);
        if (definition.getNote() != -1) { id = definition.getLinkedNoteId(); definition = items.getItemComposition(id); }
        ClueItemCatalog.Tags base = catalog.classify(id, definition.getName());
        if (!observedRewards.containsKey(id)) return base;
        Set<ClueTier> reward = EnumSet.noneOf(ClueTier.class); reward.addAll(base.reward); reward.addAll(observedRewards.get(id));
        return new ClueItemCatalog.Tags(base.required, reward, base.common, base.scroll);
    }
    void onGameStateChanged(GameStateChanged event)
    {
        GameState state = event.getGameState();
        if (state != GameState.LOGGED_IN) ground.clear();
        if (state == GameState.LOGGED_IN) scanPending = true;
        if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
        {
            recent.clear(); observedRewards.clear(); caskets.clear(); profile = ""; catalog.clearCache(); refresh();
        }
    }
    void onConfigChanged() { if (!active) return; caskets.clear(); ground.clear(); catalog.clearCache(); scanPending = true; refresh(); }
    void onGameTick()
    {
        if (!active || client.getGameState() != GameState.LOGGED_IN) return;
        String next = Objects.toString(configs.getRSProfileKey(), "");
        if (!next.equals(profile)) { profile = next; recent.clear(); observedRewards.clear(); caskets.clear(); ground.clear(); scanPending = true; refresh(); }
        caskets.tick(client.getTickCount());
        // Once on enabling/login/region load; never scan the scene each tick or frame.
        if (enabled() && scanPending)
        {
            scanPending = false;
            for (Tile[][] plane : client.getScene().getTiles()) for (Tile[] column : plane) for (Tile tile : column)
                if (tile != null && tile.getGroundItems() != null) for (TileItem item : tile.getGroundItems()) track(tile, item, false);
        }
    }
    void onItemSpawned(ItemSpawned event) { if (enabled()) track(event.getTile(), event.getItem(), true); }
    void onItemDespawned(ItemDespawned event) { ground.remove(event.getItem()); }
    private void track(Tile tile, TileItem item, boolean record)
    {
        ClueItemCatalog.Tags tags = classify(item.getId());
        if (tiers(tags).isEmpty()) return;
        String name = items.getItemComposition(item.getId()).getName();
        ground.put(item, new GroundItem(tile, name, tags));
        if (record) remember(item.getId(), name, "Ground item seen (source unknown)", tags);
    }
    void onNpcLootReceived(net.runelite.client.events.NpcLootReceived event)
    {
        if (!enabled()) return;
        for (net.runelite.client.game.ItemStack item : event.getItems())
        {
            ClueItemCatalog.Tags tags = classify(item.getId());
            if (!tiers(tags).isEmpty()) remember(item.getId(), items.getItemComposition(item.getId()).getName(), "NPC drop: " + event.getNpc().getName(), tags);
        }
    }
    private void remember(int id, String name, String origin, ClueItemCatalog.Tags tags)
    {
        Seen previous = recent.get(id);
        if (previous != null && previous.tick == client.getTickCount() && origin.startsWith("Ground") && previous.origin.startsWith("NPC")) return;
        recent.remove(id); recent.put(id, new Seen(name, origin, tags, client.getTickCount()));
        while (recent.size() > 25) recent.remove(recent.keySet().iterator().next());
        refresh();
    }
    void onChatMessage(ChatMessage event)
    {
        if (!enabled() || (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.MESBOX)) return;
        if (caskets.completion(net.runelite.client.util.Text.removeTags(event.getMessage()), client.getTickCount())) captureCasket();
    }
    void onItemContainerChanged(ItemContainerChanged event)
    {
        if (!enabled() || event.getContainerId() != net.runelite.api.gameval.InventoryID.TRAIL_REWARDINV) return;
        if (caskets.containerChanged(client.getTickCount())) captureCasket();
    }
    private void captureCasket()
    {
        ItemContainer container = client.getItemContainer(net.runelite.api.gameval.InventoryID.TRAIL_REWARDINV);
        if (container == null || caskets.tier() == null) return;
        ClueTier tier = caskets.tier();
        boolean found = false;
        for (Item item : container.getItems())
        {
            if (item.getId() < 0 || item.getQuantity() <= 0) continue;
            int id = items.canonicalize(item.getId());
            observedRewards.computeIfAbsent(id, k -> EnumSet.noneOf(ClueTier.class)).add(tier);
            remember(id, items.getItemComposition(id).getName(), "Opened " + tier.label + " casket (observed)", classify(id));
            found = true;
        }
        if (found) caskets.consumed();
    }
    void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (!enabled() || !config.clueMenuMarkers()) return;
        MenuEntry entry = event.getMenuEntry(); int id = entry.getItemId(); MenuAction action = entry.getType();
        if (action == MenuAction.GROUND_ITEM_FIRST_OPTION || action == MenuAction.GROUND_ITEM_SECOND_OPTION
            || action == MenuAction.GROUND_ITEM_THIRD_OPTION || action == MenuAction.GROUND_ITEM_FOURTH_OPTION
            || action == MenuAction.GROUND_ITEM_FIFTH_OPTION || action == MenuAction.EXAMINE_ITEM_GROUND) id = entry.getIdentifier();
        else if (id < 0) return;
        if (id < 0 || entry.getTarget().contains("[Clue:")) return;
        ClueItemCatalog.Tags tags = classify(id); Set<ClueTier> tiers = tiers(tags); if (tiers.isEmpty()) return;
        StringBuilder suffix = new StringBuilder(" <col=aaaaaa>[Clue: " + kind(tags) + "</col>");
        for (ClueTier tier : tiers) suffix.append(" <col=").append(tier.hex()).append(">● ").append(tier.shortName).append("</col>");
        entry.setTarget(entry.getTarget() + suffix + "<col=aaaaaa>]</col>");
    }
    private void lookup(String query)
    {
        clientThread.invokeLater(() -> {
            if (!active) return;
            String result = "Enter an item ID.";
            try
            {
                int id = Integer.parseInt(query.trim());
                if (id >= 0 && id < 100000) result = describe(items.getItemComposition(id).getName(), classify(id));
            }
            catch (NumberFormatException ignored) { result = "Use the numeric in-game item ID for this test build."; }
            final String text = result;
            SwingUtilities.invokeLater(() -> { if (active && panel != null) panel.result.setText(text); });
        });
    }
    static String describe(String name, ClueItemCatalog.Tags tags)
    {
        StringBuilder text = new StringBuilder(name);
        if (tags.scroll != null) text.append("\nClue / casket: ").append(tags.scroll.label);
        if (!tags.required.isEmpty()) text.append("\nNeeded for emote / Charlie clues: ").append(labels(tags.required));
        if (!tags.reward.isEmpty()) text.append("\nReward table: ").append(labels(tags.reward));
        if (!tags.common.isEmpty()) text.append("\nCommon reward table: ").append(labels(tags.common));
        if (tags.scroll == null && tags.required.isEmpty() && tags.reward.isEmpty() && tags.common.isEmpty()) text.append("\nNo match in bundled clue catalogue.");
        return text.toString();
    }
    private static String labels(Set<ClueTier> tiers)
    {
        List<String> names = new ArrayList<>(); for (ClueTier tier : tiers) names.add(tier.label); return String.join(", ", names);
    }
    private void refresh()
    {
        List<Seen> snapshot = new ArrayList<>(recent.values()); Collections.reverse(snapshot); StringBuilder text = new StringBuilder();
        for (Seen seen : snapshot) text.append(describe(seen.name, seen.tags)).append("\n").append(seen.origin).append("\n\n");
        String value = text.length() == 0 ? "No clue-related drops observed in this session yet." : text.toString();
        SwingUtilities.invokeLater(() -> { if (active && panel != null) { panel.recent.setText(value); panel.status.setText(config.clueHubEnabled() ? "Clue Hub — TEST" : "Clue Hub — disabled"); } });
    }
    private final class Panel extends PluginPanel
    {
        final JLabel status = new JLabel("Clue Hub — TEST");
        final JTextArea recent = area(), result = area();
        Panel()
        {
            setLayout(new BorderLayout(0, 8));
            recent.setText("No clue-related drops observed in this session yet.");
            JPanel header = new JPanel(); header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS)); header.add(status);
            for (ClueTier tier : ClueTier.values()) { JLabel label = new JLabel("● " + tier.label); label.setForeground(tier.color); header.add(label); }
            JTextArea help = area(); help.setText("Req = needed for an emote / Charlie clue.\nReward table = possible clue source, not proof of where this copy came from.\nMultiple colours = multiple tiers.\nCommon rewards are hidden by default.\nSherlock skill tasks are not indexed in this test.\nGround labels are separate from Ground Items."); header.add(help);
            JPanel options = new JPanel(); options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
            toggle(options, "Enable Clue Hub", "clueHubEnabled", config.clueHubEnabled());
            toggle(options, "Ground labels", "clueGroundMarkers", config.clueGroundMarkers());
            toggle(options, "Inventory / bank dots", "clueWidgetMarkers", config.clueWidgetMarkers());
            toggle(options, "Item menu labels", "clueMenuMarkers", config.clueMenuMarkers());
            toggle(options, "Required items", "clueRequiredItems", config.clueRequiredItems());
            toggle(options, "Reward table items", "clueRewardItems", config.clueRewardItems());
            toggle(options, "Include common rewards", "clueCommonRewards", config.clueCommonRewards()); header.add(options);
            JTextField query = new JTextField(); JButton search = new JButton("Look up item ID");
            search.addActionListener(e -> { if (!query.getText().trim().isEmpty()) lookup(query.getText()); }); query.addActionListener(e -> search.doClick());
            header.add(query); header.add(search); header.add(result);
            JButton guide = new JButton("Open clue guide"); guide.addActionListener(e -> LinkBrowser.browse("https://oldschool.runescape.wiki/w/Treasure_Trails")); header.add(guide);
            JTextArea source = area(); source.setText("Data: RuneLite emote clues (BSD-2-Clause), OSRS Wiki reward tables (CC BY-NC-SA 3.0), snapshot 2026-10-17. Offline data can change after game updates."); header.add(source);
            JPanel contents = new JPanel(new BorderLayout(0,8)); contents.add(header,BorderLayout.NORTH); contents.add(recent,BorderLayout.CENTER); add(new JScrollPane(contents),BorderLayout.CENTER);
        }
        private void toggle(JPanel target, String label, String key, boolean selected)
        {
            JCheckBox box = new JCheckBox(label, selected); box.addActionListener(e -> configs.setConfiguration("one-man-sync", key, box.isSelected())); target.add(box);
        }
        private JTextArea area()
        {
            JTextArea area = new JTextArea(); area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
            area.setBackground(new Color(30,33,38)); area.setForeground(new Color(220,220,220)); area.setBorder(BorderFactory.createEmptyBorder(5,5,5,5)); return area;
        }
    }
}
