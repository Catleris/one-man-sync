package lt.oneman.sync;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class CompanionFeedbackTest {
    @Test public void singularBloodveldUsesRealTaskLocationsAndRules(){
        List<SlayerLabLocations.Destination> a=SlayerLabLocations.forTask("Bloodveld","");
        assertEquals(SlayerLabLocations.forTask("Bloodvelds","").size(),a.size());assertTrue(a.size()>=5);
        assertEquals("50",SlayerLabKnowledge.find("Bloodveld").level);
        assertTrue(SlayerLabCatalog.find("Bloodveld").advice.contains("50 Slayer"));
        SlayerLabLocations.Destination stronghold=a.stream().filter(d->d.name.equals("Stronghold Slayer Cave")).findFirst().get();
        assertTrue(SlayerLabAdvice.comparison(stronghold).contains("Cannon: ALLOWED"));
        assertTrue(SlayerLabAdvice.comparison(a.stream().filter(d->d.name.equals("Slayer Tower")).findFirst().get()).contains("NOT ALLOWED"));
        assertEquals(1,SlayerLabLocations.forTask("Bloodveld","Stronghold Slayer Dungeon").size());
        assertTrue(SlayerLabLocations.forTask("Bloodveld","Unverified area").isEmpty());
        assertNull(SlayerLabLocations.forTask("Bloodveld","Meiyerditch").get(0).entrance);
    }
    @Test public void chronicleOwnershipExcludesPlaceholdersAndOtherBooks(){
        Map<String,Integer> empty=Collections.emptyMap();
        assertTrue(CompanionSupplies.hasChronicle(Collections.singletonMap("Chronicle",1),empty,empty));
        assertTrue(CompanionSupplies.hasChronicle(empty,empty,Collections.singletonMap("chronicle",1)));
        assertFalse(CompanionSupplies.hasChronicle(empty,empty,Collections.singletonMap("chronicle",0)));
        assertFalse(CompanionSupplies.hasChronicle(Collections.singletonMap("Book of law",1),empty,empty));
    }
    @Test public void slayerGoalDescribesActualGapTaskAndAccessibleMasters(){
        CompanionPlanner.Step s=new CompanionPlanner.Step();s.type="training";s.skills.put("Slayer",60);
        SlayerLabJournal.Session task=new SlayerLabJournal.Session();task.task="Bloodveld";task.remaining=90;
        String p=CompanionTraining.describe(s,Collections.singletonMap("Slayer",56),Collections.emptySet(),70,"IRONMAN",task,190000,Collections.emptyMap());
        assertTrue(p.contains("56 / 60"));assertTrue(p.contains("Levels remaining: 4"));assertTrue(p.contains("Bloodveld — 90 remaining"));assertFalse(p.contains("Duradel"));assertFalse(p.contains("Chaeldar"));
        assertTrue(CompanionTraining.masters(100,56,Collections.singleton("SHILO_VILLAGE")).get(0).contains("Duradel"));
        assertFalse(CompanionTraining.masters(100,49,Collections.singleton("SHILO_VILLAGE")).get(0).contains("Duradel"));
    }
    @Test public void selectedGoalSurvivesRouteRebuildAndShowsOnlyItsPrerequisites(){
        CompanionPlanner.Catalog c=new CompanionPlanner.Catalog();c.nodes=Collections.emptyList();c.methods=Collections.singletonMap("Slayer",Arrays.asList("LT","EN"));
        CompanionPlanner p=new CompanionPlanner(c);CompanionPlanner.Step s=p.trainingFromId("TRAIN_Slayer_60");assertNotNull(s);
        assertEquals(1,p.forGoal(s,Collections.singletonMap("Slayer",56),Collections.emptySet(),Collections.emptySet(),0).size());
        assertNull(p.trainingFromId("TRAIN_Fake_60"));assertNull(p.trainingFromId("TRAIN_Slayer_999"));
        assertTrue(CompanionTraining.achieved(s,Collections.singletonMap("Slayer",60),Collections.emptySet(),Collections.emptySet()));
    }
    @Test public void syncDiagnosticIsBoundedAndRedactsCredentials(){
        com.google.gson.Gson g=new com.google.gson.Gson();
        assertTrue(CompanionSyncError.describe(g,"{\"error\":\"Sync DB klaida: missing column\"}").contains("missing column"));
        assertFalse(CompanionSyncError.describe(g,"{\"error\":\"Bearer secret-value\"}").contains("secret-value"));
        assertEquals("",CompanionSyncError.describe(g,"<html>password=secret</html>"));
    }
}
