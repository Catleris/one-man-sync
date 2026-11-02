package lt.oneman.sync;
import java.awt.*;
import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
@Singleton
final class CompanionBankOverlay extends WidgetItemOverlay {
    @Inject private OneManSyncConfig config;
    volatile Set<Integer> needed=Collections.emptySet();
    volatile Map<Integer,Color> slayerNeeded=Collections.emptyMap();
    CompanionBankOverlay(){showOnBank();}
    @Override public void renderItemOverlay(Graphics2D g,int id,WidgetItem item){
        Color slayer=config.slayerBankItems() ? slayerNeeded.get(id) : null;
        if(slayer!=null) {
            Rectangle r=item.getCanvasBounds();g.setColor(slayer);g.drawRect(r.x+1,r.y+1,r.width-2,r.height-2);
        }
        if(config.companionRoadmapVisible() && config.highlightGoalItems() && needed.contains(id)){
            Rectangle r=item.getCanvasBounds();g.setColor(new Color(218,176,85,210));g.drawRect(r.x,r.y,r.width,r.height);
        }
    }
}
