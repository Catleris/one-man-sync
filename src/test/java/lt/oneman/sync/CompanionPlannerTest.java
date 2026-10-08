package lt.oneman.sync;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class CompanionPlannerTest {
    private CompanionPlanner.Step step(String id,String type,int rank){CompanionPlanner.Step s=new CompanionPlanner.Step();s.id=id;s.name=id;s.type=type;s.rank=rank;return s;}
    private CompanionPlanner planner(CompanionPlanner.Step...nodes){CompanionPlanner.Catalog c=new CompanionPlanner.Catalog();c.nodes=Arrays.asList(nodes);return new CompanionPlanner(c);}
    @Test public void usesActualLevelAndDoesNotInventQuestXp(){
        CompanionPlanner.Step quest=step("QUEST","quest",1);quest.skills.put("Agility",25);
        List<CompanionPlanner.Step> p=planner(quest).plan(Collections.singletonMap("Agility",1),Collections.emptySet(),Collections.emptySet(),0);
        assertEquals("TRAIN_Agility_25",p.get(0).id);
        assertTrue(planner(quest).requirements(p.get(0),Collections.singletonMap("Agility",1),Collections.emptySet(),0).contains("1 / 25"));
    }
    @Test public void unknownSkillIsNotCompleted(){CompanionPlanner.Step s=step("TRAIN","skill",1);s.autoSkills=true;s.skills.put("Agility",5);assertEquals(1,planner(s).plan(Collections.emptyMap(),Collections.emptySet(),Collections.emptySet(),0).size());}
    @Test public void completedQuestAndConfirmedUnlockAreSkipped(){
        assertTrue(planner(step("Q","quest",1),step("U","unlock",2)).plan(Collections.emptyMap(),Collections.singleton("Q"),Collections.singleton("U"),0).isEmpty());
    }
    @Test public void prerequisitesPrecedeQuestAndCyclesTerminate(){
        CompanionPlanner.Step a=step("A","quest",1),b=step("B","quest",2);a.quests.add("B");b.quests.add("A");
        List<CompanionPlanner.Step> p=planner(a,b).plan(Collections.emptyMap(),Collections.emptySet(),Collections.emptySet(),0);assertEquals(2,p.size());assertEquals("B",p.get(0).id);
    }
    @Test public void supplyQuantityAndExactNamesMatter(){CompanionPlanner.Step s=step("DORICS_QUEST","quest",1);assertEquals(6,CompanionSupplies.quantity(s,"Clay"));assertFalse(CompanionSupplies.matches("Iron bar","Iron ore"));assertTrue(CompanionSupplies.matches("Rune pickaxe","Pickaxe"));}
    @Test public void bundledFreshAccountRouteStartsWithActualStarterGoals() throws Exception {
        com.google.gson.Gson gson=new com.google.gson.Gson();
        try(java.io.Reader reader=new java.io.InputStreamReader(getClass().getResourceAsStream("efficiency-catalog.json"),java.nio.charset.StandardCharsets.UTF_8)) {
            CompanionPlanner p=new CompanionPlanner(gson.fromJson(reader,CompanionPlanner.Catalog.class));
            Map<String,Integer> levels=new HashMap<>();levels.put("Thieving",1);levels.put("Agility",1);levels.put("Hitpoints",10);
            java.util.List<CompanionPlanner.Step> route=p.plan(levels,Collections.emptySet(),Collections.emptySet(),0);
            assertEquals("STARTER_SUPPLIES",route.get(0).id);assertEquals("STARTER_THIEVING",route.get(1).id);assertEquals("X_MARKS_THE_SPOT",route.get(2).id);
            assertTrue(p.requirements(route.get(1),levels,Collections.emptySet(),0).contains("1 / 5"));
            assertFalse(route.stream().anyMatch(n->n.id.startsWith("MAX_")));
            for(CompanionPlanner.Step n:route)if(n.skills.containsKey("Agility"))assertTrue(p.requirements(n,levels,Collections.emptySet(),0).contains("Agility: 1 /"));
        }
    }
    @Test public void maxingIsNotNextStep(){assertTrue(planner(step("MAX_MINING","skill",600)).plan(Collections.emptyMap(),Collections.emptySet(),Collections.emptySet(),0).isEmpty());}
}
