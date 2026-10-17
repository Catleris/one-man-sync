package lt.oneman.sync;

import java.util.EnumSet;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClueItemCatalogTest
{
    @Test public void emoteTextsResolveAgainstInstalledRuneLite()
    {
        assertEquals("Update the bundled texts when RuneLite changes emote clues", 0, new ClueItemCatalog().unresolvedSteps());
    }
    @Test public void requiredItemsSupportAlternativesAndMultipleTiers()
    {
        ClueItemCatalog data = new ClueItemCatalog();
        assertTrue(data.classify(ItemID.GOLD_RING, "Gold ring").required.contains(ClueTier.BEGINNER));
        assertTrue(data.classify(ItemID.ABYSSAL_WHIP, "Abyssal whip").required.contains(ClueTier.MASTER));
        assertTrue(data.classify(ItemID.LEATHER_CHAPS, "Leather chaps").required.containsAll(EnumSet.of(ClueTier.BEGINNER, ClueTier.EASY)));
        assertTrue(data.classify(ItemID.IBANSTAFF_UPGRADED, "Iban's staff (u)").required.contains(ClueTier.MASTER));
        assertTrue("empty-slot checks must not match unrelated items", data.classify(ItemID.COINS, "Coins").required.isEmpty());
    }
    @Test public void knownRewardsHaveCorrectTiersAndCommonRewardsAreOptional()
    {
        ClueItemCatalog data = new ClueItemCatalog();
        assertEquals(EnumSet.of(ClueTier.BEGINNER), data.rewardTiers("Mole slippers", false));
        assertEquals(EnumSet.of(ClueTier.MEDIUM), data.rewardTiers("Ranger boots", false));
        assertTrue(data.rewardTiers("3rd age platebody", false).containsAll(EnumSet.of(ClueTier.HARD, ClueTier.ELITE, ClueTier.MASTER)));
        ClueItemCatalog.Tags coins = data.classify(ItemID.COINS, "Coins");
        assertTrue(coins.visible(true, true, false).isEmpty());
        assertFalse(coins.visible(true, true, true).isEmpty());
    }
    @Test public void clueContainersRecognizeTierWithoutTaggingUnrelatedItems()
    {
        assertEquals(ClueTier.BEGINNER, ClueTier.scrollTier("Clue scroll (beginner)"));
        assertEquals(ClueTier.MASTER, ClueTier.scrollTier("Reward casket (master)"));
        assertEquals(ClueTier.HARD, ClueTier.scrollTier("Clue geode (hard)"));
        assertNull(ClueTier.scrollTier("Master wand"));
        assertNull(ClueTier.scrollTier("Clue hunter boots"));
    }
}
