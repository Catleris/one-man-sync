package lt.oneman.sync;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** Small original helmet mark, drawn at toolbar size without tiny lettering. */
final class OneManIcon
{
    private OneManIcon(){}
    static BufferedImage toolbar(){
        BufferedImage image=new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(3,2,new Color(51,73,55),21,23,new Color(21,34,26)));g.fillRoundRect(1,1,22,22,7,7);
        g.setColor(new Color(111,92,47));g.drawRoundRect(1,1,21,21,7,7);
        g.setPaint(new GradientPaint(5,4,new Color(255,220,126),18,21,new Color(185,130,41)));
        Path2D helmet=new Path2D.Double();helmet.moveTo(6,10);helmet.lineTo(3,5);helmet.lineTo(7,7);helmet.lineTo(9,5);
        helmet.quadTo(12,3,15,5);helmet.lineTo(17,7);helmet.lineTo(21,5);helmet.lineTo(18,10);
        helmet.lineTo(18,17);helmet.lineTo(13,21);helmet.lineTo(11,21);helmet.lineTo(6,17);helmet.closePath();g.fill(helmet);
        g.setColor(new Color(26,37,29));g.fillRoundRect(8,10,8,3,2,2);g.fillRect(11,12,2,7);
        g.setColor(new Color(247,218,141));g.fillRect(11,6,2,5);g.dispose();return image;
    }
}
