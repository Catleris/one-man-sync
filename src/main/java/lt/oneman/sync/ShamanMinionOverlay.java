package lt.oneman.sync;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/** Highlights currently observed NPCs only; no prediction or blast radius. */
final class ShamanMinionOverlay extends Overlay
{
    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    @Inject private ModelOutlineRenderer outlines;
    private volatile MonstersHub hub;

    ShamanMinionOverlay()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    void setHub(MonstersHub hub) { this.hub = hub; }

    static Color opacity(Color color, int percent)
    {
        int alpha = color.getAlpha() * Math.max(0, Math.min(100, percent)) / 100;
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    // One global cosmetic phase: never restarted by spawning, scene changes or configuration.
    static int pulsePercent(long monotonicNanos)
    {
        long period = 2_400_000_000L;
        double phase = Math.floorMod(monotonicNanos, period) / (double) period;
        return (int) Math.round(80 + 20 * Math.cos(2 * Math.PI * phase));
    }

    @Override public Dimension render(Graphics2D graphics)
    {
        MonstersHub currentHub = hub;
        if (currentHub == null || client.getGameState() != GameState.LOGGED_IN || !currentHub.enabled()) return null;
        Color tileColor = opacity(config.monstersShamanColor(), config.monstersShamanTileOpacity());
        Color modelColor = opacity(config.monstersShamanModelColor(), config.monstersShamanModelOpacity());
        Color outlineColor = config.monstersShamanPulse()
            ? opacity(modelColor, pulsePercent(System.nanoTime())) : modelColor;
        Color fillColor = opacity(config.monstersShamanModelColor(), config.monstersShamanFillOpacity());
        for (MonstersHub.Marker marker : currentHub.markers())
        {
            NPC npc = marker.npc;
            if (marker.plane != client.getTopLevelWorldView().getPlane()
                || npc.getWorldView() != client.getTopLevelWorldView() || !MonstersHub.isMinion(npc.getId())) continue;
            if (config.monstersShamanTiles() && tileColor.getAlpha() > 0)
            {
                Polygon tile = Perspective.getCanvasTilePoly(client, marker.location);
                if (tile != null)
                {
                    graphics.setColor(new Color(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue(), tileColor.getAlpha() * 16 / 100));
                    graphics.fillPolygon(tile);
                    graphics.setColor(tileColor); graphics.drawPolygon(tile);
                }
            }
            if (config.monstersShamanFill() && fillColor.getAlpha() > 0)
            {
                Shape hull = npc.getConvexHull();
                if (hull != null) { graphics.setColor(fillColor); graphics.fill(hull); }
            }
            if (config.monstersShamanOutline() && modelColor.getAlpha() > 0)
                outlines.drawOutline(npc, Math.max(1, Math.min(6, config.monstersShamanOutlineWidth())), outlineColor, 2);
            if (config.monstersShamanLabel() && modelColor.getAlpha() > 0)
            {
                Point text = npc.getCanvasTextLocation(graphics, "Spawn", npc.getLogicalHeight() + 20);
                if (text != null) OverlayUtil.renderTextLocation(graphics, text, "Spawn", modelColor);
            }
        }
        return null;
    }
}
