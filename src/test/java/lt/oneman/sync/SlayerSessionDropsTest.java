package lt.oneman.sync;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SlayerSessionDropsTest {
    @Test public void startingOrWaitingForSpawnDoesNotCountKills() {
        SlayerSessionDrops session=new SlayerSessionDrops();session.start(1000);
        assertEquals(0,session.superiorKills());assertEquals(0,session.notableDrops());
    }
    @Test public void onlySuperiorLootCountsAsSuperiorAndWorksAfterTaskCompletion() {
        SlayerSessionDrops session=new SlayerSessionDrops();
        assertTrue(session.loot("1","Marble gargoyle",Collections.emptyMap(),false,1000));
        assertEquals(1,session.superiorKills());
        session.loot("2","Gargoyle",Collections.singletonMap("Granite maul",1),true,2000);
        assertEquals(1,session.superiorKills());assertEquals(1,session.notableDrops());
        assertFalse(SlayerSessionDrops.superior("Basilisk Knight"));
        assertFalse(SlayerSessionDrops.superior("Thermonuclear smoke devil"));
        assertTrue(SlayerSessionDrops.superior("INSATIABLE MUTATED BLOODVELD"));
    }
    @Test public void duplicateAttributionDoesNotDoubleCountDropsOrKills() {
        SlayerSessionDrops session=new SlayerSessionDrops();
        Map<String,Integer> loot=Collections.singletonMap("Imbued heart",1);
        assertTrue(session.loot("npc:tick","Choke devil",loot,true,1000));
        assertFalse(session.loot("npc:tick","Choke devil",loot,true,1000));
        assertEquals(1,session.superiorKills());assertEquals(1,session.notableDrops());
    }
    @Test public void unrelatedOffTaskLootAndCommonItemsAreExcluded() {
        SlayerSessionDrops session=new SlayerSessionDrops();
        assertFalse(session.loot("1","Cerberus",Collections.singletonMap("Eternal crystal",1),false,1000));
        session.loot("2","Gargoyle",Collections.singletonMap("Coins",10000),true,1000);
        session.loot("3","Gargoyle",Collections.singletonMap("Granite maul",0),true,1000);
        assertEquals(0,session.notableDrops());assertTrue(session.recent().isEmpty());
    }
    @Test public void notableDropsPreserveSourceQuantityAndAreNotReportedAsPickedUp() {
        SlayerSessionDrops session=new SlayerSessionDrops();
        session.loot("1","Cerberus",Collections.singletonMap("Eternal crystal",2),true,1000);
        assertEquals(2,session.recent().get(0).quantity);
        assertEquals("Cerberus",session.recent().get(0).source);
        assertTrue(session.summary(61000).contains("pickup not verified"));
        assertTrue(session.summary(61000).contains("0h 1m"));
    }
    @Test public void recentHistoryIsBoundedNewestFirstAndImmutable() {
        SlayerSessionDrops session=new SlayerSessionDrops();
        for(int i=1;i<=7;i++) session.loot(""+i,"King kurask",Collections.singletonMap("Eternal gem",i),true,1000L*i);
        assertEquals(7,session.superiorKills());assertEquals(7,session.notableDrops());
        assertEquals(5,session.recent().size());assertEquals(7,session.recent().get(0).quantity);
        try {session.recent().clear();fail("Must be immutable");}catch(UnsupportedOperationException expected) {}
    }
    @Test public void taskChangesDoNotResetSessionButExplicitResetDoes() {
        SlayerSessionDrops session=new SlayerSessionDrops();session.start(1000);
        session.loot("1","Marble gargoyle",Collections.singletonMap("Imbued heart",1),true,2000);
        session.start(3000);
        session.loot("2","Night beast",Collections.emptyMap(),true,4000);
        assertEquals(2,session.superiorKills());assertTrue(session.summary(61000).contains("0h 1m"));
        session.reset(61000);assertEquals(0,session.superiorKills());assertTrue(session.recent().isEmpty());
        session.reset(0);assertTrue(session.summary(62000).startsWith("Waiting"));
    }
}
