package lt.oneman.sync;

import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

/** Session-local last NPC reference. Inventory information stays until replaced or cleared. */
@Singleton
final class TargetInfo implements MouseListener
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private OneManSyncConfig config;
    @Inject private ConfigManager configs;
    @Inject private OverlayManager overlays;
    @Inject private TargetInfoOverlay overlay;
    @Inject private MouseManager mouseManager;
    @Inject private EventBus eventBus;
    private final TargetInfoData data=new TargetInfoData();
    private boolean active;
    private NPC blockedTarget;
    private boolean captured, consumeClick;
    static final class Snapshot {
        final String name;final TargetInfoData.Stats stats;
        Snapshot(String name,TargetInfoData.Stats stats){this.name=name;this.stats=stats;}
    }
    void startUp(){active=true;clear();overlay.infoView=true;overlays.add(overlay);mouseManager.registerMouseListener(0,this);eventBus.register(this);}
    void shutDown(){active=false;clear();overlays.remove(overlay);mouseManager.unregisterMouseListener(this);eventBus.unregister(this);}
    void clear(){overlay.snapshot=null;overlay.hideHitAreas();blockedTarget=null;}
    void open(){if(active&&config.monstersTargetInfo())overlay.infoView=true;}
    void showItems(){overlay.infoView=false;overlay.hideHitAreas();}
    void configChanged(){if(!config.monstersTargetInfo())clear();}
    void gameStateChanged(GameState state){
        // Loading/teleporting does not discard the user's selected reference.
        if(state==GameState.LOGIN_SCREEN||state==GameState.HOPPING||state==GameState.CONNECTION_LOST||state==GameState.LOGGING_IN)clear();
        else overlay.hideHitAreas();
    }
    void tick(OneManSlayerState.LocalTask task){
        if(!active||!config.monstersTargetInfo()){clear();return;}
        if(client.getGameState()!=GameState.LOGGED_IN){overlay.hideHitAreas();return;}
        if(config.monstersTargetSlayerOnly()&&overlay.snapshot!=null&&!matchesTask(task,overlay.snapshot.name))overlay.snapshot=null;
        Player player=client.getLocalPlayer();
        Actor actor=player==null?null:player.getInteracting();
        if(!(actor instanceof NPC)){blockedTarget=null;return;}
        NPC npc=(NPC)actor;
        if(npc==blockedTarget||npc.isDead()||(npc.getHealthScale()>0&&npc.getHealthRatio()==0))return;
        NPCComposition composition=npc.getTransformedComposition();
        if(composition==null||npc.getName()==null)return;
        String name=Text.removeTags(npc.getName()).trim();
        if(config.monstersTargetSlayerOnly()&&!matchesTask(task,name)){overlay.snapshot=null;return;}
        overlay.snapshot=new Snapshot(name,data.find(composition.getId()));
    }
    @Subscribe public void onPostMenuSort(PostMenuSort event){
        // The information page covers the item grid. Do not expose hidden item actions/tooltips.
        net.runelite.api.Point mouse=client.getMouseCanvasPosition();
        if(active&&config.monstersTargetInfo()&&mouse!=null&&overlay.blocksItems(mouse.getX(),mouse.getY()))
            client.setMenuEntries(new net.runelite.api.MenuEntry[0]);
    }
    private void action(int action){
        if(!active||!config.monstersTargetInfo())return;
        if(action==TargetInfoOverlay.ITEMS)showItems();
        else if(action==TargetInfoOverlay.INFO)open();
        else if(action==TargetInfoOverlay.DETAILS)configs.setConfiguration("one-man-sync","monstersTargetExpanded",!config.monstersTargetExpanded());
        else if(action==TargetInfoOverlay.CLEAR){
            overlay.snapshot=null;
            Player player=client.getLocalPlayer();Actor target=player==null?null:player.getInteracting();
            blockedTarget=target instanceof NPC?(NPC)target:null;
        }
    }
    @Override public MouseEvent mousePressed(MouseEvent e){
        if(!active||!config.monstersTargetInfo())return e;
        int hit=overlay.hit(e.getX(),e.getY());
        if(hit!=TargetInfoOverlay.OUTSIDE){
            captured=true;consumeClick=true;e.consume();
            if(e.getButton()==MouseEvent.BUTTON1&&hit>0)clientThread.invokeLater(()->action(hit));
        }
        return e;
    }
    @Override public MouseEvent mouseReleased(MouseEvent e){if(captured){e.consume();captured=false;}return e;}
    @Override public MouseEvent mouseClicked(MouseEvent e){if(consumeClick){e.consume();consumeClick=false;}return e;}
    @Override public MouseEvent mouseDragged(MouseEvent e){if(captured)e.consume();return e;}
    @Override public MouseEvent mouseMoved(MouseEvent e){return e;}
    @Override public MouseEvent mouseEntered(MouseEvent e){return e;}
    @Override public MouseEvent mouseExited(MouseEvent e){return e;}
    static boolean matchesTask(OneManSlayerState.LocalTask task,String name){
        if(task==null||task.remaining<=0||task.name.isEmpty())return false;
        SlayerLabKnowledge.Entry entry=SlayerLabKnowledge.find(task.name);
        return entry!=null&&entry.matchesNpc(name);
    }
}
