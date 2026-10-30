package lt.oneman.sync;
import java.util.*;
/** Reuses Companion quest snapshots. First/unknown observations are silent history. */
final class QuestAlarmTracker {
    private final Map<String,Boolean> known=new HashMap<>();
    List<AchievementAlarmQueue.Notice> observe(Map<String,Boolean> finished,boolean enabled){
        List<AchievementAlarmQueue.Notice> notices=new ArrayList<>();
        finished.forEach((name,complete)->{
            if(name==null||name.isEmpty()||complete==null)return;
            Boolean previous=known.get(name);
            if(previous==null){known.put(name,complete);return;}
            // Completion cannot legitimately regress during a session. Ignore transient reads.
            if(!previous&&complete){
                known.put(name,true);
                if(enabled)notices.add(new AchievementAlarmQueue.Notice(AchievementAlarmQueue.Kind.QUEST,name,"COMPLETED",-1,false));
            }
        });
        return notices;
    }
    void reset(){known.clear();}
}
