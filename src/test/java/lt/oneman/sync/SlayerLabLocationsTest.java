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
        assertEquals(2, choices.size());
        assertNotEquals(choices.get(0).entrance, choices.get(1).entrance);
        assertTrue(choices.get(1).access.contains("Heroes' Quest"));
    }
    @Test public void assignedAreaCannotRouteToAnotherDungeon()
    {
        List<SlayerLabLocations.Destination> choices = SlayerLabLocations.forTask("Dust devils", "the Catacombs of Kourend");
        assertEquals(1, choices.size());
        assertEquals("Catacombs of Kourend", choices.get(0).name);
        assertTrue(SlayerLabLocations.forTask("Dust devils", "unsupported assigned dungeon").isEmpty());
    }
    @Test public void knownAreaAliasesAreAccepted()
    {
        assertEquals(1, SlayerLabLocations.forTask("Bloodvelds", "Stronghold Slayer Dungeon").size());
        assertEquals(1, SlayerLabLocations.forTask("Cave horrors", "Mos Le’Harmless Cave").size());
    }
    @Test public void unknownTasksAndUnmappedLocationsDoNotInventPins()
    {
        assertTrue(SlayerLabLocations.forTask("Unknown monster", "").isEmpty());
        assertTrue(SlayerLabLocations.forTask("Lizards", "").isEmpty());
        assertTrue(SlayerLabLocations.forTask("Blue dragons", "Wilderness Slayer Cave").isEmpty());
    }
    @Test public void wildernessIsExplicitAndNotFirstChoice()
    {
        List<SlayerLabLocations.Destination> choices = SlayerLabLocations.forTask("Greater demons", "");
        assertFalse(choices.get(0).wilderness);
        assertTrue(choices.stream().anyMatch(d -> d.wilderness && d.toString().contains("WILDERNESS")));
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
}
