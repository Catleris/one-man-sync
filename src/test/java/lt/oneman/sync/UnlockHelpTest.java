package lt.oneman.sync;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class UnlockHelpTest
{
    private final UnlockCatalog catalog=new UnlockCatalog();
    @Test public void redDragonsShowPurchaseAndCostWithoutDeveloperDiagnostics()
    {
        UnlockCatalog.Entry entry=catalog.byId.get("SLAYER_REDDRAGONS");
        UnlockCatalog.Result result=new UnlockCatalog.Result(entry,UnlockCatalog.State.PENDING,"Source: game completion / purchase flag\nPurchase flag only; task eligibility, current toggles and stored tasks are separate.");
        assertTrue(UnlockHelp.title(entry).contains("Seeing Red"));
        assertEquals("Locked · 50 Slayer points",UnlockHelp.status(result));
        assertTrue(UnlockHelp.details(result).contains("Slayer master → Rewards → Unlock"));
        assertFalse(UnlockHelp.details(result).contains("flag"));
        assertFalse(UnlockHelp.details(result).contains("Source:"));
        assertEquals("Unlocked",UnlockHelp.status(new UnlockCatalog.Result(entry,UnlockCatalog.State.UNLOCKED,"")));
    }
    @Test public void historicalItemEvidenceIsNotPresentedAsCurrentOwnershipOrRawTimestamps()
    {
        UnlockCatalog.Facts facts=new UnlockCatalog.Facts();facts.live=true;
        UnlockCatalog.Result result=catalog.evaluate(catalog.byId.get("FIRE_CAPE"),facts,Collections.emptySet(),Collections.singletonMap("FIRE_CAPE",1700000000000L));
        assertEquals("Previously seen",UnlockHelp.status(result));
        assertFalse(UnlockHelp.details(result).contains("2023-"));
        assertFalse(UnlockHelp.details(result).contains("Source:"));
        assertTrue(UnlockHelp.details(result).contains("Fight Caves"));
    }
    @Test public void retiredSeparateMithrilTaskIsNotOfferedAsAPurchase()
    {
        assertFalse(catalog.byId.containsKey("SLAYER_MITHRIL"));
    }
}
