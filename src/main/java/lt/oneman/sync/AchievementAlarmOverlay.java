package lt.oneman.sync;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.*;

/** Decorative, click-through notification. No gameplay input or world highlighting. */
final class AchievementAlarmOverlay extends Overlay
{
    private static final Color GOLD=new Color(218,176,85), CREAM=new Color(247,232,193);
    @Inject private Client client;
    @Inject private AchievementAlarms alarms;
    @Inject private OneManSyncConfig config;
    AchievementAlarmOverlay(){setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_WIDGETS);setPriority(OverlayPriority.HIGH);}
    @Override public Dimension render(Graphics2D graphics) {
        AchievementAlarmQueue.Notice notice=alarms.displayed();
        if(notice==null||client.getGameState()!=GameState.LOGGED_IN)return null;
        if(notice.combat?!config.alarmCombat():!config.alarmCollection())return null;
        int available=client.getViewportWidth();if(available<200)return null;
        double scale=Math.min(config.alarmScale()/100.0,(available-16)/520.0);
        int x=client.getViewportX()+(int)((available-520*scale)/2);
        int y=client.getViewportY()+18;
        Graphics2D g=(Graphics2D)graphics.create();
        try {g.translate(x,y);g.scale(scale,scale);draw(g,notice,alarms.icon());}finally{g.dispose();}
        return null;
    }
    static void draw(Graphics2D g,AchievementAlarmQueue.Notice n,BufferedImage icon) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Color accent=n.combat&&n.tier>=0?CombatAchievementCatalog.Tier.values()[n.tier].color:GOLD;
        g.setColor(new Color(0,0,0,100));g.fillRoundRect(24,20,472,160,18,18);
        Path2D body=new Path2D.Double();body.moveTo(42,14);body.lineTo(478,14);body.lineTo(503,38);body.lineTo(492,142);body.lineTo(463,160);body.lineTo(57,160);body.lineTo(28,142);body.lineTo(17,38);body.closePath();
        g.setPaint(new GradientPaint(0,16,new Color(38,34,25,248),0,160,new Color(12,14,14,248)));g.fill(body);
        g.setStroke(new BasicStroke(3));g.setColor(GOLD);g.draw(body);
        g.setStroke(new BasicStroke(1));g.setColor(new Color(108,78,33));g.drawRoundRect(48,22,424,129,12,12);
        // Symmetrical layered gold wings, painted once per displayed frame without asset downloads.
        for(int side=0;side<2;side++) {
            Graphics2D wing=(Graphics2D)g.create();
            if(side==1){wing.translate(520,0);wing.scale(-1,1);}
            for(int i=0;i<4;i++) {
                int yy=21+i*21;
                Path2D feather=new Path2D.Double();feather.moveTo(43,yy+12);feather.lineTo(2+i*5,yy-14);feather.lineTo(12+i*5,yy+12);feather.lineTo(45,yy+35);feather.closePath();
                wing.setPaint(new GradientPaint(5,yy,GOLD,44,yy+28,new Color(77,53,22)));wing.fill(feather);wing.setColor(new Color(241,205,120));wing.draw(feather);
            }wing.dispose();
        }
        diamond(g,260,13,12,GOLD);diamond(g,260,13,6,CREAM);
        g.setFont(new Font(Font.SERIF,Font.BOLD,18));center(g,n.combat?"COMBAT ACHIEVEMENT":"COLLECTION LOG",260,46,GOLD);
        g.setColor(new Color(125,92,41));g.drawLine(161,56,457,56);
        Path2D shield=new Path2D.Double();shield.moveTo(67,58);shield.lineTo(132,58);shield.lineTo(129,118);shield.lineTo(100,143);shield.lineTo(70,118);shield.closePath();
        g.setPaint(new GradientPaint(70,58,new Color(69,54,29),129,142,new Color(19,20,18)));g.fill(shield);g.setColor(GOLD);g.setStroke(new BasicStroke(2));g.draw(shield);
        if(!n.combat&&icon!=null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(icon,75,72,50,44,null);
        } else if(n.combat) {
            g.setStroke(new BasicStroke(5));g.setColor(CREAM);g.drawLine(82,78,118,116);g.drawLine(118,78,82,116);
            g.setStroke(new BasicStroke(3));g.setColor(GOLD);g.drawLine(80,108,91,119);g.drawLine(109,119,120,108);
        } else {
            g.setColor(CREAM);g.setFont(new Font(Font.SERIF,Font.BOLD,35));center(g,"✦",100,109,CREAM);
        }
        Font title=new Font(Font.SERIF,Font.BOLD,22);g.setFont(title);
        while(g.getFontMetrics().stringWidth(n.name)>290&&title.getSize()>12){title=title.deriveFont((float)title.getSize()-1);g.setFont(title);}
        String name=n.name;while(g.getFontMetrics().stringWidth(name)>290&&name.length()>1)name=name.substring(0,name.length()-2)+"…";
        center(g,name,307,90,CREAM);
        g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));center(g,n.detail,307,119,accent);
        diamond(g,189,116,3,GOLD);diamond(g,425,116,3,GOLD);
        g.setFont(new Font(Font.SERIF,Font.PLAIN,12));center(g,n.preview?"PREVIEW  •  OneMan Sync":"OneMan Sync",307,143,new Color(185,165,126));
        g.setColor(new Color(31,28,22));g.fillRoundRect(195,153,130,20,6,6);g.setColor(GOLD);g.drawRoundRect(195,153,130,20,6,6);
        g.setFont(new Font(Font.SERIF,Font.PLAIN,11));center(g,"ONEMAN",260,167,GOLD);
    }
    private static void center(Graphics2D g,String text,int x,int y,Color color){g.setColor(color);g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);}
    private static void diamond(Graphics2D g,int x,int y,int r,Color color){g.setColor(color);g.fillPolygon(new int[]{x,x+r,x,x-r},new int[]{y-r,y,y+r,y},4);}
}
