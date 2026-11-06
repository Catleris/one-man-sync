package lt.oneman.sync;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
final class CombatInfoPage extends Overlay
{
    private static final Color GOLD = new Color(237, 194, 90);
    private static final Color MUTED = new Color(188, 178, 151);
    private static final Color GREEN = new Color(153, 215, 154);
    private final Set<Integer> bankWeapons = new HashSet<>();
    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    @Inject private ItemManager items;
    @Inject private SpriteManager sprites;
    @Inject private ElementalWeaknessOverlay weakness;
    private Widget pageAnchor;
    private int bookId;
    private int tintedSpriteId = -1;
    private BufferedImage tintedBook;
    private BufferedImage[] statIcons;
    private String bankHint;
    private TargetInfoData.Stats hintTarget;
    private boolean hintDirty;

    CombatInfoPage()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    void onMenuOpened(MenuOpened event)
    {
        if (!config.monstersTargetInfo()) return;
        MenuEntry[] entries = event.getMenuEntries();
        for (int i = entries.length - 1; i >= 0; i--)
        {
            int icon = magicIcon(entries[i].getParam1());
            if (icon == -1 || entries[i].getWidget() == null) continue;
            Menu menu = client.getMenu();
            menu.createMenuEntry(i).setOption(isOpen() ? "Close info" : "Info")
                .setTarget(entries[i].getTarget()).setType(MenuAction.RUNELITE)
                .onClick(ignored -> toggle(icon));
            event.setMenuEntries(menu.getMenuEntries());
            return;
        }
    }

    private static int magicIcon(int id)
    {
        switch (id)
        {
            case InterfaceID.Toplevel.STONE6:
            case InterfaceID.Toplevel.ICON6: return InterfaceID.Toplevel.ICON6;
            case InterfaceID.ToplevelOsrsStretch.STONE6:
            case InterfaceID.ToplevelOsrsStretch.ICON6: return InterfaceID.ToplevelOsrsStretch.ICON6;
            case InterfaceID.ToplevelPreEoc.STONE6:
            case InterfaceID.ToplevelPreEoc.ICON6: return InterfaceID.ToplevelPreEoc.ICON6;
            default: return -1;
        }
    }

    private void toggle(int icon)
    {
        if (isOpen())
        {
            close();
            return;
        }
        Widget inventory = client.getWidget(InterfaceID.INVENTORY, 0);
        pageAnchor = visible(inventory) ? inventory : client.getWidget(InterfaceID.MagicSpellbook.UNIVERSE);
        bookId = icon;
        hintDirty = true;
    }

    void close() { pageAnchor = null; }

    void clearBank()
    {
        bankWeapons.clear();
        hintDirty = true;
    }

    void bankChanged(Item[] snapshot)
    {
        clearBank();
        if (snapshot == null) return;
        for (Item item : snapshot)
            if (item != null && item.getId() > 0 && item.getQuantity() > 0) bankWeapons.add(item.getId());
    }

    private String bankHint(TargetInfoData.Stats stats)
    {
        if (bankWeapons.isEmpty()) return "Open your bank to read its items.";
        if (stats == null || stats.melee() == null) return null;
        int minimum = Math.min(stats.stab, Math.min(stats.slash, stats.crush));
        int bestId = -1;
        int bestBonus = 0;
        for (int id : bankWeapons)
        {
            ItemStats item = items.getItemStats(id);
            ItemEquipmentStats equipment = item == null ? null : item.getEquipment();
            if (equipment == null || equipment.getSlot() != EquipmentInventorySlot.WEAPON.getSlotIdx()) continue;
            int bonus = Math.max(stats.stab == minimum ? equipment.getAstab() : 0,
                Math.max(stats.slash == minimum ? equipment.getAslash() : 0, stats.crush == minimum ? equipment.getAcrush() : 0));
            if (bonus > bestBonus || (bonus > 0 && bonus == bestBonus && id < bestId))
            {
                bestId = id;
                bestBonus = bonus;
            }
        }
        ItemComposition item = bestId == -1 ? null : items.getItemComposition(bestId);
        return item == null ? null : item.getName();
    }

    boolean coversPointer()
    {
        if (!isOpen()) return false;
        Point mouse = client.getMouseCanvasPosition();
        return mouse != null && pageAnchor.getBounds().contains(mouse.getX(), mouse.getY());
    }

    private boolean isOpen()
    {
        return config.monstersTargetInfo() && visible(pageAnchor);
    }

    private static boolean visible(Widget widget)
    {
        if (widget == null || widget.isHidden()) return false;
        Rectangle bounds = widget.getBounds();
        return bounds != null && bounds.width >= 130 && bounds.height >= 160;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!isOpen())
        {
            close();
            return null;
        }
        drawBook(graphics);
        Rectangle bounds = pageAnchor.getBounds();
        Graphics2D page = (Graphics2D) graphics.create();
        try
        {
            page.clip(bounds);
            page.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            page.setPaint(new GradientPaint(bounds.x, bounds.y, new Color(35, 45, 38),
                bounds.x, bounds.y + bounds.height, new Color(21, 29, 25)));
            page.fill(bounds);
            page.setColor(new Color(145, 121, 67));
            page.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
            page.setColor(new Color(66, 77, 60));
            page.drawRect(bounds.x + 2, bounds.y + 2, bounds.width - 5, bounds.height - 5);

            int x = bounds.x + 7;
            int y = bounds.y;
            int width = bounds.width - 14;
            page.setFont(FontManager.getRunescapeFont());
            page.setColor(new Color(61, 67, 48));
            page.fillRoundRect(x, y + 6, width, 21, 5, 5);
            centered(page, "Target Info", x, width, y + 21, GOLD);
            page.setColor(new Color(44, 55, 44));
            page.fillRoundRect(x, y + 33, width, 33, 6, 6);
            page.setFont(FontManager.getRunescapeSmallFont());
            page.setColor(MUTED);
            page.drawString("LAST TARGET", x + 6, y + 44);
            page.setFont(FontManager.getRunescapeFont());
            page.setColor(GOLD);
            String name = weakness.lastName();
            page.drawString(fit(page, name == null ? "No target selected" : name, width - 12), x + 6, y + 59);

            TargetInfoData.Stats stats = weakness.lastStats();
            if (stats == null)
            {
                page.setColor(MUTED);
                page.drawString(name == null ? "Use Check weakness on an NPC." : "Reference stats unavailable.", x + 3, y + 88);
                return null;
            }
            page.setColor(new Color(39, 55, 43));
            page.fillRoundRect(x, y + 72, width, 22, 5, 5);
            row(page, "Element", value(ElementalWeaknessOverlay.elemental(stats)), x + 5, width - 10, y + 87, GREEN);
            page.setFont(FontManager.getRunescapeSmallFont());
            row(page, "Lowest melee", value(stats.melee()), x + 3, width - 6, y + 105, GOLD);
            drawCombatTiles(page, stats, x, y + 112, width);

            row(page, "Ranged L / S / H", value(stats.light) + " / " + value(stats.standard) + " / " + value(stats.heavy),
                x + 3, width - 6, y + 179, Color.WHITE);
            row(page, "Def / Magic lvl", value(stats.defence) + " / " + value(stats.magicLevel),
                x + 3, width - 6, y + 193, Color.WHITE);
            row(page, "Base HP", value(stats.hp), x + 3, width - 6, y + 207, Color.WHITE);
            row(page, "Attack speed", stats.speed == null || stats.speed <= 0 ? "—" : stats.speed + " ticks",
                x + 3, width - 6, y + 221, Color.WHITE);

            if (config.monstersWeaknessBankGear() && (hintDirty || hintTarget != stats))
            {
                hintTarget = stats;
                bankHint = bankHint(stats);
                hintDirty = false;
            }
            if (config.monstersWeaknessBankGear() && bankHint != null && bounds.height >= 253)
            {
                page.setColor(new Color(44, 55, 44));
                page.fillRoundRect(x, y + 228, width, 22, 5, 5);
                row(page, "Bank weapon", bankHint, x + 4, width - 8, y + 243, GOLD);
            }
        }
        finally
        {
            page.dispose();
        }
        return null;
    }

    private void drawBook(Graphics2D graphics)
    {
        Widget book = client.getWidget(bookId);
        if (book == null || book.isHidden()) return;
        Rectangle bounds = book.getBounds();
        int spriteId = book.getSpriteId();
        if (bounds == null || spriteId < 0) return;
        if (tintedBook == null || tintedSpriteId != spriteId)
        {
            BufferedImage original = sprites.getSprite(spriteId, 0);
            if (original == null) return;
            tintedBook = brighten(original);
            tintedSpriteId = spriteId;
        }
        graphics.drawImage(tintedBook, bounds.x, bounds.y, bounds.width, bounds.height, null);
    }

    static BufferedImage brighten(BufferedImage original)
    {
        BufferedImage tinted = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < original.getHeight(); y++)
        {
            for (int x = 0; x < original.getWidth(); x++)
            {
                int pixel = original.getRGB(x, y);
                if ((pixel >>> 24) == 0) continue;
                int red = lighten((pixel >>> 16) & 255);
                int green = lighten((pixel >>> 8) & 255);
                int blue = lighten(pixel & 255);
                tinted.setRGB(x, y, (pixel & 0xff000000) | (red << 16) | (green << 8) | blue);
            }
        }
        return tinted;
    }

    private static int lighten(int channel)
    {
        return channel + (255 - channel) / 4;
    }

    private void drawCombatTiles(Graphics2D page, TargetInfoData.Stats stats, int x, int y, int width)
    {
        if (statIcons == null)
            statIcons = new BufferedImage[]{items.getImage(ItemID.BRONZE_DAGGER), items.getImage(ItemID.BRONZE_SCIMITAR),
                items.getImage(ItemID.BRONZE_WARHAMMER), items.getImage(ItemID.MINDRUNE)};
        Integer[] bonuses = {stats.stab, stats.slash, stats.crush, stats.magic};
        String[] labels = {"Stab", "Slash", "Crush", "Magic"};
        int cell = width / 4;
        for (int i = 0; i < labels.length; i++)
        {
            int left = x + i * cell;
            int tileWidth = cell - 3;
            page.setPaint(new GradientPaint(left, y, new Color(61, 67, 48), left, y + 52, new Color(37, 45, 35)));
            page.fillRoundRect(left, y, tileWidth, 52, 5, 5);
            page.setColor(new Color(94, 99, 65));
            page.drawRoundRect(left, y, tileWidth - 1, 51, 5, 5);
            if (statIcons[i] != null) page.drawImage(statIcons[i], left + (tileWidth - 23) / 2, y + 2, 23, 23, null);
            page.setFont(FontManager.getRunescapeSmallFont());
            centered(page, labels[i], left, tileWidth, y + 35, MUTED);
            page.setFont(FontManager.getRunescapeFont());
            centered(page, value(bonuses[i]), left, tileWidth, y + 48, Color.WHITE);
        }
        page.setFont(FontManager.getRunescapeSmallFont());
    }

    private static void row(Graphics2D page, String label, String value, int x, int width, int y, Color color)
    {
        int labelWidth = page.getFontMetrics().stringWidth(label);
        String fitted = fit(page, value, width - labelWidth - 6);
        page.setColor(MUTED);
        page.drawString(label, x, y);
        page.setColor(color);
        page.drawString(fitted, x + width - page.getFontMetrics().stringWidth(fitted), y);
    }

    private static void centered(Graphics2D page, String text, int x, int width, int y, Color color)
    {
        page.setColor(color);
        page.drawString(text, x + (width - page.getFontMetrics().stringWidth(text)) / 2, y);
    }

    private static String fit(Graphics2D page, String text, int width)
    {
        if (page.getFontMetrics().stringWidth(text) <= width) return text;
        int end = text.length();
        while (end > 0 && page.getFontMetrics().stringWidth(text.substring(0, end) + "…") > width) end--;
        return end == 0 ? "" : text.substring(0, end) + "…";
    }

    private static String value(Object value) { return value == null ? "—" : value.toString(); }
}
