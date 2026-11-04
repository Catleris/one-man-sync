package lt.oneman.sync;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** Passive, movable card beside the inventory; never alters inventory widgets or click zones. */
@Singleton
final class TargetInfoOverlay extends OverlayPanel
{
    private static final Color GOLD=new Color(218,176,85), MUTED=new Color(170,179,172), GREEN=new Color(133,211,164);
    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    volatile TargetInfo.Snapshot snapshot;
    @Inject TargetInfoOverlay(ConfigManager configs){
        setPosition(OverlayPosition.BOTTOM_RIGHT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setResizable(false);
        panelComponent.setPreferredSize(new Dimension(194,0));
        panelComponent.setBackgroundColor(new Color(24,32,28,238));
        panelComponent.setGap(new Point(0,3));
        addMenuEntry(MenuAction.RUNELITE_OVERLAY,"Toggle details","OneMan target info",
            event->configs.setConfiguration("one-man-sync","monstersTargetExpanded",!config.monstersTargetExpanded()));
        addMenuEntry(MenuAction.RUNELITE_OVERLAY,"Hide","OneMan target info",
            event->configs.setConfiguration("one-man-sync","monstersTargetInfo",false));
    }
    @Override public Dimension render(Graphics2D graphics){
        TargetInfo.Snapshot current=snapshot;
        if(!config.monstersTargetInfo()||current==null||client.getGameState()!=GameState.LOGGED_IN||!inventoryVisible())return null;
        panelComponent.getChildren().add(TitleComponent.builder().text(shorten(current.name,25)).color(GOLD).build());
        TargetInfoData.Stats s=current.stats;
        if(s==null){line("Reference stats","Unavailable",MUTED);return super.render(graphics);}
        line("Element",s.elementText(),GREEN);
        line("Melee bonus",s.meleeText(),Color.WHITE);
        if(config.monstersTargetExpanded()){
            line("Stab / Slash / Crush",TargetInfoData.value(s.stab)+" / "+TargetInfoData.value(s.slash)+" / "+TargetInfoData.value(s.crush),Color.WHITE);
            line("Magic defence",TargetInfoData.value(s.magic),Color.WHITE);
            line("Ranged L / S / H",TargetInfoData.value(s.light)+" / "+TargetInfoData.value(s.standard)+" / "+TargetInfoData.value(s.heavy),Color.WHITE);
            line("Def / Magic level",TargetInfoData.value(s.defence)+" / "+TargetInfoData.value(s.magicLevel),Color.WHITE);
            line("Base HP",TargetInfoData.value(s.hp),Color.WHITE);
            line("Base attack speed",s.speed==null||s.speed<=0?"?":s.speed+" ticks",Color.WHITE);
            line("Static reference","Not live stats",MUTED);
        }
        return super.render(graphics);
    }
    private void line(String left,String right,Color color){
        panelComponent.getChildren().add(LineComponent.builder().left(left).leftColor(MUTED).right(right).rightColor(color).build());
    }
    private boolean inventoryVisible(){
        // The inventory's sole root has no named child component in gamevals.
        Widget inventory=client.getWidget(InterfaceID.INVENTORY,0);
        return inventory!=null&&!inventory.isHidden();
    }
    static String shorten(String name,int max){return name.length()>max?name.substring(0,max-1)+"…":name;}
}
