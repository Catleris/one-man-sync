package lt.oneman.sync;

import net.runelite.client.plugins.timetracking.farming.CropState;
import net.runelite.client.plugins.timetracking.farming.Produce;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmingHealthTest {
    private FarmingPatchDecoder.DecodedPatch state(CropState state) {return new FarmingPatchDecoder.DecodedPatch(Produce.MAGIC,state,1,1700000000L);}
    @Test public void diseaseAlertsOnlyOnceUntilObservedRecovery() {
        FarmingHealth health=new FarmingHealth();
        assertTrue(health.observe("Tree: Varrock",state(CropState.DISEASED)));
        assertFalse(health.observe("Tree: Varrock",state(CropState.DISEASED)));
        assertFalse(health.observe("Tree: Varrock",null));
        assertFalse(health.observe("Tree: Varrock",state(CropState.DEAD)));
        assertFalse(health.observe("Tree: Varrock",state(CropState.GROWING)));
        assertTrue(health.observe("Tree: Varrock",state(CropState.DISEASED)));
    }
    @Test public void unknownAmountsDoNotMeanEmpty() {
        FarmingHealth health=new FarmingHealth();health.buckets(1);
        assertFalse(health.low(50));assertFalse(health.notifyLow(50));
        assertTrue(health.bucketText(50,100).contains("unknown"));
    }
    @Test public void explicitCheckSupportsCommaSeparatedAndSingularCounts() {
        FarmingHealth health=new FarmingHealth();health.buckets(1);
        assertTrue(health.message("Your bottomless compost bucket is currently holding 1,234 uses of ultracompost.",100));
        assertFalse(health.low(50));assertTrue(health.bucketText(50,160).contains("1234"));
        assertTrue(health.message("Your bottomless compost bucket is currently holding one use of supercompost.",200));
        assertTrue(health.low(50));
    }
    @Test public void lowNotificationRearmsOnlyAfterRefill() {
        FarmingHealth health=new FarmingHealth();health.buckets(1);
        health.message("Your bottomless compost bucket has 50 uses of regular compost remaining.",100);
        assertTrue(health.notifyLow(50));assertFalse(health.notifyLow(50));
        health.message("You fill your bottomless compost bucket with 100 buckets of ultracompost. Your bottomless compost bucket now contains a total of 250 uses.",120);
        assertFalse(health.notifyLow(50));
        health.message("Your bottomless compost bucket has a single use of ultracompost remaining.",140);
        assertTrue(health.notifyLow(50));
    }
    @Test public void noInferenceFromOrdinaryCompostMessages() {
        FarmingHealth health=new FarmingHealth();health.buckets(1);
        assertFalse(health.message("You treat the herb patch with ultracompost.",100));
        assertFalse(health.low(10000));
        assertFalse(health.message("Your bottomless compost bucket is currently holding 50,000 uses of ultracompost.",100));
    }
    @Test public void multipleBucketsAndProfileResetDoNotShareCounts() {
        FarmingHealth health=new FarmingHealth();health.buckets(1);
        health.message("Your bottomless compost bucket has run out of compost!",100);
        assertTrue(health.low(0));
        health.buckets(2);assertFalse(health.low(50));
        assertFalse(health.message("Your bottomless compost bucket has run out of compost!",110));
        health.buckets(1);assertTrue(health.bucketText(50,120).contains("unknown"));
        health.reset();assertEquals("",health.bucketText(50,130));
    }
    @Test public void emptyItemIsConfirmedButMissingBucketIsSilent() {
        FarmingHealth health=new FarmingHealth();health.buckets(1);health.empty(100);
        assertTrue(health.low(0));assertTrue(health.bucketText(50,160).contains("REFILL"));
        health.buckets(0);assertEquals("",health.bucketText(50,200));
    }
}
