package lt.oneman.sync;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

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
    private SpritePixels replacement,previous;
    private boolean active;
    private int retry;
    private long previewUntil;
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
        if(!active||!config.alarmQuests()){closed();return;}
        if(scroll!=null){
            if(client.getWidget(InterfaceID.Questscroll.SCROLL)==scroll&&!scroll.isHidden()){for(TextStyle style:textStyles)style.apply();return;}
            restore();
        }
        if(retry>0){retry--;apply();}
    }
    boolean apply(){
        if(!active||!config.alarmQuests()||client.getGameState()!=GameState.LOGGED_IN)return false;
        Widget widget=client.getWidget(InterfaceID.Questscroll.SCROLL);
        if(widget==null||widget.isHidden()||widget.getWidth()<100||widget.getHeight()<100)return false;
        if(widget==scroll)return true;
        restore();
        BufferedImage image=art.background(widget.getWidth(),widget.getHeight());
        SpritePixels sprite=ImageUtil.getImageSpritePixels(image,client);if(sprite==null)return false;
        previous=client.getWidgetSpriteOverrides().get(InterfaceID.Questscroll.SCROLL);
        replacement=sprite;scroll=widget;
        client.getWidgetSpriteOverrides().put(InterfaceID.Questscroll.SCROLL,replacement);
        for(int id:TEXT){
            Widget text=client.getWidget(id);if(text==null)continue;
            int color=(id==InterfaceID.Questscroll.CONGRATULATIONS||id==InterfaceID.Questscroll.QUEST_POINTS)?GOLD:CREAM;
            TextStyle style=new TextStyle(text,color);textStyles.add(style);style.apply();
        }
        previewUntil=0;retry=0;return true;
    }
    private void restore(){
        for(TextStyle style:textStyles)style.restore();textStyles.clear();
        if(replacement!=null&&client.getWidgetSpriteOverrides().get(InterfaceID.Questscroll.SCROLL)==replacement){
            if(previous==null)client.getWidgetSpriteOverrides().remove(InterfaceID.Questscroll.SCROLL);
            else client.getWidgetSpriteOverrides().put(InterfaceID.Questscroll.SCROLL,previous);
        }
        replacement=null;previous=null;scroll=null;
    }
    void preview(){
        if(!active||!config.alarmQuests()||client.getGameState()!=GameState.LOGGED_IN)return;
        if(client.getWidget(InterfaceID.Questscroll.SCROLL)!=null&&!client.getWidget(InterfaceID.Questscroll.SCROLL).isHidden()){opened();return;}
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
