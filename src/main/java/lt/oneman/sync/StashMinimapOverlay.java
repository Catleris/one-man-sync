package lt.oneman.sync;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Only projects nearby static STASH coordinates; never scans the scene. */
final class StashMinimapOverlay extends Overlay
{
    private static final int[] MINIMAPS={InterfaceID.Toplevel.MINIMAP,InterfaceID.ToplevelOsrsStretch.MINIMAP,InterfaceID.ToplevelPreEoc.MINIMAP};
    @Inject private Client client;
    @Inject private OneManMapLocations maps;
    StashMinimapOverlay() { setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_WIDGETS); }
    @Override public Dimension render(Graphics2D graphics)
    {
        if(client.getGameState()!=GameState.LOGGED_IN || client.getLocalPlayer()==null) return null;
        Widget minimap=null;
        for(int id:MINIMAPS) { Widget widget=client.getWidget(id);if(widget!=null&&!widget.isHidden()) {minimap=widget;break;} }
        if(minimap==null || minimap.getWidth()<16 || minimap.getHeight()<16) return null;
        Rectangle bounds=minimap.getBounds();if(bounds==null) return null;
        double radius=Math.min(bounds.width,bounds.height)/2.0-6;
        Graphics2D g=(Graphics2D)graphics.create();
        g.clip(new Ellipse2D.Double(bounds.getCenterX()-radius,bounds.getCenterY()-radius,radius*2,radius*2));
        try {
            for(OneManMapCatalog.Site site:maps.minimapStashes()) for(WorldPoint world:site.points) {
                if(world.getPlane()!=client.getPlane()) continue;
                LocalPoint local=LocalPoint.fromWorld(client,world);if(local==null) continue;
                net.runelite.api.Point point=Perspective.localToMinimap(client,local);if(point==null) continue;
                g.setColor(Color.BLACK);g.fillOval(point.getX()-4,point.getY()-4,8,8);
                g.setColor(site.color);g.fillOval(point.getX()-3,point.getY()-3,6,6);
            }
        } finally { g.dispose(); }
        return null;
    }
}
