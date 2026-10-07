package lt.oneman.sync;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SlayerLabCatalogTest
{
    private static Set<String> items(String... names) { return new HashSet<>(Arrays.asList(names)); }
    @Test public void protectionMustBeWornAndHelmetVariantsWork()
    {
        SlayerLabCatalog.Requirement req = SlayerLabCatalog.find("Dust devils").requirements.get(0);
        assertEquals("EQUIP — in inventory", req.status(items("facemask"), items()));
        assertEquals("READY — equipped", req.status(items(), items("black slayer helmet (i)")));
        assertEquals("MISSING — not carried", req.status(items("black mask (i)"), items()));
    }
    @Test public void helmetDoesNotReplaceShieldOrNeckProtection()
    {
        for (String task : Arrays.asList("Basilisks", "Cockatrice", "Cave horrors"))
            assertEquals("MISSING — not carried", SlayerLabCatalog.find(task).requirements.get(0).status(items(), items("slayer helmet")));
    }
    @Test public void finishingToolAlternativesCount()
    {
        SlayerLabCatalog.Requirement req = SlayerLabCatalog.find("Gargoyles").requirements.get(0);
        assertEquals("READY — in inventory", req.status(items("rock hammer"), items()));
        assertEquals("READY — equipped", req.status(items(), items("granite hammer")));
    }
    @Test public void brineSabreMustBeEquipped()
    {
        SlayerLabCatalog.Requirement req = SlayerLabCatalog.find("Rockslugs").requirements.get(0);
        assertEquals("EQUIP — in inventory", req.status(items("brine sabre"), items()));
        assertEquals("READY — in inventory", req.status(items("brine sabre", "bag of salt"), items()));
    }
    @Test public void unknownTasksDoNotInventRequirements()
    {
        assertNull(SlayerLabCatalog.find("unverified monster"));
        assertNotNull(SlayerLabCatalog.find("  BLUE   DRAGONS "));
        assertTrue(SlayerLabCatalog.wiki("TzTok-Jad & other").contains("%26"));
    }
    @Test public void blueDragonNotesPreserveAlternativeRoutes()
    {
        SlayerLabCatalog.Guide guide = SlayerLabCatalog.find("Blue dragons");
        assertTrue(guide.locations.contains("dusty key"));
        assertTrue(guide.locations.contains("70 Agility"));
        assertTrue(guide.advice.contains("Baby blue dragons"));
        assertTrue(guide.requirements.isEmpty());
    }
    @Test public void everyGuideHasPreparationAndLocations()
    {
        assertEquals(25, SlayerLabCatalog.all().size());
        for (SlayerLabCatalog.Guide guide : SlayerLabCatalog.all())
        {
            assertFalse(guide.locations.isEmpty());
            assertFalse(guide.advice.isEmpty());
            assertTrue(guide.wikiUrl().startsWith("https://oldschool.runescape.wiki/"));
        }
    }
}
