package lt.oneman.sync;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Inventory-bound local information page. The native item grid is never moved or resized. */
@Singleton
final class TargetInfoOverlay extends Overlay
{
    static final int OUTSIDE=-1, BODY=0, ITEMS=1, INFO=2, DETAILS=3, CLEAR=4;
    private static final Color GOLD=new Color(237,194,90), MUTED=new Color(188,178,151), GREEN=new Color(153,215,154);
    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    @Inject private ItemManager items;
    volatile TargetInfo.Snapshot snapshot;
    volatile boolean infoView;
    private volatile HitAreas hitAreas=HitAreas.EMPTY;
    private BufferedImage[] icons;
    TargetInfoOverlay(){setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_WIDGETS);}
    static final class HitAreas {
        static final HitAreas EMPTY=new HitAreas(new Rectangle(),new Rectangle(),new Rectangle(),new Rectangle(),new Rectangle(),false,new Rectangle(),OUTSIDE);
        final Rectangle panel, items, info, details, clear, side;final boolean page;final int sideAction;
        HitAreas(Rectangle panel,Rectangle items,Rectangle info,Rectangle details,Rectangle clear,boolean page,Rectangle side,int sideAction){
            this.panel=panel;this.items=items;this.info=info;this.details=details;this.clear=clear;this.page=page;this.side=side;this.sideAction=sideAction;
        }
    }
    void hideHitAreas(){hitAreas=HitAreas.EMPTY;}
    int hit(int x,int y){
        HitAreas h=hitAreas;
        if(h.side.contains(x,y))return h.sideAction;
        if(h.items.contains(x,y))return ITEMS;
        if(h.info.contains(x,y))return INFO;
        if(h.details.contains(x,y))return DETAILS;
        if(h.clear.contains(x,y))return CLEAR;
        return h.page&&h.panel.contains(x,y)?BODY:OUTSIDE;
    }
    boolean blocksItems(int x,int y){HitAreas h=hitAreas;return h.page&&h.panel.contains(x,y);}
    @Override public Dimension render(Graphics2D graphics){
        hideHitAreas();
        if(!config.monstersTargetInfo()||client.getGameState()!=GameState.LOGGED_IN)return null;
        // The inventory's sole root has no named child component in gamevals.
        Widget inventory=client.getWidget(InterfaceID.INVENTORY,0);
        if(inventory==null||inventory.isHidden())return null;
        Rectangle bounds=inventory.getBounds();
        if(bounds==null||bounds.width<140||bounds.height<180)return null;
        Graphics2D g=(Graphics2D)graphics.create();
        try{
            Font body=FontManager.getRunescapeFont(),small=FontManager.getRunescapeSmallFont();
            g.setFont(body);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle side=sideButtonBounds(bounds,client.getCanvasWidth(),client.getCanvasHeight());
            if(!side.isEmpty())sideButton(g,side,infoView);
            // Keep the shield visible without intercepting a native drag or menu.
            if(client.getDraggedWidget()!=null||client.isMenuOpen())return null;
            if(!infoView){
                hitAreas=new HitAreas(new Rectangle(),new Rectangle(),new Rectangle(),new Rectangle(),new Rectangle(),false,side,INFO);
                return null;
            }
            g.clip(bounds);
            g.setPaint(new GradientPaint(bounds.x,bounds.y,new Color(35,45,38),bounds.x,bounds.y+bounds.height,new Color(21,29,25)));
            g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);
            g.setColor(new Color(145,121,67));g.drawRect(bounds.x,bounds.y,bounds.width-1,bounds.height-1);
            g.setColor(new Color(66,77,60));g.drawRect(bounds.x+2,bounds.y+2,bounds.width-5,bounds.height-5);
            int x=bounds.x+7,w=bounds.width-14;
            Rectangle itemTab=new Rectangle(x,bounds.y+6,w/2-2,21),infoTab=new Rectangle(x+w/2+2,bounds.y+6,w-w/2-2,21);
            button(g,itemTab,"Items",false);button(g,infoTab,"Target info",true);
            TargetInfo.Snapshot current=snapshot;
            g.setColor(new Color(44,55,44));g.fillRoundRect(x,bounds.y+33,w,33,6,6);
            g.setFont(small);g.setColor(MUTED);g.drawString("LAST TARGET",x+6,bounds.y+44);
            g.setFont(body);g.setColor(GOLD);
            g.drawString(fit(current==null?"No target selected":current.name,g.getFontMetrics(),w-12),x+6,bounds.y+59);
            TargetInfoData.Stats s=current==null?null:current.stats;
            Rectangle details=new Rectangle(x,bounds.y+bounds.height-28,w/2-3,22);
            Rectangle clear=new Rectangle(x+w/2+3,bounds.y+bounds.height-28,w-w/2-3,22);
            if(current==null||s==null){
                g.setColor(MUTED);
                g.drawString(current==null?"Attack an NPC to select it.":"Reference stats unavailable.",x,bounds.y+91);
                g.setFont(small);
                g.drawString(current==null?"Its info stays after combat.":"This NPC variant is unknown.",x,bounds.y+108);
                g.drawString("Use Items to see your bag.",x,bounds.y+137);
            }else{
                g.setColor(new Color(39,55,43));g.fillRoundRect(x,bounds.y+72,w,22,5,5);
                row(g,x+5,bounds.y+87,w-10,"Element",s.elementText(),GREEN);
                g.setFont(small);row(g,x+3,bounds.y+103,w-6,"Lowest melee",s.meleeText(),GOLD);
                if(icons==null)icons=new BufferedImage[]{items.getImage(ItemID.BRONZE_DAGGER),items.getImage(ItemID.BRONZE_SCIMITAR),items.getImage(ItemID.BRONZE_WARHAMMER),items.getImage(ItemID.MINDRUNE)};
                Integer[] values={s.stab,s.slash,s.crush,s.magic};
                String[] labels={"Stab","Slash","Crush","Magic"};
                int cell=w/4;
                for(int i=0;i<4;i++){
                    int cx=x+i*cell,cy=bounds.y+110;
                    g.setPaint(new GradientPaint(cx,cy,new Color(61,67,48),cx,cy+52,new Color(37,45,35)));
                    g.fillRoundRect(cx,cy,cell-3,52,5,5);
                    g.setColor(new Color(94,99,65));g.drawRoundRect(cx,cy,cell-4,51,5,5);
                    if(icons[i]!=null)g.drawImage(icons[i],cx+(cell-3-23)/2,cy+2,23,23,null);
                    g.setFont(small);centered(g,labels[i],cx,cell-3,cy+35,MUTED);
                    g.setFont(body);centered(g,TargetInfoData.value(values[i]),cx,cell-3,cy+48,Color.WHITE);
                }
                int bottom=details.y-7;
                g.setFont(small);
                if(config.monstersTargetExpanded()){
                    String[][] rows={{"Ranged L / S / H",TargetInfoData.value(s.light)+" / "+TargetInfoData.value(s.standard)+" / "+TargetInfoData.value(s.heavy)},
                        {"Def / Magic lvl",TargetInfoData.value(s.defence)+" / "+TargetInfoData.value(s.magicLevel)},
                        {"Base HP",TargetInfoData.value(s.hp)},
                        {"Attack speed",s.speed==null||s.speed<=0?"?":s.speed+" ticks"}};
                    int lineY=bounds.y+178;
                    for(int i=0;i<rows.length;i++)if(lineY<bottom){
                        if(i%2==0){g.setColor(new Color(35,44,36));g.fillRect(x,lineY-10,w,14);}
                        row(g,x+3,lineY,w-6,rows[i][0],rows[i][1],Color.WHITE);lineY+=14;
                    }
                }else{
                    g.setColor(MUTED);g.drawString("Reference defence bonuses",x+3,bounds.y+183);
                    g.drawString("Open Details for base stats.",x+3,bounds.y+198);
                }
            }
            g.setFont(body);
            button(g,details,config.monstersTargetExpanded()?"Less":"Details",false);button(g,clear,"Clear",false);
            hitAreas=new HitAreas(new Rectangle(bounds),itemTab,infoTab,details,clear,true,side,ITEMS);
        }finally{g.dispose();}
        return null;
    }
    private static void row(Graphics2D g,int x,int y,int width,String label,String value,Color color){
        g.setColor(MUTED);g.drawString(fit(label,g.getFontMetrics(),Math.max(0,width-g.getFontMetrics().stringWidth(value)-8)),x,y);g.setColor(color);
        g.drawString(value,x+width-g.getFontMetrics().stringWidth(value),y);
    }
    private static void centered(Graphics2D g,String text,int x,int width,int y,Color color){g.setColor(color);g.drawString(text,x+(width-g.getFontMetrics().stringWidth(text))/2,y);}
    /** Side dock leaves both the item rectangle and native top/bottom tab rows untouched. */
    static Rectangle sideButtonBounds(Rectangle inventory,int canvasWidth,int canvasHeight){
        int width=32,height=36,gap=14;
        int x=inventory.x-gap-width;
        if(x<2)x=inventory.x+inventory.width+gap;
        int y=Math.max(2,Math.min(inventory.y+(inventory.height-height)/2,canvasHeight-height-2));
        if(x<2||x+width>canvasWidth-2||y+height>canvasHeight-2)return new Rectangle();
        return new Rectangle(x,y,width,height);
    }
    private static void sideButton(Graphics2D g, Rectangle bounds, boolean showingInfo)
    {
        Graphics2D icon = (Graphics2D) g.create();
        try
        {
            icon.translate(bounds.x, bounds.y);
            int[] shieldX = {5, 26, 30, 30, 27, 22, 16, 10, 5, 2, 2};
            int[] shieldY = {2, 2, 6, 22, 27, 31, 34, 31, 27, 22, 6};
            icon.setColor(new Color(19, 20, 16));
            icon.fillPolygon(shieldX, shieldY, shieldX.length);
            int[] rimX = {6, 25, 29, 29, 26, 21, 16, 11, 6, 3, 3};
            int[] rimY = {3, 3, 7, 22, 26, 30, 33, 30, 26, 22, 7};
            icon.setPaint(new GradientPaint(0, 3, new Color(245, 204, 107),
                0, 33, new Color(140, 100, 39)));
            icon.fillPolygon(rimX, rimY, rimX.length);
            int[] insetX = {7, 24, 27, 27, 24, 20, 16, 12, 8, 5, 5};
            int[] insetY = {6, 6, 9, 21, 25, 28, 30, 28, 25, 21, 9};
            icon.setPaint(new GradientPaint(0, 6, showingInfo ? new Color(65, 71, 43) : new Color(37, 70, 62),
                0, 30, new Color(18, 36, 32)));
            icon.fillPolygon(insetX, insetY, insetX.length);

            if (showingInfo)
            {
                icon.setColor(GOLD);
                icon.drawRoundRect(10, 13, 12, 13, 4, 4);
                icon.drawRoundRect(13, 10, 6, 4, 3, 3);
                icon.drawRoundRect(12, 19, 8, 5, 3, 3);
                return;
            }

            int[] eyeX = {6, 10, 14, 18, 22, 26, 22, 18, 14, 10};
            int[] eyeY = {17, 13, 11, 11, 13, 17, 21, 23, 23, 21};
            icon.setColor(GOLD);
            icon.fillPolygon(eyeX, eyeY, eyeX.length);
            int[] lidX = {8, 12, 15, 17, 20, 24, 20, 17, 15, 12};
            int[] lidY = {17, 14, 13, 13, 14, 17, 20, 21, 21, 20};
            icon.setColor(new Color(24, 28, 21));
            icon.fillPolygon(lidX, lidY, lidX.length);
            icon.setColor(new Color(153, 210, 119));
            icon.fillOval(12, 13, 9, 9);
            icon.setColor(new Color(20, 35, 23));
            icon.fillRect(16, 13, 2, 9);
            icon.setColor(new Color(213, 244, 169));
            icon.fillRect(13, 15, 2, 2);
        }
        finally
        {
            icon.dispose();
        }
    }

    private static void button(Graphics2D g,Rectangle r,String text,boolean selected){
        g.setPaint(new GradientPaint(r.x,r.y,selected?new Color(115,94,49):new Color(52,64,49),r.x,r.y+r.height,selected?new Color(72,65,36):new Color(30,40,31)));
        g.fillRoundRect(r.x,r.y,r.width,r.height,5,5);
        g.setColor(selected?GOLD:new Color(110,119,78));g.drawRoundRect(r.x,r.y,r.width-1,r.height-1,5,5);
        centered(g,text,r.x,r.width,r.y+r.height-6,selected?GOLD:MUTED);
    }
    static String fit(String name,FontMetrics metrics,int width){
        if(metrics.stringWidth(name)<=width)return name;
        while(!name.isEmpty()&&metrics.stringWidth(name+"…")>width)name=name.substring(0,name.length()-1);
        return name+"…";
    }
}
