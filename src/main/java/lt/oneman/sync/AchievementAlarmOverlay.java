package lt.oneman.sync;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.*;

/** Decorative, click-through notification. No gameplay input or world highlighting. */
final class AchievementAlarmOverlay extends Overlay
{
    @Inject private Client client;
    @Inject private AchievementAlarms alarms;
    @Inject private OneManSyncConfig config;
    AchievementAlarmOverlay(){setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_WIDGETS);setPriority(OverlayPriority.HIGH);}
    @Override public Dimension render(Graphics2D graphics) {
        AchievementAlarmQueue.Notice notice=alarms.displayed();
        if(notice==null||client.getGameState()!=GameState.LOGGED_IN)return null;
        if(!alarms.enabled(notice))return null;
        int available=client.getViewportWidth();if(available<200)return null;
        double scale=Math.min(config.alarmScale()/100.0,(available-16)/520.0);
        int x=client.getViewportXOffset()+(int)((available-520*scale)/2);
        int y=client.getViewportYOffset()+18;
        Graphics2D g=(Graphics2D)graphics.create();
        try {g.translate(x,y);g.scale(scale,scale);draw(g,notice,alarms.icon());}finally{g.dispose();}
        return null;
    }
    private final LegendaryAlarmArt art=new LegendaryAlarmArt();
    private void draw(Graphics2D g,AchievementAlarmQueue.Notice notice,BufferedImage icon){art.draw(g,notice,icon);}
}
