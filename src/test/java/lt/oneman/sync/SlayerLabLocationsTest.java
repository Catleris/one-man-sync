package lt.oneman.sync;

import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class SlayerLabLocationsTest
{
    @Test public void blueDragonsHaveTwoDistinctEntrances()
    {
        List<SlayerLabLocations.Destination> choices = SlayerLabLocations.forTask("Blue dragons", "");
        assertTrue(choices.size() >= 2);
        assertNotEquals(choices.get(0).entrance, choices.get(1).entrance);
        assertTrue(choices.get(1).access.contains("Heroes' Quest"));
    }
    @Test public void assignedAreaCannotRouteToAnotherDungeon()
    {
        List<SlayerLabLocations.Destination> choices = SlayerLabLocations.forTask("Dust devils", "the Catacombs of Kourend");
        assertEquals(1, choices.size());
        assertEquals("Catacombs of Kourend", choices.get(0).name);
        assertNull(SlayerLabLocations.forTask("Dust devils", "unsupported assigned dungeon").get(0).entrance);
    }
    @Test public void knownAreaAliasesAreAccepted()
    {
        assertEquals(1, SlayerLabLocations.forTask("Bloodvelds", "Stronghold Slayer Dungeon").size());
        assertEquals(1, SlayerLabLocations.forTask("Cave horrors", "Mos Le’Harmless Cave").size());
    }
    @Test public void unknownTasksAndUnmappedLocationsDoNotInventPins()
    {
        assertTrue(SlayerLabLocations.forTask("Unknown monster", "").isEmpty());
        assertFalse(SlayerLabLocations.forTask("Lizards", "").isEmpty());
        assertNull(SlayerLabLocations.forTask("Lizards", "").get(0).entrance);
        assertTrue(SlayerLabLocations.forTask("Blue dragons", "Wilderness Slayer Cave").get(0).wilderness);
    }
    @Test public void wildernessIsExplicitAndNotFirstChoice()
    {
        List<SlayerLabLocations.Destination> choices = SlayerLabLocations.forTask("Greater demons", "");
        assertFalse(choices.get(0).wilderness);
        assertTrue(choices.stream().anyMatch(d -> d.wilderness && d.toString().contains("WILDERNESS")));
    }
    @Test public void unrestrictedLizardmenExposeSourceLocations()
    {
        List<SlayerLabLocations.Destination> choices=SlayerLabLocations.forTask("Lizardmen", "");
        assertTrue(choices.stream().anyMatch(d->d.name.equalsIgnoreCase("Lizardmen Settlement")));
        assertTrue(choices.stream().anyMatch(d->d.name.equalsIgnoreCase("Lizardman canyon")));
        assertEquals(choices,SlayerLabLocations.forTask("Lizardman", ""));
        assertTrue(choices.stream().allMatch(d->d.entrance==null));
    }
    @Test public void everySourceLocationIsSelectableWithoutAnAssignedArea()
    {
        for(SlayerLabKnowledge.Entry entry:SlayerLabKnowledge.all())
            for(String place:entry.locations.split(";\\s*"))
                if(!place.trim().isEmpty() && !place.equalsIgnoreCase("N/A"))
                    assertTrue(entry.task+" / "+place,SlayerLabLocations.forTask(entry.task,"").stream().anyMatch(d->d.matches(place)));
    }
    @Test public void sourceFallbackKeepsAssignedAreaRestrictionAndWildWarning()
    {
        List<SlayerLabLocations.Destination> choices=SlayerLabLocations.forTask("Lizardmen","Lizardman canyon");
        assertEquals(1,choices.size());
        assertEquals("Lizardman canyon",choices.get(0).name);
        assertNull(choices.get(0).entrance);
        assertTrue(new SlayerLabLocations.Destination("Wilderness reference","No pin").wilderness);
        assertFalse(SlayerLabLocations.forTask("Hellhounds","").isEmpty());
        assertFalse(SlayerLabLocations.forTask("Dark beasts","").isEmpty());
    }
    @Test public void directionHandlesArrivalAndFloors()
    {
        WorldPoint player = new WorldPoint(3000, 3000, 0);
        assertEquals("NE", SlayerLabLocations.direction(player, new WorldPoint(3020, 3040, 0)));
        assertEquals("W", SlayerLabLocations.direction(player, new WorldPoint(2980, 3000, 0)));
        assertEquals("at entrance", SlayerLabLocations.direction(player, new WorldPoint(3003, 3002, 0)));
        assertEquals("different floor", SlayerLabLocations.direction(player, new WorldPoint(3000, 3000, 1)));
    }
    @Test public void mappedOptionsAreImmutable()
    {
        try
        {
            SlayerLabLocations.forTask("Blue dragons", "").clear();
            fail("Caller must not mutate shared destinations");
        }
        catch (UnsupportedOperationException expected) { }
    }
    @Test public void konarAssignmentSurvivesIncompleteMonsterCatalogue() {
        List<SlayerLabLocations.Destination> choices = SlayerLabLocations.forTask("Uncatalogued task", "the Karuulm Slayer Dungeon");
        assertEquals(1, choices.size());
        assertEquals("Karuulm Slayer Dungeon", choices.get(0).name);
        assertNotNull(choices.get(0).entrance);
        choices = SlayerLabLocations.forTask("Fire giants", "Isle of Souls Dungeon");
        assertEquals(new WorldPoint(2308,2919,0), choices.get(0).entrance);
    }
    @Test public void unmappedKonarAreaRemainsVisibleWithoutInventedPin() {
        List<SlayerLabLocations.Destination> choices = SlayerLabLocations.forTask("Fire giants", "Waterfall Dungeon");
        assertEquals(1, choices.size());
        assertEquals("Waterfall Dungeon", choices.get(0).name);
        assertNull(choices.get(0).entrance);
    }
}
