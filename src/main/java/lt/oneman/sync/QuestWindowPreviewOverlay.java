package lt.oneman.sync;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.*;
/** Explicit local preview only. Does not open, hide or fabricate a server-owned interface. */
final class QuestWindowPreviewOverlay extends Overlay {
    @Inject private Client client;
    @Inject private QuestWindowTheme theme;
    private BufferedImage background;
    QuestWindowPreviewOverlay(){setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_WIDGETS);setPriority(OverlayPriority.HIGH);}
    @Override public Dimension render(Graphics2D graphics){
        if(!theme.previewVisible())return null;
        if(background==null)background=theme.previewBackground();
        double scale=Math.min(1,Math.min((client.getViewportWidth()-16)/488.0,(client.getViewportHeight()-16)/334.0));
        if(scale<=0)return null;
        Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.translate(client.getViewportXOffset()+(client.getViewportWidth()-488*scale)/2,client.getViewportYOffset()+(client.getViewportHeight()-334*scale)/2);
            g.scale(scale,scale);drawPreview(g,background);
        }finally{g.dispose();}
        return null;
    }
    static void drawPreview(Graphics2D g,BufferedImage background){
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.drawImage(background,0,0,null);
        text(g,"Quest window preview",244,65,19,QuestWindowTheme.GOLD);
        text(g,"Original quest title",244,98,17,QuestWindowTheme.CREAM);
        text(g,"The real game supplies:",244,143,14,QuestWindowTheme.CREAM);
        text(g,"Quest rewards and XP",244,174,14,QuestWindowTheme.CREAM);
        text(g,"Quest Points • Reward model • Ribbon",244,202,13,QuestWindowTheme.CREAM);
        text(g,"Original close button and controls",244,230,13,QuestWindowTheme.CREAM);
        text(g,"PREVIEW ONLY • No quest completed",244,284,12,QuestWindowTheme.GOLD);
    }
    private static void text(Graphics2D g,String text,int x,int y,int size,int rgb){g.setFont(new Font(Font.SERIF,Font.BOLD,size));g.setColor(new Color(rgb));g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);}
}
