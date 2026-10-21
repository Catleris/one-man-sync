package lt.oneman.sync;

import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;

public class UnlockCatalogTest
{
    private final UnlockCatalog catalog=new UnlockCatalog();
    private UnlockCatalog.State state(String id,UnlockCatalog.Facts facts) {
        return catalog.evaluate(catalog.byId.get(id),facts,Collections.emptySet(),Collections.emptyMap()).state;
    }
    @Test public void comprehensiveCatalogIsUniqueAndEveryQuestHasItsOwnJournalEntry()
    {
        assertEquals(catalog.entries.size(),catalog.byId.size());
        assertTrue(catalog.entries.size()>400);
        for(Quest quest:Quest.values())assertTrue(catalog.byId.containsKey("QUEST_"+quest.name()));
        assertTrue(catalog.entries.stream().filter(e->"ITEM".equals(e.mode)).count()>80);
        assertEquals(48,catalog.entries.stream().filter(e->"DIARY".equals(e.mode)).count());
    }
    @Test public void questCompletionDoesNotProveItemOwnershipAndHistoricalObservationIsSeparate()
    {
        UnlockCatalog.Facts facts=new UnlockCatalog.Facts();facts.live=true;
        facts.quests.put(Quest.RECIPE_FOR_DISASTER__CULINAROMANCER,QuestState.FINISHED);
        assertEquals(UnlockCatalog.State.READY,state("BARROWS_GLOVES",facts));
        UnlockCatalog.Result result=catalog.evaluate(catalog.byId.get("BARROWS_GLOVES"),facts,Collections.emptySet(),Collections.singletonMap("BARROWS_GLOVES",1700000000000L));
        assertEquals(UnlockCatalog.State.OBSERVED,result.state);
        assertTrue(result.detail.contains("not proof of current ownership"));
        assertEquals(UnlockCatalog.State.MANUAL,catalog.evaluate(catalog.byId.get("BARROWS_GLOVES"),facts,Collections.singleton("BARROWS_GLOVES"),Collections.emptyMap()).state);
    }
    @Test public void fairyPermissionCanBeUnlockedBeforeFinishingTheQuest()
    {
        UnlockCatalog.Facts facts=new UnlockCatalog.Facts();facts.live=true;
        facts.quests.put(Quest.FAIRYTALE_II__CURE_A_QUEEN,QuestState.IN_PROGRESS);
        facts.bits.put(VarbitID.FAIRYRING_PERMISSION,1);
        assertEquals(UnlockCatalog.State.UNLOCKED,state("FAIRY_RINGS",facts));
        assertEquals(UnlockCatalog.State.STARTED,state("QUEST_FAIRYTALE_II__CURE_A_QUEEN",facts));
    }
    @Test public void unknownSignalsAndLogoutNeverBecomeFalseCompletionOrCrossAccountNotes()
    {
        UnlockCatalog.Facts facts=new UnlockCatalog.Facts();facts.live=true;
        assertEquals(UnlockCatalog.State.UNKNOWN,state("ANCIENTS",facts));
        assertEquals(UnlockCatalog.State.UNKNOWN,state("RIGOUR",facts));
        facts.live=false;
        assertEquals(UnlockCatalog.State.UNKNOWN,catalog.evaluate(catalog.byId.get("GRACEFUL"),facts,Collections.singleton("GRACEFUL"),Collections.emptyMap()).state);
    }
    @Test public void skillGateAndManualConfirmationRemainDistinct()
    {
        UnlockCatalog.Facts facts=new UnlockCatalog.Facts();facts.live=true;
        facts.skills.put(Skill.FARMING,44);
        assertEquals(UnlockCatalog.State.PENDING,state("FARM_GUILD_45",facts));
        facts.skills.put(Skill.FARMING,45);
        assertEquals(UnlockCatalog.State.READY,state("FARM_GUILD_45",facts));
        facts.quests.put(Quest.DESERT_TREASURE_I,QuestState.FINISHED);
        assertEquals(UnlockCatalog.State.UNLOCKED,state("ANCIENTS",facts));
    }
    @Test public void diaryLegacyValuesAndRewardClaimStatusesAreNotInterchangeable()
    {
        UnlockCatalog.Facts facts=new UnlockCatalog.Facts();facts.live=true;
        UnlockCatalog.Entry karamja=catalog.byId.get("DIARY_Karamja_Easy");
        facts.bits.put(karamja.varbit,1);
        assertEquals(UnlockCatalog.State.PENDING,state(karamja.id,facts));
        facts.bits.put(karamja.varbit,2);
        assertEquals(UnlockCatalog.State.UNLOCKED,state(karamja.id,facts));
        facts.bits.put(VarbitID.CA_TIER_STATUS_EASY,1);
        assertEquals(UnlockCatalog.State.READY,state("CA_Easy",facts));
        facts.bits.put(VarbitID.CA_TIER_STATUS_EASY,2);
        assertEquals(UnlockCatalog.State.UNLOCKED,state("CA_Easy",facts));
    }
    @Test public void chargedItemMatchingNeverTreatsOneGracefulPieceAsAFullSet()
    {
        assertTrue(catalog.matchingItems("Black mask (10)").stream().anyMatch(e->e.id.equals("BLACK_MASK")));
        assertFalse(catalog.matchingItems("Graceful hood").stream().anyMatch(e->e.id.equals("GRACEFUL")));
        assertTrue(catalog.matchingItems("Barrows gloves").stream().anyMatch(e->e.id.equals("BARROWS_GLOVES")));
        assertTrue(catalog.matchingItems("Dragon defender ornament kit").isEmpty());
    }
}
