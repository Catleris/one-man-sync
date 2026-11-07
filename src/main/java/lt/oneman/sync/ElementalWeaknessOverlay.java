package lt.oneman.sync;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
final class ElementalWeaknessOverlay extends Overlay
{
    private final TargetInfoData data = new TargetInfoData();
    private final Set<NPC> nearby = Collections.newSetFromMap(new IdentityHashMap<>());
    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    @Inject private ItemManager items;
    private NPC selected;
    private int selectedVariant;
    private TargetInfoData.Stats lastStats;
    private String lastName;
    private WorldView world;
    private String selectedName;
    private long selectedAt;

    ElementalWeaknessOverlay()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    void onMenuOpened(MenuOpened event)
    {
        if (!config.monstersTargetInfo() || client.getGameState() != GameState.LOGGED_IN) return;
        Menu menu = client.getMenu();
        MenuEntry[] entries = event.getMenuEntries();
        boolean added = false;
        for (int i = entries.length - 1; i >= 0; i--)
        {
            MenuEntry entry = entries[i];
            if (entry.getType() != MenuAction.EXAMINE_NPC) continue;
            NPC npc = entry.getNpc();
            if (npc == null) continue;
            menu.createMenuEntry(i)
                .setOption("Check weakness")
                .setTarget(entry.getTarget())
                .setType(MenuAction.RUNELITE)
                .onClick(ignored -> check(npc));
            added = true;
        }
        if (added) event.setMenuEntries(menu.getMenuEntries());
    }

    void check(NPC npc)
    {
        if (!config.monstersTargetInfo() || npc.isDead() || client.getGameState() != GameState.LOGGED_IN) return;
        clearSelection();
        TargetInfoData.Stats stats = stats(npc);
        String name = npc.getName();
        lastStats = stats;
        lastName = name;
        if (stats != null && name != null)
        {
            selected = npc;
            selectedVariant = npc.getTransformedComposition().getId();
            world = npc.getWorldView();
            selectedName = name;
            selectedAt = System.nanoTime();
            if (world != null && config.monstersWeaknessSameName())
                for (NPC candidate : world.npcs()) appeared(candidate);
        }
        if (config.monstersWeaknessChat())
        {
            String message = stats == null ? "No listed combat data." : label(stats);
            if (stats != null && elemental(stats) != null) message += " weakness.";
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", (name == null ? "NPC" : name) + ": " + message, null);
        }
    }

    void appeared(NPC npc)
    {
        if (selectedName == null || npc.getWorldView() != world || !selectedName.equals(npc.getName()) || npc.isDead()) return;
        NPCComposition variant = npc.getTransformedComposition();
        if ((selected == null || selected.isDead()) && variant != null && variant.getId() == selectedVariant)
        {
            selected = npc;
            nearby.remove(npc);
        }
        else if (npc != selected) nearby.add(npc);
    }

    void despawned(NPC npc)
    {
        if (npc == selected) selected = null;
        nearby.remove(npc);
    }

    void clear()
    {
        clearSelection();
        lastStats = null;
        lastName = null;
    }

    private void clearSelection()
    {
        selected = null;
        world = null;
        selectedName = null;
        nearby.clear();
    }

    TargetInfoData.Stats lastStats() { return lastStats; }
    String lastName() { return lastName; }

    private TargetInfoData.Stats stats(NPC npc)
    {
        NPCComposition variant = npc.getTransformedComposition();
        return variant == null ? null : data.find(variant.getId());
    }

    static String elemental(TargetInfoData.Stats stats)
    {
        return stats.percent != null && stats.percent > 0 && runeId(stats.element) != -1
            ? stats.percent + "% " + stats.element : null;
    }

    private static String label(TargetInfoData.Stats stats)
    {
        String elemental = elemental(stats);
        if (elemental != null) return elemental;
        String melee = stats.melee();
        String ranged = stats.ranged();
        if (melee == null && ranged == null) return "No clear weakness";
        if (melee == null) return "Range: " + ranged;
        return ranged == null ? melee : melee + " | Range: " + ranged;
    }

    private static int runeId(String element)
    {
        switch (element)
        {
            case "Air": return ItemID.AIRRUNE;
            case "Water": return ItemID.WATERRUNE;
            case "Earth": return ItemID.EARTHRUNE;
            case "Fire": return ItemID.FIRERUNE;
            default: return -1;
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (selectedName == null || !config.monstersTargetInfo() || client.getGameState() != GameState.LOGGED_IN) return null;
        int duration = config.monstersWeaknessDuration();
        if (duration > 0 && System.nanoTime() - selectedAt >= TimeUnit.SECONDS.toNanos(duration))
        {
            clearSelection();
            return null;
        }
        if (selected != null && !selected.isDead()) draw(graphics, selected);
        if (config.monstersWeaknessSameName())
            for (NPC npc : nearby)
                if (!npc.isDead() && selectedName.equals(npc.getName())) draw(graphics, npc);
        return null;
    }

    private void draw(Graphics2D graphics, NPC npc)
    {
        TargetInfoData.Stats stats = stats(npc);
        if (stats == null) return;
        String text = label(stats);
        Point point = npc.getCanvasTextLocation(graphics, text, npc.getLogicalHeight() + 24);
        if (point == null) return;
        Color color = config.monstersWeaknessColor();
        OverlayUtil.renderTextLocation(graphics, point, text, color == null ? Color.WHITE : color);
        if (config.monstersWeaknessRune() && elemental(stats) != null)
        {
            BufferedImage rune = items.getImage(runeId(stats.element));
            int x = point.getX() + graphics.getFontMetrics().stringWidth(text) / 2;
            if (rune != null) OverlayUtil.renderImageLocation(graphics, new Point(x - rune.getWidth() / 2,
                point.getY() - graphics.getFontMetrics().getHeight() - rune.getHeight()), rune);
        }
    }
}
