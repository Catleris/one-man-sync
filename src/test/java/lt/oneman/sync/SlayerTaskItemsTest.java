package lt.oneman.sync;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SlayerTaskItemsTest
{
    private List<SlayerTaskItems.Row> rows(String task, Set<String> inv, Set<String> worn, Map<String,Integer> bank, boolean known)
    { return SlayerTaskItems.evaluate(SlayerTaskItems.rules(task,false),inv,worn,bank,known); }
    private Set<String> items(String... names) { return new HashSet<>(Arrays.asList(names)); }
    @Test public void standardTasksDoNotRequestGenericGear()
    {
        for(String task:Arrays.asList("Bloodvelds","Greater demons","Hellhounds","Abyssal demons","Kalphites","unknown",""))
            assertTrue(task,rows(task,items(),items(),Collections.emptyMap(),true).isEmpty());
    }
    @Test public void hammerDisappearsWhenAnyAlternativeIsCarried()
    {
        Map<String,Integer> bank=Collections.singletonMap("rock hammer",2);
        List<SlayerTaskItems.Row> missing=rows("Gargoyles",items(),items(),bank,true);
        assertEquals(SlayerTaskItems.Status.BANK,missing.get(0).status);
        assertEquals(SlayerTaskItems.MANDATORY,SlayerTaskItems.highlight("rock hammer",missing));
        for(String alternative:Arrays.asList("rock hammer","granite hammer","rock thrownhammer")) {
            assertNull(SlayerTaskItems.highlight("rock hammer",rows("Gargoyles",items(alternative),items(),bank,true)));
            assertNull(SlayerTaskItems.highlight("rock hammer",rows("Gargoyles",items(),items(alternative),bank,true)));
        }
    }
    @Test public void recolouredHelmetsReplaceOnlyTheirComponentProtection()
    {
        for(String task:Arrays.asList("Dust devils","Smoke devils","Banshees","Aberrant spectres","Wall beasts"))
            for(String helm:Arrays.asList("slayer helmet","slayer helmet (i)","black slayer helmet (i)","tzkal slayer helmet (i)")) {
                assertEquals(SlayerTaskItems.Status.CARRIED,rows(task,items(helm),items(),Collections.emptyMap(),true).get(0).status);
                assertEquals(SlayerTaskItems.Status.EQUIPPED,rows(task,items(),items(helm),Collections.emptyMap(),true).get(0).status);
            }
        for(String task:Arrays.asList("Basilisks","Cockatrice","Cave horrors"))
            assertEquals(SlayerTaskItems.Status.MISSING,rows(task,items("slayer helmet (i)"),items(),Collections.emptyMap(),true).get(0).status);
    }
    @Test public void unknownBankIsNotReportedAsMissingAndZeroQuantityDoesNotCount()
    {
        assertEquals(SlayerTaskItems.Status.UNKNOWN,rows("Gargoyles",items(),items(),Collections.emptyMap(),false).get(0).status);
        assertEquals(SlayerTaskItems.Status.MISSING,rows("Gargoyles",items(),items(),Collections.singletonMap("rock hammer",0),true).get(0).status);
    }
    @Test public void emptySprayDoesNotSatisfyFinishingRequirement()
    {
        for(String empty:Arrays.asList("fungicide spray 0","fungicide spray (0)","fungicide"))
            assertEquals(SlayerTaskItems.Status.MISSING,rows("Mutated zygomites",items(empty),items(),Collections.emptyMap(),true).get(0).status);
        for(String charged:Arrays.asList("fungicide spray 1","fungicide spray 10","fungicide spray"))
            assertEquals(SlayerTaskItems.Status.CARRIED,rows("Mutated zygomites",items(charged),items(),Collections.emptyMap(),true).get(0).status);
    }
    @Test public void brineSabreReplacesSaltAndSingularTasksResolve()
    {
        assertEquals(SlayerTaskItems.Status.EQUIPPED,rows("Rockslug",items(),items("brine sabre"),Collections.emptyMap(),true).get(0).status);
        assertFalse(rows("Gargoyle",items(),items(),Collections.emptyMap(),true).isEmpty());
        assertFalse(rows("Desert lizards",items(),items(),Collections.emptyMap(),true).isEmpty());
    }
    @Test public void bootsAreSuppressedByEliteDiary()
    {
        for(String task:Arrays.asList("Wyrms","Drakes","Hydras")) {
            assertTrue(SlayerTaskItems.rules(task,true).isEmpty());
            List<SlayerTaskItems.Row> rows=rows(task,items(),items(),Collections.singletonMap("boots of stone",1),true);
            assertEquals(SlayerTaskItems.UTILITY,SlayerTaskItems.highlight("boots of stone",rows));
            assertNull(SlayerTaskItems.highlight("boots of stone",rows(task,items("granite boots"),items(),Collections.emptyMap(),true)));
        }
    }
    @Test public void ancientWyvernRulesDoNotAcceptDragonfireShield()
    {
        assertEquals(SlayerTaskItems.Status.MISSING,rows("Fossil Island wyverns",items("dragonfire shield"),items(),Collections.emptyMap(),true).get(0).status);
        assertEquals(SlayerTaskItems.Status.CARRIED,rows("Skeletal wyverns",items("dragonfire shield"),items(),Collections.emptyMap(),true).get(0).status);
    }
    @Test public void unrelatedItemsNeverHighlight()
    {
        assertNull(SlayerTaskItems.highlight("shark",rows("Gargoyles",items(),items(),Collections.emptyMap(),true)));
        assertNull(SlayerTaskItems.highlight("rock hammer",Collections.emptyList()));
    }
}
