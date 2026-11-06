package lt.oneman.sync;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
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
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
final class CombatInfoPage extends Overlay
{
    private static final Color PAPER = new Color(248, 241, 218);
    private final Set<Integer> bankWeapons = new HashSet<>();
    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    @Inject private ItemManager items;
    @Inject private ElementalWeaknessOverlay weakness;
    private Widget pageAnchor;
    private int bookId;
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
        Widget book = client.getWidget(bookId);
        if (book != null && !book.isHidden() && book.getBounds() != null)
        {
            Rectangle bounds = book.getBounds();
            int x = bounds.x + bounds.width / 2;
            int y = bounds.y + bounds.height / 2;
            graphics.setColor(PAPER);
            graphics.fillPolygon(new int[]{x-8,x-2,x,x+2,x+8,x+8,x+2,x,x-2,x-8},
                new int[]{y-5,y-5,y-3,y-5,y-5,y+5,y+5,y+7,y+5,y+5},10);
            graphics.setColor(new Color(113, 99, 76));
            graphics.drawLine(x, y-3, x, y+6);
        }
        Rectangle bounds = pageAnchor.getBounds();
        Graphics2D page = (Graphics2D) graphics.create();
        try
        {
            page.clip(bounds);
            page.setColor(new Color(39, 33, 25));
            page.fill(bounds);
            page.setColor(PAPER);
            page.drawRect(bounds.x + 1, bounds.y + 1, bounds.width - 3, bounds.height - 3);
            int x = bounds.x + 8;
            int y = bounds.y + 22;
            page.drawString("Target Info", x, y);
            TargetInfoData.Stats stats = weakness.lastStats();
            String name = weakness.lastName();
            page.drawString(name == null ? "Use Check weakness on an NPC." : name, x, y + 20);
            if (stats != null)
            {
                page.drawString("Element: " + value(ElementalWeaknessOverlay.elemental(stats)), x, y + 45);
                page.drawString("Melee: " + value(stats.melee()), x, y + 63);
                page.drawString("Range: " + value(stats.ranged()), x, y + 81);
            }
            if (config.monstersWeaknessBankGear() && (hintDirty || hintTarget != stats))
            {
                hintTarget = stats;
                bankHint = bankHint(stats);
                hintDirty = false;
            }
            if (config.monstersWeaknessBankGear() && bankHint != null)
            {
                page.drawString("Bank weapon by melee bonus", x, y + 108);
                page.drawString(bankHint, x, y + 126);
            }
        }
        finally
        {
            page.dispose();
        }
        return null;
    }

    private static String value(String text) { return text == null ? "—" : text; }
}
