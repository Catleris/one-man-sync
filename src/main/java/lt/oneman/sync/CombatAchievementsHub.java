package lt.oneman.sync;

import com.google.gson.Gson;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

/** Passive account progress. All client/config reads and writes occur on the client thread. */
@Slf4j
@Singleton
final class CombatAchievementsHub
{
    static final String TRACKED_KEY="combatHubTracked";
    static final int[] VARPS={
        VarPlayerID.CA_TASK_COMPLETED_0,VarPlayerID.CA_TASK_COMPLETED_1,VarPlayerID.CA_TASK_COMPLETED_2,
        VarPlayerID.CA_TASK_COMPLETED_3,VarPlayerID.CA_TASK_COMPLETED_4,VarPlayerID.CA_TASK_COMPLETED_5,
        VarPlayerID.CA_TASK_COMPLETED_6,VarPlayerID.CA_TASK_COMPLETED_7,VarPlayerID.CA_TASK_COMPLETED_8,
        VarPlayerID.CA_TASK_COMPLETED_9,VarPlayerID.CA_TASK_COMPLETED_10,VarPlayerID.CA_TASK_COMPLETED_11,
        VarPlayerID.CA_TASK_COMPLETED_12,VarPlayerID.CA_TASK_COMPLETED_13,VarPlayerID.CA_TASK_COMPLETED_14,
        VarPlayerID.CA_TASK_COMPLETED_15,VarPlayerID.CA_TASK_COMPLETED_16,VarPlayerID.CA_TASK_COMPLETED_17,
        VarPlayerID.CA_TASK_COMPLETED_18,VarPlayerID.CA_TASK_COMPLETED_19,VarPlayerID.CA_TASK_COMPLETED_20};
    static final int[] THRESHOLDS={VarbitID.CA_THRESHOLD_EASY,VarbitID.CA_THRESHOLD_MEDIUM,VarbitID.CA_THRESHOLD_HARD,
        VarbitID.CA_THRESHOLD_ELITE,VarbitID.CA_THRESHOLD_MASTER,VarbitID.CA_THRESHOLD_GRANDMASTER};
    static final class Snapshot {
        final List<CombatAchievementCatalog.Task> tasks;final Set<Integer> completed,tracked;
        final int[] thresholds;final int points;final boolean ready;final String profile;
        Snapshot(List<CombatAchievementCatalog.Task> tasks,Set<Integer> completed,Set<Integer> tracked,int[] thresholds,int points,boolean ready,String profile){
            this.tasks=tasks;this.completed=Collections.unmodifiableSet(new HashSet<>(completed));this.tracked=Collections.unmodifiableSet(new HashSet<>(tracked));
            this.thresholds=thresholds.clone();this.points=points;this.ready=ready;this.profile=profile;
        }
        static Snapshot empty(){return new Snapshot(Collections.emptyList(),Collections.emptySet(),Collections.emptySet(),new int[6],0,false,"");}
        boolean known(CombatAchievementCatalog.Task task){return ready&&task.id/32<VARPS.length;}
    }
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ConfigManager configs;
    @Inject private Gson gson;
    private volatile boolean active;
    private volatile Snapshot snapshot=Snapshot.empty();
    private CombatAchievementsPanel panel;
    private List<CombatAchievementCatalog.Task> catalog=Collections.emptyList();
    private Set<Integer> tracked=new HashSet<>();
    private String profile="";
    private boolean dirty=true;
    private int delay=10,retry,cooldown;
    void startUp(){active=true;dirty=true;delay=10;retry=0;cooldown=0;profile="";tracked.clear();snapshot=Snapshot.empty();}
    void shutDown(){active=false;profile="";catalog=Collections.emptyList();tracked.clear();snapshot=Snapshot.empty();render();}
    JPanel companionPanel(){if(panel==null)panel=new CombatAchievementsPanel(this::track);panel.update(snapshot);return panel;}
    Snapshot snapshot(){return snapshot;}
    void changed(){dirty=true;}
    void onGameStateChanged(GameStateChanged event){
        if(event.getGameState()==GameState.LOGGED_IN){delay=10;dirty=true;retry=0;}
        else {profile="";tracked.clear();snapshot=Snapshot.empty();render();}
    }
    void onGameTick(){
        if(!active||client.getGameState()!=GameState.LOGGED_IN)return;
        String key=configs.getRSProfileKey();
        if(key==null||key.isEmpty()){if(!profile.isEmpty()){profile="";tracked.clear();snapshot=Snapshot.empty();render();}return;}
        if(!key.equals(profile)){
            profile=key;tracked.clear();snapshot=Snapshot.empty();render();dirty=true;delay=10;
            try {String saved=configs.getConfiguration("one-man-sync",key,TRACKED_KEY);
                int[] ids=saved==null?null:gson.fromJson(saved,int[].class);if(ids!=null)for(int id:ids)if(id>=0)tracked.add(id);
            }catch(RuntimeException error){log.debug("Unable to read tracked combat tasks",error);}
        }
        if(delay>0){delay--;return;}
        if(cooldown>0){cooldown--;return;}
        if(!dirty)return;
        cooldown=4;
        if(catalog.isEmpty()){
            if(retry++%10!=0)return;
            try{catalog=CombatAchievementCatalog.load(client);}catch(RuntimeException error){log.debug("Combat task cache not ready",error);return;}
            if(catalog.isEmpty())return;
        }
        int[] packed=new int[VARPS.length];for(int i=0;i<VARPS.length;i++)packed[i]=client.getVarpValue(VARPS[i]);
        Set<Integer> completed=new HashSet<>();for(CombatAchievementCatalog.Task task:catalog)if(CombatAchievementCatalog.completed(task.id,packed))completed.add(task.id);
        int[] thresholds=new int[THRESHOLDS.length];for(int i=0;i<thresholds.length;i++)thresholds[i]=client.getVarbitValue(THRESHOLDS[i]);
        int points=Math.max(0,client.getVarbitValue(VarbitID.CA_POINTS));
        boolean changed=!snapshot.ready||!snapshot.profile.equals(profile)||!snapshot.completed.equals(completed)||!snapshot.tracked.equals(tracked)
            ||!Arrays.equals(snapshot.thresholds,thresholds)||snapshot.points!=points;
        snapshot=new Snapshot(catalog,completed,tracked,thresholds,points,true,profile);
        dirty=false;if(changed)render();
    }
    private void track(int id,boolean selected,String expectedProfile){
        clientThread.invokeLater(()->{
            if(!active||!snapshot.ready||!profile.equals(expectedProfile)||!profile.equals(configs.getRSProfileKey())||client.getGameState()!=GameState.LOGGED_IN)return;
            if(catalog.stream().noneMatch(task->task.id==id))return;
            if(selected)tracked.add(id);else tracked.remove(id);
            configs.setConfiguration("one-man-sync",profile,TRACKED_KEY,gson.toJson(tracked));dirty=true;
        });
    }
    private void render(){SwingUtilities.invokeLater(()->{if(panel!=null)panel.update(snapshot);});}
}
