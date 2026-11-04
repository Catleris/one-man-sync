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
    @Test public void stripsCombatCompletionMarkerFromEveryTierAndDeduplicatesCleanNames(){
        String[] tiers={"easy","medium","hard","elite","master","grandmaster"};
        for(int i=0;i<tiers.length;i++){
            AchievementAlarmQueue.Notice notice=AchievementAlarmQueue.parse(
                "CA_ID:700|Congratulations, you've completed an "+tiers[i]+" combat task: @ach_comp@Mummy! ("+(i+1)+" points).");
            assertNotNull(notice);assertEquals("Mummy!",notice.name);assertEquals(i,notice.tier);
        }
        AchievementAlarmQueue.Notice tagged=AchievementAlarmQueue.parse(
            "Congratulations, you've completed an easy combat task: @ach_comp@Mummy!");
        AchievementAlarmQueue.Notice plain=AchievementAlarmQueue.parse(
            "Congratulations, you've completed an easy combat task: Mummy!");
        assertEquals(plain.key,tagged.key);
        AchievementAlarmQueue queue=new AchievementAlarmQueue();queue.add(tagged,100);queue.add(plain,101);
        assertSame(tagged,queue.advance(100,3,true,true));assertNull(queue.advance(3100,3,true,true));
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
    @Test public void onlyRealCurrentOrPendingNoticesReplaceNativePopups(){
        AchievementAlarmQueue q=new AchievementAlarmQueue();
        q.add(new AchievementAlarmQueue.Notice(false,"Preview","NEW ITEM",-1,true),100);
        q.advance(100,3,true,true);
        assertFalse(q.containsReal(AchievementAlarmQueue.Kind.COLLECTION));
        q.add(AchievementAlarmQueue.parse("New item added to your collection log: Huasca seed"),101);
        assertTrue(q.containsReal(AchievementAlarmQueue.Kind.COLLECTION));
        assertFalse(q.containsReal(AchievementAlarmQueue.Kind.COMBAT_TASK));
        q.advance(3100,3,true,true);assertTrue(q.containsReal(AchievementAlarmQueue.Kind.COLLECTION));
        q.advance(6100,3,true,true);assertFalse(q.containsReal(AchievementAlarmQueue.Kind.COLLECTION));
        q.add(AchievementAlarmQueue.parse("New item added to your collection log: Another item"),6200);
        q.advance(6200,3,false,true);assertFalse(q.containsReal(AchievementAlarmQueue.Kind.COLLECTION));
        q.clear();assertFalse(q.containsReal(AchievementAlarmQueue.Kind.COLLECTION));
    }
}
