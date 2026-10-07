package lt.oneman.sync;

import java.awt.*;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

final class SlayerLabMapPoint extends WorldMapPoint
{
    private static final BufferedImage NORMAL = icon(new Color(50, 130, 200), false);
    private static final BufferedImage SELECTED = icon(new Color(80, 205, 255), true);
    private static final BufferedImage PULSE = icon(new Color(170, 235, 255), true);
    private static final BufferedImage WILD = icon(new Color(240, 150, 60), false);
    private final SlayerLabLocations.Destination destination;
    SlayerLabMapPoint(SlayerLabLocations.Destination destination)
    {
        super(destination.entrance, destination.wilderness ? WILD : NORMAL);
        this.destination = destination;
        setName("OneMan: " + destination.name);
        setTooltip(destination.toString() + " — entrance\n" + destination.access);
        setJumpOnClick(true);
    }
    void highlight(boolean selected, boolean bright)
    {
        setImage(selected ? (bright ? PULSE : SELECTED) : (destination.wilderness ? WILD : NORMAL));
        setSnapToEdge(selected);
    }
    private static BufferedImage icon(Color color, boolean selected)
    {
        BufferedImage image = new BufferedImage(24, 28, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(10, 25, 45, 230));
        g.fillOval(2, 1, 20, 20);
        g.setColor(color);
        g.setStroke(new BasicStroke(selected ? 3f : 2f));
        g.drawOval(3, 2, 18, 18);
        g.fillPolygon(new int[]{7, 17, 12}, new int[]{18, 18, 27}, 3);
        g.fillOval(8, 7, 8, 8);
        g.dispose();
        return image;
    }
}
