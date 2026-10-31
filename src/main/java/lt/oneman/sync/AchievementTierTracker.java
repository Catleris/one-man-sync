package lt.oneman.sync;
import java.util.*;
/** Session-local transitions only: first observations establish a silent baseline. */
final class AchievementTierTracker {
    private int points;
    private int[] thresholds;
    private boolean[] diaries;
    List<AchievementAlarmQueue.Notice> observe(int points,int[] thresholds,boolean[] diaries,boolean combatEnabled,boolean diaryEnabled) {
        List<AchievementAlarmQueue.Notice> notices=new ArrayList<>();
        if(this.thresholds!=null)for(int i=0;i<Math.min(6,thresholds.length);i++) {
            int target=thresholds[i];
            // A missing or changed threshold is a new baseline, not an earned unlock.
            if(i<this.thresholds.length&&target>0&&target==this.thresholds[i]&&this.points<target&&points>=target&&combatEnabled)
                notices.add(new AchievementAlarmQueue.Notice(AchievementAlarmQueue.Kind.COMBAT_TIER,
                    AchievementAlarmQueue.TIERS[i]+" tier unlocked","TIER UNLOCKED",i,false));
        }
        if(this.diaries!=null)for(int i=0;i<Math.min(diaries.length,DiaryUnlockCatalog.ENTRIES.size());i++) {
            if(i<this.diaries.length&&!this.diaries[i]&&diaries[i]&&diaryEnabled) {
                DiaryUnlockCatalog.Entry entry=DiaryUnlockCatalog.ENTRIES.get(i);
                notices.add(new AchievementAlarmQueue.Notice(AchievementAlarmQueue.Kind.DIARY,entry.area+" Diary","DIARY COMPLETED",entry.tier,false));
            }
        }
        this.points=points;this.thresholds=thresholds.clone();this.diaries=diaries.clone();return notices;
    }
    void baselineCombat(int points,int[] thresholds){this.points=points;this.thresholds=thresholds.clone();}
    void baselineDiaries(boolean[] diaries){this.diaries=diaries.clone();}
    void reset(){points=0;thresholds=null;diaries=null;}
}
