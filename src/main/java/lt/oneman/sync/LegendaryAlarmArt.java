package lt.oneman.sync;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Bundled artwork derived from the owner's selected Legendary reference. No runtime downloads. */
final class LegendaryAlarmArt
{
    static final int WIDTH=520, HEIGHT=190;
    private final BufferedImage collection,combat;
    LegendaryAlarmArt() {
        try(InputStream stream=getClass().getResourceAsStream("legendary-alarms.png")) {
            if(stream==null)throw new IllegalStateException("Missing Legendary alarm artwork");
            BufferedImage sheet=ImageIO.read(stream);
            if(sheet==null||sheet.getWidth()!=1536||sheet.getHeight()!=1024)throw new IllegalStateException("Invalid Legendary alarm artwork");
            collection=frame(sheet,4,512);combat=frame(sheet,520,504);
        } catch(IOException error){throw new IllegalStateException("Cannot load Legendary alarm artwork",error);}
    }
    private static BufferedImage frame(BufferedImage sheet,int y,int height) {
        BufferedImage result=new BufferedImage(WIDTH*2,HEIGHT*2,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=result.createGraphics();
        try {g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(sheet,0,0,WIDTH*2,HEIGHT*2,70,y,1466,y+height,null);
        }finally{g.dispose();}
        return result;
    }
    void draw(Graphics2D graphics,AchievementAlarmQueue.Notice notice,BufferedImage item) {
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(notice.combat?combat:collection,0,0,WIDTH,HEIGHT,null);
            String heading=notice.kind==AchievementAlarmQueue.Kind.QUEST?"QUEST COMPLETED":notice.kind==AchievementAlarmQueue.Kind.DIARY?"ACHIEVEMENT DIARY":notice.kind==AchievementAlarmQueue.Kind.COMBAT_TIER?"COMBAT TIER UNLOCKED":notice.combat?"COMBAT ACHIEVEMENT":"COLLECTION LOG";
            text(g,heading,260,57,notice.combat?18:20,new Color(245,204,112),390);
            text(g,notice.name,notice.combat?344:335,notice.combat?95:98,20,new Color(249,238,211),notice.combat?190:200);
            if(notice.combat) {
                String[] tiers={"EASY","MEDIUM","HARD","ELITE","MASTER","GRANDMASTER"};
                int tier=Math.max(0,Math.min(tiers.length-1,notice.tier));
                text(g,tiers[tier],344,120,15,new Color(249,211,131),94);
                String detail=notice.kind==AchievementAlarmQueue.Kind.COMBAT_TASK?"+"+(tier+1)+" points":notice.detail;
                text(g,detail,344,148,notice.kind==AchievementAlarmQueue.Kind.COMBAT_TASK?14:11,new Color(239,223,188),190);
            } else {
                if(item!=null) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    int width=61,height=Math.max(1,item.getHeight()*width/Math.max(1,item.getWidth()));
                    if(height>65){height=65;width=item.getWidth()*height/Math.max(1,item.getHeight());}
                    g.drawImage(item,167-width/2,119-height/2,width,height,null);
                } else text(g,"✦",167,128,29,new Color(245,204,112),65);
                text(g,notice.kind==AchievementAlarmQueue.Kind.QUEST?"COMPLETED":"NEW ITEM",335,132,16,new Color(249,211,131),100);
                if(notice.preview)text(g,"PREVIEW",335,154,11,new Color(197,182,152),180);
            }
            text(g,notice.preview?"OneMan Sync • Preview":"OneMan Sync",260,notice.combat?174:181,11,new Color(225,210,178),106);
        }finally{g.dispose();}
    }
    private static void text(Graphics2D g,String text,int cx,int baseline,int size,Color color,int maxWidth) {
        Font font=new Font(Font.SERIF,Font.BOLD,size);g.setFont(font);
        while(g.getFontMetrics().stringWidth(text)>maxWidth&&font.getSize()>10){font=font.deriveFont((float)font.getSize()-1);g.setFont(font);}
        String value=text;
        while(g.getFontMetrics().stringWidth(value)>maxWidth&&value.length()>2)value=value.substring(0,value.length()-2)+"…";
        int x=cx-g.getFontMetrics().stringWidth(value)/2;
        g.setColor(new Color(0,0,0,220));g.drawString(value,x+1,baseline+1);g.setColor(color);g.drawString(value,x,baseline);
    }
}
