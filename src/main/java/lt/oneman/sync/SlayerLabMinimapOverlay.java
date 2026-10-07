package lt.oneman.sync;

import java.awt.*;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

/** Pulsing entrance indicator, drawn only when the point is visible on the minimap. */
final class SlayerLabMinimapOverlay extends Overlay
{
    private final Client client;
    private final OneManSlayerHelper plugin;
    @Inject SlayerLabMinimapOverlay(Client client, OneManSlayerHelper plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }
    @Override public Dimension render(Graphics2D graphics)
    {
        WorldPoint target = plugin.navigationTarget();
        if (target == null || client.getLocalPlayer() == null || target.getPlane() != client.getTopLevelWorldView().getPlane()
            || client.getTopLevelWorldView().isInstance()) return null;
        LocalPoint local = LocalPoint.fromWorld(client.getTopLevelWorldView(), target);
        boolean directionOnly = false;
        WorldPoint player = client.getLocalPlayer().getWorldLocation();
        if ((player.getY() >= 6400) != (target.getY() >= 6400)) return null;
        if (local == null || player.distanceTo(target) > 15)
        {
            double dx = target.getX() - player.getX(), dy = target.getY() - player.getY();
            double length = Math.hypot(dx, dy);
            if (length == 0) return null;
            WorldPoint probe = new WorldPoint(player.getX() + (int) Math.round(15 * dx / length),
                player.getY() + (int) Math.round(15 * dy / length), player.getPlane());
            local = LocalPoint.fromWorld(client.getTopLevelWorldView(), probe);
            directionOnly = true;
        }
        if (local == null) return null;
        net.runelite.api.Point point = Perspective.localToMinimap(client, local);
        if (point == null) return null;
        boolean bright = (client.getTickCount() / 2) % 2 == 0;
        graphics.setColor(bright ? new Color(100, 220, 255) : new Color(30, 140, 240));
        graphics.setStroke(new BasicStroke(2));
        if (directionOnly)
        {
            net.runelite.api.Point centre = Perspective.localToMinimap(client, client.getLocalPlayer().getLocalLocation());
            if (centre == null) return null;
            double angle = Math.atan2(point.getY() - centre.getY(), point.getX() - centre.getX());
            double vx = Math.cos(angle), vy = Math.sin(angle);
            int x = point.getX(), y = point.getY();
            graphics.fillPolygon(new int[]{x + (int)(7*vx), x + (int)(-5*vx-5*vy), x + (int)(-5*vx+5*vy)},
                new int[]{y + (int)(7*vy), y + (int)(-5*vy+5*vx), y + (int)(-5*vy-5*vx)}, 3);
            return null;
        }
        graphics.drawOval(point.getX() - 7, point.getY() - 7, 14, 14);
        graphics.fillPolygon(new int[]{point.getX()-5,point.getX()+5,point.getX()},
            new int[]{point.getY()-11,point.getY()-11,point.getY()-3},3);
        return null;
    }
}
