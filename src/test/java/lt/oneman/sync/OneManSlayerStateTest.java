package lt.oneman.sync;

import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class OneManSlayerStateTest
{
    private JsonObject state(int count,int target,long seen,boolean valid)
    {
        JsonObject o=new JsonObject();o.addProperty("taskName",valid?"Hydras":"");o.addProperty("amount",count);
        o.addProperty("initialAmount",100);o.addProperty("location","Karuulm");o.addProperty("rawTaskTarget",target);
        o.addProperty("rawTaskCount",count);o.addProperty("rawTaskArea",2);o.addProperty("taskSignalValid",valid);
        o.addProperty("lastTaskSeenEpochMs",seen);return o;
    }
    @Test public void completedTaskCannotBeResurrected()
    {
        JsonObject old=state(45,10,1000,true),clear=state(0,10,2000,true);
        assertEquals(0,OneManSlayerState.reconcile(clear,old,2000).get("amount").getAsInt());
    }
    @Test public void changedTargetOrAreaOrInitialCannotReuseLabel()
    {
        JsonObject old=state(45,10,1000,true);
        assertEquals("",OneManSlayerState.reconcile(state(44,11,0,false),old,2000).get("taskName").getAsString());
        JsonObject area=state(44,10,0,false);area.addProperty("rawTaskArea",3);
        assertEquals("",OneManSlayerState.reconcile(area,old,2000).get("taskName").getAsString());
        JsonObject initial=state(44,10,0,false);initial.addProperty("initialAmount",120);
        assertEquals("",OneManSlayerState.reconcile(initial,old,2000).get("taskName").getAsString());
    }
    @Test public void boundedFallbackUsesLiveQuantityAndFreshRewards()
    {
        JsonObject current=state(44,10,0,false);current.addProperty("streak",194);current.addProperty("points",0);
        JsonObject rewards=new JsonObject();rewards.addProperty("biggerAndBadder",true);current.add("rewardUnlocks",rewards);
        JsonObject old=state(45,10,1000,true);old.addProperty("streak",150);old.addProperty("points",500);
        JsonObject r=OneManSlayerState.reconcile(current,old,2000);
        assertEquals("Hydras",r.get("taskName").getAsString());assertEquals(44,r.get("amount").getAsInt());
        assertEquals(194,r.get("streak").getAsInt());assertEquals(0,r.get("points").getAsInt());
        assertTrue(r.getAsJsonObject("rewardUnlocks").get("biggerAndBadder").getAsBoolean());
        assertFalse(r.get("taskSignalValid").getAsBoolean());assertEquals(1000,r.get("lastTaskSeenEpochMs").getAsLong());
    }
    @Test public void repeatedFallbackDoesNotExtendLifetime()
    {
        JsonObject old=state(45,10,1000,true);
        JsonObject first=OneManSlayerState.reconcile(state(44,10,0,false),old,100000);
        assertEquals("Hydras",first.get("taskName").getAsString());
        JsonObject expired=OneManSlayerState.reconcile(state(43,10,0,false),first,121001);
        assertEquals("",expired.get("taskName").getAsString());
    }
    @Test public void confirmedNewAssignmentWinsAndIncreasedCountCannotReuse()
    {
        JsonObject old=state(45,10,1000,true),fresh=state(60,11,2000,true);fresh.addProperty("taskName","Drakes");
        assertEquals("Drakes",OneManSlayerState.reconcile(fresh,old,2000).get("taskName").getAsString());
        assertEquals("",OneManSlayerState.reconcile(state(60,10,0,false),old,2000).get("taskName").getAsString());
    }
}
