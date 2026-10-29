package lt.oneman.sync;
import org.junit.Test;
import static org.junit.Assert.*;
public class AchievementAlarmQueueTest {
    @Test public void acceptsAnyCollectionItemIncludingPets(){
        assertEquals("Pet snakeling",AchievementAlarmQueue.parse("New item added to your collection log: Pet snakeling").name);
        assertEquals("Future unknown item",AchievementAlarmQueue.parse("New item added to your collection log: Future unknown item.").name);
        assertNull(AchievementAlarmQueue.parse("Valuable drop: Dragon warhammer"));
    }
    @Test public void acceptsAllCombatTiersAndOptionalId(){
        String[] tiers={"easy","medium","hard","elite","master","grandmaster"};
        for(int i=0;i<tiers.length;i++){
            AchievementAlarmQueue.Notice n=AchievementAlarmQueue.parse("CA_ID:700|Congratulations, you've completed an "+tiers[i]+" combat task: A future task ("+(i+1)+" points).");
            assertNotNull(n);assertEquals(i,n.tier);assertEquals("A future task",n.name);
        }
        assertNotNull(AchievementAlarmQueue.parse("Congratulations, you've completed a hard combat task: Perfect kill."));
        assertNull(AchievementAlarmQueue.parse("A friend completed a hard combat task"));
    }
    @Test public void deduplicatesQueuesExpiresAndClears(){
        AchievementAlarmQueue q=new AchievementAlarmQueue();
        AchievementAlarmQueue.Notice a=AchievementAlarmQueue.parse("New item added to your collection log: Bran");
        AchievementAlarmQueue.Notice b=AchievementAlarmQueue.parse("New item added to your collection log: Pet snakeling");
        q.add(a,100);q.add(a,101);q.add(b,102);
        assertSame(a,q.advance(100,3,true,true));assertSame(a,q.advance(3099,3,true,true));
        assertSame(b,q.advance(3100,3,true,true));assertNull(q.advance(6100,3,true,true));
        q.add(a,12000);assertSame(a,q.advance(12000,3,true,true));assertNull(q.advance(12001,3,false,true));
        q.add(b,22000);q.clear();assertNull(q.advance(22000,3,true,true));
    }
}
