package lt.oneman.sync;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.ui.overlay.OverlayManager;
import java.awt.Rectangle;

/** A cosmetic skin of the native completion scroll, never a replacement for its content/actions. */
@Singleton
final class QuestWindowTheme {
    static final int GOLD=0xe8be69, CREAM=0xf6ead2;
    private static final int[] TEXT={InterfaceID.Questscroll.CONGRATULATIONS,InterfaceID.Questscroll.QUEST_TITLE,
        InterfaceID.Questscroll.QUEST_POINTS,InterfaceID.Questscroll.AWARD_TEXT,
        InterfaceID.Questscroll.QUEST_REWARD1,InterfaceID.Questscroll.QUEST_REWARD2,InterfaceID.Questscroll.QUEST_REWARD3,
        InterfaceID.Questscroll.QUEST_REWARD4,InterfaceID.Questscroll.QUEST_REWARD5,InterfaceID.Questscroll.QUEST_REWARD6,InterfaceID.Questscroll.QUEST_REWARD7};
    private final Client client;
    private final OneManSyncConfig config;
    private final OverlayManager overlays;
    private final QuestWindowArt art=new QuestWindowArt();
    @Inject private QuestWindowPreviewOverlay previewOverlay;

    private final List<TextStyle> textStyles=new ArrayList<>();
    private Widget scroll;
    private int previousType,previousSprite,previousOpacity,previousContentType;
    private Widget[] previousChildren;
    private Widget backgroundChild;
    private boolean previousTiling;
    private SpritePixels replacement,previousOverride;
    private boolean active;
    private int retry;
    private long previewUntil;
    private volatile String status="Quest: starting";
    String status(){return status;}
    private void updateStatus(){
        Widget w=client.getWidget(InterfaceID.Questscroll.SCROLL);
        Widget c=client.getWidget(InterfaceID.Questscroll.CONTENT);
        status="Quest: "+(!active?"inactive":!config.alarmQuests()?"OFF":scroll!=null?"APPLIED":w==null?"waiting for window":
            "found type="+w.getType()+" size="+w.getWidth()+"x"+w.getHeight()+" hidden="+w.isHidden()+" content="+(c==null?"missing":c.isHidden()?"hidden":"visible"));
    }
    @Inject QuestWindowTheme(Client client,OneManSyncConfig config,OverlayManager overlays){this.client=client;this.config=config;this.overlays=overlays;}
    void startUp(){active=true;retry=10;overlays.add(previewOverlay);}
    void shutDown(){active=false;previewUntil=0;retry=0;restore();overlays.remove(previewOverlay);}
    void opened(){if(active&&config.alarmQuests()){previewUntil=0;retry=10;apply();}}
    void closed(){previewUntil=0;retry=0;restore();}
    void gameStateChanged(GameState state){
        if(state!=GameState.LOGGED_IN)closed();else if(active&&config.alarmQuests())retry=10;
    }
    void configChanged(){
        if(!config.alarmQuests()){closed();return;}
        retry=10;if(active&&client.getGameState()==GameState.LOGGED_IN)apply();
    }
    void tick(){
        updateStatus();
        if(!active||!config.alarmQuests()){closed();return;}
        if(scroll!=null){
            if(client.getWidget(InterfaceID.Questscroll.SCROLL)==scroll&&contentVisible()){for(TextStyle style:textStyles)style.apply();return;}
            restore();
        }
        apply(); // A single widget lookup also catches late layout and an already-open window.
    }
    boolean apply(){
        if(!active||!config.alarmQuests()||client.getGameState()!=GameState.LOGGED_IN)return false;
        Widget widget=client.getWidget(InterfaceID.Questscroll.SCROLL);
        if(widget==null||!contentVisible()||(widget!=scroll&&widget.isHidden())||widget.getWidth()<1||widget.getHeight()<1)return false;
        if(widget==scroll)return true;
        restore();
        SpritePixels sprite=ImageUtil.getImageSpritePixels(art.background(widget.getWidth(),widget.getHeight()),client);
        if(sprite==null)return false;
        Widget[] children=widget.getChildren();
        Widget child=widget.createChild(-1,WidgetType.GRAPHIC);
        if(child==null)return false;
        scroll=widget;
        previousChildren=children;backgroundChild=child;previousContentType=widget.getContentType();
        previousType=widget.getType();previousSprite=widget.getSpriteId();
        previousOpacity=widget.getOpacity();previousTiling=widget.getSpriteTiling();
        previousOverride=client.getWidgetSpriteOverrides().get(InterfaceID.Questscroll.SCROLL);
        replacement=sprite;
        client.getWidgetSpriteOverrides().put(InterfaceID.Questscroll.SCROLL,sprite);
        // Legacy IF1 graphics do not use the same sprite path as dynamic IF3 children.
        // Keep the decoration's bounds, but let its dynamic graphic child draw the skin.
        child.setOriginalX(0);child.setOriginalY(0);
        child.setOriginalWidth(widget.getWidth());child.setOriginalHeight(widget.getHeight());
        child.setSpriteId(net.runelite.api.SpriteID.TAB_QUESTS);
        child.setSpriteTiling(false);child.setOpacity(0);
        widget.setContentType(0);widget.setType(WidgetType.LAYER);
        child.revalidate();
        for(int id:TEXT){
            Widget text=client.getWidget(id);if(text==null)continue;
            int color=(id==InterfaceID.Questscroll.CONGRATULATIONS||id==InterfaceID.Questscroll.QUEST_POINTS)?GOLD:CREAM;
            TextStyle style=new TextStyle(text,color);textStyles.add(style);style.apply();
        }
        previewUntil=0;retry=0;updateStatus();return true;
    }
    private void restore(){
        for(TextStyle style:textStyles)style.restore();textStyles.clear();
        if(scroll!=null){
            Widget[] current=scroll.getChildren();
            // Only remove our own child; do not discard children another plugin added.
            if(current!=null&&backgroundChild!=null){
                boolean onlyOwn=true;
                int originalLength=previousChildren==null?0:previousChildren.length;
                for(int i=originalLength;i<current.length;i++)
                    if(current[i]!=null&&current[i]!=backgroundChild)onlyOwn=false;
                if(onlyOwn)scroll.setChildren(previousChildren);
                else for(int i=0;i<current.length;i++)if(current[i]==backgroundChild)current[i]=null;
            }
            scroll.setContentType(previousContentType);
            scroll.setType(previousType);scroll.setSpriteId(previousSprite);
            scroll.setSpriteTiling(previousTiling);scroll.setOpacity(previousOpacity);
        }
        if(replacement!=null&&client.getWidgetSpriteOverrides().get(InterfaceID.Questscroll.SCROLL)==replacement){
            if(previousOverride==null)client.getWidgetSpriteOverrides().remove(InterfaceID.Questscroll.SCROLL);
            else client.getWidgetSpriteOverrides().put(InterfaceID.Questscroll.SCROLL,previousOverride);
        }
        replacement=null;previousOverride=null;backgroundChild=null;previousChildren=null;
        scroll=null;

    }
    private boolean contentVisible(){
        Widget widget=client.getWidget(InterfaceID.Questscroll.SCROLL);
        if(widget==null)return false;
        // The visible background itself is authoritative. CONTENT may be an unused,
        // hidden sibling on the live interface, so it must not gate completion styling.
        if(widget!=scroll)return !widget.isHidden();
        Widget parent=widget.getParent();
        return parent==null||!parent.isHidden();
    }
    Rectangle backgroundBounds(){
        if(!active||!config.alarmQuests()||client.getGameState()!=GameState.LOGGED_IN||scroll==null||!contentVisible())return null;
        return scroll.getBounds();
    }
    BufferedImage background(int width,int height){return art.background(width,height);}
    void preview(){
        if(!active||!config.alarmQuests()||client.getGameState()!=GameState.LOGGED_IN)return;
        if(contentVisible()&&client.getWidget(InterfaceID.Questscroll.SCROLL)!=null){opened();return;}
        previewUntil=System.currentTimeMillis()+config.alarmSeconds()*1000L;
    }
    boolean previewVisible(){return active&&config.alarmQuests()&&client.getGameState()==GameState.LOGGED_IN&&System.currentTimeMillis()<previewUntil;}
    BufferedImage previewBackground(){return art.background(488,334);}
    private static final class TextStyle {
        final Widget widget;final int before,color;final boolean shadow;
        TextStyle(Widget widget,int color){this.widget=widget;this.color=color;before=widget.getTextColor();shadow=widget.getTextShadowed();}
        void apply(){widget.setTextColor(color);widget.setTextShadowed(true);}
        void restore(){if(widget.getTextColor()==color)widget.setTextColor(before);if(widget.getTextShadowed())widget.setTextShadowed(shadow);}
    }
}
