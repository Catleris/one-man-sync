package lt.oneman.sync;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class AchievementTierTrackerTest {
    private final int[] thresholds={10,20,30,40,50,60};
    @Test public void loginIsSilentAndAllSixCrossingsAreReportedOnce(){
        AchievementTierTracker tracker=new AchievementTierTracker();boolean[] diaries=new boolean[48];
        assertTrue(tracker.observe(0,thresholds,diaries,true,true).isEmpty());
        for(int i=0;i<6;i++){
            List<AchievementAlarmQueue.Notice> result=tracker.observe(thresholds[i],thresholds,diaries,true,true);
            assertEquals(1,result.size());assertEquals(AchievementAlarmQueue.Kind.COMBAT_TIER,result.get(0).kind);assertEquals(i,result.get(0).tier);
            assertTrue(tracker.observe(thresholds[i],thresholds,diaries,true,true).isEmpty());
        }
        tracker.reset();assertTrue(tracker.observe(60,thresholds,diaries,true,true).isEmpty());
    }
    @Test public void coversAllDiaryRegionsAndTiersIncludingLegacyKaramja(){
        assertEquals(48,DiaryUnlockCatalog.ENTRIES.size());
        assertEquals(12,DiaryUnlockCatalog.ENTRIES.stream().map(e->e.area).distinct().count());
        AchievementTierTracker tracker=new AchievementTierTracker();boolean[] states=new boolean[48];
        tracker.observe(0,thresholds,states,true,true);
        for(int i=0;i<48;i++){
            DiaryUnlockCatalog.Entry entry=DiaryUnlockCatalog.ENTRIES.get(i);
            assertFalse(entry.complete(0));
            assertTrue(entry.complete("Karamja".equals(entry.area)&&entry.tier<3?2:1));
            states[i]=true;List<AchievementAlarmQueue.Notice> result=tracker.observe(0,thresholds,states,true,true);
            assertEquals(1,result.size());assertEquals(AchievementAlarmQueue.Kind.DIARY,result.get(0).kind);
            assertEquals(entry.area+" Diary",result.get(0).name);assertEquals(entry.tier,result.get(0).tier);
        }
    }
    @Test public void disabledAlarmsAndMissingOrChangedThresholdsDoNotReplay(){
        AchievementTierTracker tracker=new AchievementTierTracker();boolean[] states=new boolean[48];
        tracker.observe(0,new int[6],states,true,true);
        states[0]=true;assertTrue(tracker.observe(60,thresholds,states,false,false).isEmpty());
        assertTrue(tracker.observe(60,thresholds,states,true,true).isEmpty());
        tracker.reset();tracker.observe(0,thresholds,new boolean[48],true,true);
        assertTrue(tracker.observe(40,thresholds,states,false,false).isEmpty());
        assertTrue(tracker.observe(40,thresholds,states,true,true).isEmpty());
        assertEquals(1,tracker.observe(50,thresholds,states,true,true).size());
        tracker.reset();assertTrue(tracker.observe(60,thresholds,states,true,true).isEmpty());
        int[] lower={5,10,15,20,25,30};assertTrue(tracker.observe(60,lower,states,true,true).isEmpty());
    }
    @Test public void taskAndTierRemainSeparateAndDisablingOnePreservesTheOther(){
        AchievementAlarmQueue q=new AchievementAlarmQueue();
        AchievementAlarmQueue.Notice task=new AchievementAlarmQueue.Notice(true,"Example","",0,false);
        AchievementAlarmQueue.Notice tier=new AchievementAlarmQueue.Notice(AchievementAlarmQueue.Kind.COMBAT_TIER,"Easy tier unlocked","TIER UNLOCKED",0,false);
        q.add(task,100);q.add(tier,100);assertSame(task,q.advance(100,3,n->true));
        assertSame(tier,q.advance(101,3,n->n.kind!=AchievementAlarmQueue.Kind.COMBAT_TASK));
        assertNull(q.advance(102,3,n->n.kind!=AchievementAlarmQueue.Kind.COMBAT_TIER));
    }
}
