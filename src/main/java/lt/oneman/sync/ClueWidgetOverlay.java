package lt.oneman.sync;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

final class ClueWidgetOverlay extends WidgetItemOverlay
{
    @Inject private ClueHub hub;
    @Inject private OneManSyncConfig config;
    ClueWidgetOverlay() { showOnInventory(); showOnBank(); showOnEquipment(); }
    @Override public void renderItemOverlay(Graphics2D graphics, int id, WidgetItem item)
    {
        if (id < 0 || !hub.enabled() || !config.clueWidgetMarkers()) return;
        Set<ClueTier> tiers = hub.tiers(hub.classify(id));
        Rectangle bounds = item.getCanvasBounds(); int x = bounds.x + bounds.width - 7;
        for (ClueTier tier : tiers)
        {
            graphics.setColor(Color.BLACK); graphics.fillOval(x - 1, bounds.y + bounds.height - 8, 8, 8);
            graphics.setColor(tier.color); graphics.fillOval(x, bounds.y + bounds.height - 7, 6, 6); x -= 6;
        }
    }
}
