package lt.oneman.sync;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.*;

/** Only the native completion background; all native content draws above this layer. */
final class QuestWindowBackgroundOverlay extends Overlay {
    @Inject private QuestWindowTheme theme;
    private BufferedImage image;
    QuestWindowBackgroundOverlay(){
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.UNDER_WIDGETS);
    }
    @Override public Dimension render(Graphics2D graphics){
        Rectangle bounds=theme.backgroundBounds();
        if(bounds==null||bounds.width<100||bounds.height<100)return null;
        if(image==null||image.getWidth()!=bounds.width||image.getHeight()!=bounds.height)
            image=theme.background(bounds.width,bounds.height);
        graphics.drawImage(image,bounds.x,bounds.y,null);
        return null;
    }
}
