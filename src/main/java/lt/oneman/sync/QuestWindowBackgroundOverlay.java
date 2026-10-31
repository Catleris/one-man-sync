package lt.oneman.sync;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.ui.overlay.*;

/** Draws exactly in the background layer's native order, before reward siblings. */
final class QuestWindowBackgroundOverlay extends Overlay {
    @Inject private QuestWindowTheme theme;
    private BufferedImage image;
    QuestWindowBackgroundOverlay(){
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.MANUAL);
        drawAfterLayer(InterfaceID.Questscroll.SCROLL);
    }
    @Override public Dimension render(Graphics2D graphics){
        Rectangle bounds=theme.backgroundBounds();
        if(bounds==null||bounds.width<1||bounds.height<1)return null;
        if(image==null||image.getWidth()!=bounds.width||image.getHeight()!=bounds.height)
            image=theme.background(bounds.width,bounds.height);
        graphics.drawImage(image,bounds.x,bounds.y,null);
        theme.backgroundDrawn();
        return null;
    }
}
