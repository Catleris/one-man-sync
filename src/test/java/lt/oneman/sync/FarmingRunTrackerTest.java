package lt.oneman.sync;

import net.runelite.client.plugins.timetracking.farming.CropState;
import net.runelite.client.plugins.timetracking.farming.Produce;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmingRunTrackerTest {
    private static final long SEEN = 1700000000L;
    @Test public void herbsDecodeAndEstimateTheirRemainingGrowth() {
        FarmingPatchDecoder.DecodedPatch patch = FarmingPatchDecoder.decode(FarmingPatchType.HERB,"33:"+SEEN);
        assertEquals(Produce.RANARR, patch.getProduce());
        assertEquals(CropState.GROWING, patch.getCropState());
        assertEquals(1, patch.getStage());
        assertEquals(SEEN+3600, patch.estimateReadyEpochSec());
        assertFalse(patch.isReady(SEEN+3599));
        assertTrue(patch.isReady(SEEN+3600));
        assertTrue(patch.getDisplayStatus(SEEN+3600).contains("Estimated ready"));
    }
    @Test public void observedHarvestableIsReady() {
        FarmingPatchDecoder.DecodedPatch patch = FarmingPatchDecoder.decode(FarmingPatchType.HERB,"8:"+SEEN);
        assertEquals(Produce.GUAM, patch.getProduce());
        assertEquals(CropState.HARVESTABLE, patch.getCropState());
        assertTrue(patch.isReady(SEEN));
    }
    @Test public void magicTreeUsesItsOwnGrowthRate() {
        FarmingPatchDecoder.DecodedPatch patch = FarmingPatchDecoder.decode(FarmingPatchType.TREE,"48:"+SEEN);
        assertEquals(Produce.MAGIC, patch.getProduce());
        assertEquals(SEEN+28800, patch.estimateReadyEpochSec());
    }
    @Test public void weedsNeverBecomeReadyWithElapsedTime() {
        FarmingPatchDecoder.DecodedPatch patch = FarmingPatchDecoder.decode(FarmingPatchType.HERB,"3:"+SEEN);
        assertEquals(Produce.WEEDS,patch.getProduce());
        assertFalse(patch.isReady(SEEN+86400));
        assertEquals(0,patch.estimateReadyEpochSec());
    }
    @Test public void diseaseAndDeathDoNotBecomeReady() {
        for(String value:new String[]{"130", "171"}) {
            FarmingPatchDecoder.DecodedPatch patch=FarmingPatchDecoder.decode(FarmingPatchType.HERB,value+":"+SEEN);
            assertFalse(patch.isReady(SEEN+86400));
            assertEquals(0,patch.estimateReadyEpochSec());
        }
    }
    @Test public void corruptOrMissingObservationsStayUnknown() {
        for(String value:new String[]{null,"","abc:123","10:0","10:bad","256:123","-1:123","8:123:extra"})
            assertNull(FarmingPatchDecoder.decode(FarmingPatchType.HERB,value));
    }
    @Test public void birdhouseTypesAndStatesAreDistinct() {
        assertEquals(BirdHouseDecoder.State.UNKNOWN,BirdHouseDecoder.getState(-1));
        assertEquals(BirdHouseDecoder.State.UNKNOWN,BirdHouseDecoder.getState(28));
        assertEquals(BirdHouseDecoder.State.EMPTY,BirdHouseDecoder.getState(0));
        assertEquals(BirdHouseDecoder.State.BUILT,BirdHouseDecoder.getState(22));
        assertEquals(BirdHouseDecoder.State.SEEDED,BirdHouseDecoder.getState(24));
        assertEquals("Magic Bird House",BirdHouseDecoder.getHouseName(24));
    }
    @Test public void patchKeysUseRuneLiteRegionAndVarbit() {
        assertEquals("10548.4774",FarmingRunTracker.TRACKED_PATCHES.get(0).configKey());
        assertEquals(17,FarmingRunTracker.TRACKED_PATCHES.size());
        assertEquals("6967.4774",FarmingRunTracker.TRACKED_PATCHES.stream().filter(p->p.getName().equals("Herb: Hosidius")).findFirst().get().configKey());
    }
}
