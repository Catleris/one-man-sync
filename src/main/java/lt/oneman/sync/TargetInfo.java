package lt.oneman.sync;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

/** Reads only the local player's current interaction, on the client thread. */
@Singleton
final class TargetInfo
{
    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    @Inject private OverlayManager overlays;
    @Inject private TargetInfoOverlay overlay;
    private final TargetInfoData data=new TargetInfoData();
    private boolean active;
    static final class Snapshot {
        final String name;final TargetInfoData.Stats stats;
        Snapshot(String name,TargetInfoData.Stats stats){this.name=name;this.stats=stats;}
    }
    void startUp(){active=true;overlay.snapshot=null;overlays.add(overlay);}
    void shutDown(){active=false;clear();overlays.remove(overlay);}
    void clear(){overlay.snapshot=null;}
    void tick(OneManSlayerState.LocalTask task){
        clear();
        if(!active||!config.monstersTargetInfo()||client.getGameState()!=GameState.LOGGED_IN)return;
        Player player=client.getLocalPlayer();
        Actor actor=player==null?null:player.getInteracting();
        if(!(actor instanceof NPC))return;
        NPC npc=(NPC)actor;
        if(npc.isDead()||(npc.getHealthScale()>0&&npc.getHealthRatio()==0))return;
        NPCComposition composition=npc.getTransformedComposition();
        if(composition==null||npc.getName()==null)return;
        String name=Text.removeTags(npc.getName()).trim();
        if(config.monstersTargetSlayerOnly()&&!matchesTask(task,name))return;
        overlay.snapshot=new Snapshot(name,data.find(composition.getId()));
    }
    static boolean matchesTask(OneManSlayerState.LocalTask task,String name){
        if(task==null||task.remaining<=0||task.name.isEmpty())return false;
        SlayerLabKnowledge.Entry entry=SlayerLabKnowledge.find(task.name);
        return entry!=null&&entry.matchesNpc(name);
    }
}
