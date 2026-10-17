package lt.oneman.sync;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClueRewardCaptureTest
{
    @Test public void completionThenFreshContainerAndDuplicateMessage()
    {
        ClueRewardCapture capture = new ClueRewardCapture();
        assertFalse(capture.completion("You have completed 12 hard Treasure Trails.", 10));
        assertTrue(capture.containerChanged(11));
        assertEquals(ClueTier.HARD, capture.tier());
        capture.consumed();
        assertFalse(capture.completion("You have completed 12 hard Treasure Trails.", 11));
        assertFalse(capture.containerChanged(12));
    }
    @Test public void reversedEventOrderIsAcceptedOnlyForTheSameTick()
    {
        ClueRewardCapture capture = new ClueRewardCapture();
        assertFalse(capture.containerChanged(10));
        assertTrue(capture.completion("You have completed 1 beginner Treasure Trail.", 10));
        assertEquals(ClueTier.BEGINNER, capture.tier());
        capture.consumed();
        assertFalse(capture.completion("You have completed 2 beginner Treasure Trails.", 11));
    }
    @Test public void staleBarrowsContainerIsNotReusedAndPendingCompletionExpires()
    {
        ClueRewardCapture capture = new ClueRewardCapture();
        capture.containerChanged(1);
        assertFalse(capture.completion("You have completed 20 elite Treasure Trails.", 10));
        capture.tick(16);
        assertNull(capture.tier());
        assertFalse(capture.containerChanged(17));
    }
    @Test public void accountChangeClearsPendingAndDeduplication()
    {
        ClueRewardCapture capture = new ClueRewardCapture();
        String message = "You have completed 1 master Treasure Trail.";
        capture.completion(message, 10); capture.clear();
        assertFalse(capture.containerChanged(11));
        assertTrue(capture.completion(message, 11));
        assertEquals(ClueTier.MASTER, capture.tier());
        assertFalse(capture.completion("Other player: I got a reward casket!", 11));
    }
}
