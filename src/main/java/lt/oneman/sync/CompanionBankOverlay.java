package lt.oneman.sync;
import java.awt.*;
import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
final class CompanionBankOverlay extends WidgetItemOverlay {
    @Inject private OneManSyncConfig config;
    volatile Set<Integer> needed=Collections.emptySet();
    CompanionBankOverlay(){showOnBank();}
    @Override public void renderItemOverlay(Graphics2D g,int id,WidgetItem item){
        if(config.highlightGoalItems() && needed.contains(id)){
            Rectangle r=item.getCanvasBounds();g.setColor(new Color(218,176,85,210));g.drawRect(r.x,r.y,r.width,r.height);
        }
    }
}
