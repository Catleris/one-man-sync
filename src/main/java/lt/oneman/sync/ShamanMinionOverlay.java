package lt.oneman.sync;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Current NPC occupancy only, never a blast radius or future hazard. */
final class ShamanMinionOverlay extends Overlay
{
    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    private volatile MonstersHub hub;

    ShamanMinionOverlay()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    void setHub(MonstersHub hub) { this.hub = hub; }

    @Override public Dimension render(Graphics2D graphics)
    {
        if (hub == null || !hub.enabled() || client.getGameState() != GameState.LOGGED_IN) return null;
        Color color = config.monstersShamanColor();
        for (MonstersHub.Marker marker : hub.markers())
        {
            if (marker.plane != client.getTopLevelWorldView().getPlane()) continue;
            Polygon tile = Perspective.getCanvasTilePoly(client, marker.location);
            if (tile == null) continue;
            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(35, color.getAlpha())));
            graphics.fillPolygon(tile);
            graphics.setColor(color);
            graphics.drawPolygon(tile);
        }
        return null;
    }
}
