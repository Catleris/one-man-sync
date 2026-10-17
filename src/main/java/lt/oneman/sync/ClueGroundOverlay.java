package lt.oneman.sync;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class ClueGroundOverlay extends Overlay
{
    @Inject private Client client;
    // Connected after injection completes; no Guice back-reference to the constructing hub.
    private volatile ClueHub hub;
    void setHub(ClueHub hub) { this.hub=hub; }
    @Inject private OneManSyncConfig config;
    ClueGroundOverlay() { setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ABOVE_SCENE); }
    @Override public Dimension render(Graphics2D graphics)
    {
        ClueHub hub=this.hub;
        if (hub==null || !hub.enabled() || !config.clueGroundMarkers() || client.getGameState() != GameState.LOGGED_IN) return null;
        Map<Tile, Integer> rows = new HashMap<>();
        Map<Tile, Set<String>> names = new HashMap<>();
        FontMetrics metrics = graphics.getFontMetrics();
        for (ClueHub.GroundItem item : hub.groundItems())
        {
            Tile tile = item.tile;
            if (tile.getPlane() != client.getPlane() || tile.getItemLayer() == null) continue;
            if (!names.computeIfAbsent(tile, k -> new java.util.HashSet<>()).add(item.name)) continue;
            Set<ClueTier> tiers = hub.tiers(item.tags); if (tiers.isEmpty()) continue;
            String label = item.name + " [" + hub.kind(item.tags) + "]";
            int markerWidth = 0;
            for (ClueTier tier : tiers) markerWidth += 12 + metrics.stringWidth(tier.shortName);
            Point point = Perspective.getCanvasTextLocation(client, graphics, tile.getLocalLocation(), label,
                tile.getItemLayer().getHeight() + config.clueGroundLabelHeight());
            if (point == null) continue;
            int row = rows.getOrDefault(tile, 0); rows.put(tile, row + 1);
            int x = point.getX() - markerWidth / 2, y = point.getY() - row * (metrics.getHeight() + 2);
            graphics.setColor(Color.BLACK); graphics.drawString(label, x + 1, y + 1);
            graphics.setColor(new Color(235,235,220)); graphics.drawString(label, x, y);
            int dotX = x + metrics.stringWidth(label) + 5;
            for (ClueTier tier : tiers)
            {
                graphics.setColor(Color.BLACK); graphics.fillOval(dotX - 1, y - 8, 8, 8);
                graphics.setColor(tier.color); graphics.fillOval(dotX, y - 7, 6, 6);
                graphics.drawString(tier.shortName, dotX + 8, y); dotX += 12 + metrics.stringWidth(tier.shortName);
            }
        }
        return null;
    }
}
