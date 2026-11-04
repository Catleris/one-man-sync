package lt.oneman.sync;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
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
    volatile boolean infoView=true;
    private volatile HitAreas hitAreas=HitAreas.EMPTY;
    private BufferedImage[] icons;
    TargetInfoOverlay(){setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_WIDGETS);}
    static final class HitAreas {
        static final HitAreas EMPTY=new HitAreas(new Rectangle(),new Rectangle(),new Rectangle(),new Rectangle(),new Rectangle(),false);
        final Rectangle panel, items, info, details, clear;final boolean page;
        HitAreas(Rectangle panel,Rectangle items,Rectangle info,Rectangle details,Rectangle clear,boolean page){
            this.panel=panel;this.items=items;this.info=info;this.details=details;this.clear=clear;this.page=page;
        }
    }
    void hideHitAreas(){hitAreas=HitAreas.EMPTY;}
    int hit(int x,int y){
        HitAreas h=hitAreas;
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
        // Avoid taking control of an item drag already started in the native inventory.
        if(client.getDraggedWidget()!=null||client.isMenuOpen())return null;
        Graphics2D g=(Graphics2D)graphics.create();
        try{
            Font font=FontManager.getRunescapeSmallFont();g.setFont(font);
            if(!infoView){
                // A small i in the lower rim, outside all item-slot click zones.
                Rectangle toggle=new Rectangle(bounds.x+bounds.width-16,bounds.y+bounds.height+1,15,13);
                if(toggle.y+toggle.height<=client.getCanvasHeight()){
                    button(g,toggle,"i",true);
                    hitAreas=new HitAreas(new Rectangle(),new Rectangle(),toggle,new Rectangle(),new Rectangle(),false);
                }
                return null;
            }
            g.clip(bounds);
            g.setColor(new Color(60,54,42));g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);
            g.setColor(new Color(129,111,69));g.drawRect(bounds.x,bounds.y,bounds.width-1,bounds.height-1);
            int x=bounds.x+6,w=bounds.width-12,y=bounds.y+5;
            Rectangle itemTab=new Rectangle(x,y,52,18), infoTab=new Rectangle(x+54,y,w-54,18);
            button(g,itemTab,"Items",false);button(g,infoTab,"Target info",true);
            TargetInfo.Snapshot current=snapshot;
            y+=34;g.setColor(GOLD);
            g.drawString(fit(current==null?"No target selected":current.name,g.getFontMetrics(),w),x,y);
            TargetInfoData.Stats s=current==null?null:current.stats;
            Rectangle details=new Rectangle(x,bounds.y+bounds.height-23,64,18);
            Rectangle clear=new Rectangle(bounds.x+bounds.width-48,bounds.y+bounds.height-23,42,18);
            if(current==null){
                g.setColor(MUTED);g.drawString("Attack an NPC to select it.",x,y+24);
                g.drawString("Its info stays after combat.",x,y+39);
                g.drawString("Use Items to see your bag.",x,y+64);
            }else if(s==null){
                g.setColor(MUTED);g.drawString("Reference stats unavailable.",x,y+24);
                g.drawString("This NPC variant is unknown.",x,y+39);
            }else{
                row(g,x,y+21,w,"Element",s.elementText(),GREEN);
                row(g,x,y+37,w,"Melee",s.meleeText(),GOLD);
                if(icons==null)icons=new BufferedImage[]{items.getImage(ItemID.BRONZE_DAGGER),items.getImage(ItemID.BRONZE_SCIMITAR),items.getImage(ItemID.BRONZE_WARHAMMER),items.getImage(ItemID.MINDRUNE)};
                Integer[] values={s.stab,s.slash,s.crush,s.magic};
                String[] labels={"Stab","Slash","Crush","Magic"};
                int cell=w/4;
                for(int i=0;i<4;i++){
                    int cx=x+i*cell;
                    g.setColor(new Color(83,74,54));g.fillRect(cx,y+46,cell-2,55);
                    if(icons[i]!=null)g.drawImage(icons[i],cx+(cell-24)/2,y+46,24,24,null);
                    centered(g,labels[i],cx,cell,y+81,MUTED);
                    centered(g,TargetInfoData.value(values[i]),cx,cell,y+96,Color.WHITE);
                }
                int bottom=details.y-6;
                if(config.monstersTargetExpanded()){
                    String[][] rows={{"Ranged L / S / H",TargetInfoData.value(s.light)+" / "+TargetInfoData.value(s.standard)+" / "+TargetInfoData.value(s.heavy)},
                        {"Def / Magic lvl",TargetInfoData.value(s.defence)+" / "+TargetInfoData.value(s.magicLevel)},
                        {"Base HP",TargetInfoData.value(s.hp)},
                        {"Base attack speed",s.speed==null||s.speed<=0?"?":s.speed+" ticks"}};
                    int lineY=y+118;
                    for(String[] r:rows){if(lineY<bottom){row(g,x,lineY,w,r[0],r[1],Color.WHITE);lineY+=14;}}
                }
                if(y+104<bottom&&!config.monstersTargetExpanded()){
                    g.setColor(MUTED);g.drawString("Defence bonuses, not DPS.",x,y+121);
                }
            }
            button(g,details,config.monstersTargetExpanded()?"Less":"Details",false);button(g,clear,"Clear",false);
            hitAreas=new HitAreas(new Rectangle(bounds),itemTab,infoTab,details,clear,true);
        }finally{g.dispose();}
        return null;
    }
    private static void row(Graphics2D g,int x,int y,int width,String label,String value,Color color){
        g.setColor(MUTED);g.drawString(fit(label,g.getFontMetrics(),Math.max(0,width-g.getFontMetrics().stringWidth(value)-8)),x,y);g.setColor(color);
        g.drawString(value,x+width-g.getFontMetrics().stringWidth(value),y);
    }
    private static void centered(Graphics2D g,String text,int x,int width,int y,Color color){g.setColor(color);g.drawString(text,x+(width-g.getFontMetrics().stringWidth(text))/2,y);}
    private static void button(Graphics2D g,Rectangle r,String text,boolean selected){
        g.setColor(selected?new Color(106,86,43):new Color(48,44,35));g.fillRect(r.x,r.y,r.width,r.height);
        g.setColor(selected?GOLD:new Color(139,122,78));g.drawRect(r.x,r.y,r.width-1,r.height-1);
        centered(g,text,r.x,r.width,r.y+r.height-5,selected?GOLD:MUTED);
    }
    static String fit(String name,FontMetrics metrics,int width){
        if(metrics.stringWidth(name)<=width)return name;
        while(!name.isEmpty()&&metrics.stringWidth(name+"…")>width)name=name.substring(0,name.length()-1);
        return name+"…";
    }
}
