package lt.oneman.sync;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuestAlarmTrackerTest {
    @Test public void silentHistoryThenEveryNewCompletionIncludingMiniquests(){
        QuestAlarmTracker tracker=new QuestAlarmTracker();Map<String,Boolean> states=new LinkedHashMap<>();
        states.put("Cook's Assistant",true);states.put("Waterfall Quest",false);states.put("Mage Arena II",false);
        assertTrue(tracker.observe(states,true).isEmpty());
        states.put("Waterfall Quest",true);states.put("Mage Arena II",true);
        List<AchievementAlarmQueue.Notice> notices=tracker.observe(states,true);assertEquals(2,notices.size());
        assertEquals("Waterfall Quest",notices.get(0).name);assertEquals("Mage Arena II",notices.get(1).name);
        for(AchievementAlarmQueue.Notice n:notices){assertEquals(AchievementAlarmQueue.Kind.QUEST,n.kind);assertEquals(-1,n.tier);assertFalse(n.preview);}
        assertTrue(tracker.observe(states,true).isEmpty());
    }
    @Test public void disabledAndNewlyDiscoveredCompletedQuestsAreNotReplayed(){
        QuestAlarmTracker tracker=new QuestAlarmTracker();Map<String,Boolean> states=new HashMap<>();states.put("Waterfall Quest",false);
        tracker.observe(states,false);states.put("Waterfall Quest",true);assertTrue(tracker.observe(states,false).isEmpty());
        states.put("Newly discovered quest",true);assertTrue(tracker.observe(states,true).isEmpty());
        tracker.reset();assertTrue(tracker.observe(states,true).isEmpty());
    }
    @Test public void transientUnknownOrRegressedStatesCannotRepeatCompletion(){
        QuestAlarmTracker tracker=new QuestAlarmTracker();Map<String,Boolean> states=new HashMap<>();states.put("A quest",false);tracker.observe(states,true);
        states.put("A quest",true);assertEquals(1,tracker.observe(states,true).size());
        assertTrue(tracker.observe(Collections.emptyMap(),true).isEmpty());
        states.put("A quest",false);assertTrue(tracker.observe(states,true).isEmpty());
        states.put("A quest",true);assertTrue(tracker.observe(states,true).isEmpty());
    }
    @Test public void questToggleAndQueueAreIndependentOfCollection(){
        AchievementAlarmQueue queue=new AchievementAlarmQueue();
        AchievementAlarmQueue.Notice quest=new AchievementAlarmQueue.Notice(AchievementAlarmQueue.Kind.QUEST,"Waterfall Quest","COMPLETED",-1,false);
        AchievementAlarmQueue.Notice item=AchievementAlarmQueue.parse("New item added to your collection log: Bran");
        queue.add(quest,100);queue.add(item,100);
        assertSame(item,queue.advance(100,3,n->n.kind==AchievementAlarmQueue.Kind.COLLECTION));
        assertNull(queue.advance(4000,3,n->true));
    }
}
