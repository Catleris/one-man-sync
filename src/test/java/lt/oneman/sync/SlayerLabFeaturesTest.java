package lt.oneman.sync;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SlayerLabFeaturesTest
{
    @Test public void coverageIncludesHighLevelTasksAndBossAlternatives()
    {
        assertEquals(158,SlayerLabKnowledge.all().size());
        assertEquals("95",SlayerLabKnowledge.find("Hydras").level);
        assertTrue(SlayerLabAdvice.variants("Hydras").contains("Alchemical Hydra"));
        assertEquals("92",SlayerLabKnowledge.find("Araxytes").level);
        for(SlayerLabKnowledge.Entry entry:SlayerLabKnowledge.all()) assertNotNull(SlayerLabCatalog.find(entry.task));
    }
    @Test public void npcMatchingIncludesBossAndSingularButRejectsOtherTasks()
    {
        assertTrue(SlayerLabKnowledge.find("Hellhounds").matchesNpc("Cerberus"));
        assertTrue(SlayerLabKnowledge.find("Blue dragons").matchesNpc("Baby blue dragon"));
        assertTrue(SlayerLabKnowledge.find("Hydras").matchesNpc("Alchemical Hydra"));
        assertFalse(SlayerLabKnowledge.find("Blue dragons").matchesNpc("Black dragon"));
    }
    @Test public void highLevelEntrancesRespectAssignedArea()
    {
        assertEquals("Karuulm Slayer Dungeon",SlayerLabLocations.forTask("Hydras","").get(0).name);
        assertTrue(SlayerLabLocations.forTask("Hydras","Slayer Tower").isEmpty());
        assertEquals(2,SlayerLabLocations.forTask("Basilisks","").size());
        assertEquals(1,SlayerLabLocations.forTask("Basilisks","Fremennik Slayer Dungeon").size());
    }
    @Test public void quantitiesCountFoodDosesAndAmmoWithoutConfusingCharges()
    {
        Map<String,Integer> items=new HashMap<>();
        items.put("shark",4); items.put("prayer potion(4)",2); items.put("slayer ring (8)",1);
        items.put("rune arrow",100);items.put("rune arrowtips",500);items.put("death rune",80);
        SlayerLabSupplies.Counts c=SlayerLabSupplies.count(items,Collections.singleton("shark"),Collections.singleton("prayer potion(4)"));
        assertEquals(4,c.food);assertEquals(8,c.doses);assertEquals(100,c.ammo);assertEquals(80,c.runes);
    }
    @Test public void zeroThresholdDisablesWarningAndStyleSelectsResource()
    {
        SlayerLabConfig off=new SlayerLabConfig(){@Override public int foodWarning(){return 0;}};
        SlayerLabSupplies.Counts c=new SlayerLabSupplies.Counts();
        assertEquals("",c.warnings(off));
        SlayerLabConfig magic=new SlayerLabConfig(){@Override public CombatStyle combatStyle(){return CombatStyle.MAGIC;}};
        assertTrue(c.warnings(magic).contains("RUNES LOW"));assertFalse(c.warnings(magic).contains("AMMO LOW"));
    }
    @Test public void sessionSeparatesTaskCreditsFromLootKillsAndEstimatesEta()
    {
        SlayerLabJournal j=new SlayerLabJournal();j.observe("Hydras","",20,20,1000);
        j.loot(Collections.singletonMap("hydra leather",1));j.xp(300);
        j.observe("Hydras","",16,20,121000);
        assertEquals(4,j.active.credits);assertEquals(1,j.active.kills);
        assertTrue(SlayerLabJournal.metrics(j.active,121000).contains("Estimated remaining: 8 min"));
        j.observe("Hydras","",0,20,122000);j.end("Completed",122000);
        assertEquals(20,j.history.get(0).credits); assertNull(j.active);
    }
    @Test public void newTaskAndRemainingIncreaseArchiveInsteadOfMerging()
    {
        SlayerLabJournal j=new SlayerLabJournal();j.observe("Hydras","",20,0,1000);j.active.note="setup A";
        j.observe("Hydras","",30,0,2000);
        assertEquals(1,j.history.size());assertEquals("setup A",j.history.get(0).note);assertEquals(0,j.active.credits);
        j.observe("Wyrms","",20,0,3000);assertEquals(2,j.history.size());
        for(int i=0;i<60;i++){j.end("Logout",4000+i);j.observe("Wyrms","",20,0,4000+i);}
        assertEquals(50,j.history.size());
    }
    @Test public void bankPreparationDistinguishesOwnedCarriedAndEquipped()
    {
        SlayerLabAccount a=new SlayerLabAccount();SlayerLabCatalog.Guide g=SlayerLabCatalog.find("Dust devils");
        assertTrue(a.preparation(g,Collections.emptySet(),Collections.emptySet(),1000).contains("bank unknown"));
        a.bank.seen=1000;a.bank.items.put("facemask",1);
        assertTrue(a.preparation(g,Collections.emptySet(),Collections.emptySet(),61000).contains("TAKE FROM BANK"));
        assertTrue(a.preparation(g,Collections.singleton("facemask"),Collections.emptySet(),61000).contains("EQUIP"));
        assertTrue(a.preparation(g,Collections.emptySet(),Collections.singleton("facemask"),61000).contains("READY"));
        assertTrue(a.bankAge(61000).contains("1 min ago"));
    }
    @Test public void travelDoesNotTreatEmptyRingOrUnverifiedQuestAsReady()
    {
        assertFalse(SlayerLabTravel.slayerRing(Collections.singleton("slayer ring (0)")));
        assertTrue(SlayerLabTravel.slayerRing(Collections.singleton("slayer ring (8)")));
        SlayerLabLocations.Destination d=SlayerLabLocations.forTask("Bloodvelds","Stronghold Slayer Cave").get(0);
        String blocked=SlayerLabTravel.route(d,Collections.singleton("royal seed pod"),Collections.emptySet());
        assertTrue(blocked.contains("verified Monkey Madness II"));
        assertTrue(SlayerLabTravel.route(d,Collections.singleton("royal seed pod"),Collections.singleton("Monkey Madness II")).contains("CARRIED + quest completed"));
    }
    @Test public void lootGoalsSeparateBankOwnershipFromThisSession()
    {
        SlayerLabAccount.Bank bank=new SlayerLabAccount.Bank();SlayerLabJournal j=new SlayerLabJournal();
        j.observe("Hydras","",10,0,1000);j.loot(Collections.singletonMap("hydra leather",1));
        String view=SlayerLabAdvice.loot("Hydras","Hydra leather",bank,j.active);
        assertTrue(view.contains("Bank snapshot count: unknown"));assertTrue(view.contains("Observed this session: 1"));
        bank.seen=1000;bank.items.put("hydra leather",2);
        assertTrue(SlayerLabAdvice.loot("Hydras","Hydra leather",bank,j.active).contains("Bank snapshot count: 2"));
    }
    @Test public void lateInitialAmountIsRecordedAndChangedAssignmentStartsFresh()
    {
        SlayerLabJournal j=new SlayerLabJournal(); j.observe("Hydras","",20,0,1000);
        j.observe("Hydras","",19,25,2000); assertEquals(25,j.active.initial);
        j.observe("Hydras","",18,30,3000); assertEquals(1,j.history.size()); assertEquals(0,j.active.credits);
    }
    @Test public void recoveredSessionCanStopAtLastObservationWithoutOfflineTime()
    {
        SlayerLabJournal j=new SlayerLabJournal();j.observe("Hydras","",20,20,1000);
        j.observe("Hydras","",19,20,61000);
        j.end("Recovered",j.active.updated);
        assertEquals(60000,j.history.get(0).duration(99999999));
    }
}
