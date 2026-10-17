package lt.oneman.sync;

import com.google.gson.JsonObject;

/** Bounded label fallback; live counts, rewards and points always win. */
final class OneManSlayerState
{
    static final long LABEL_GRACE_MS=120000;
    static JsonObject reconcile(JsonObject current,JsonObject previous,long now)
    {
        if(previous==null || bool(current,"taskSignalValid")) return current;
        int target=integer(current,"rawTaskTarget");
        int count=integer(current,"rawTaskCount");
        long seen=number(previous,"lastTaskSeenEpochMs");
        boolean same=target>0 && target==integer(previous,"rawTaskTarget")
            && integer(current,"rawTaskArea")==integer(previous,"rawTaskArea")
            && integer(current,"initialAmount")==integer(previous,"initialAmount");
        boolean recent=seen>0 && now>=seen && now-seen<=LABEL_GRACE_MS;
        if(count>0 && count<=integer(previous,"amount") && same && recent
            && !string(previous,"taskName").isEmpty())
        {
            current.addProperty("taskName",string(previous,"taskName"));
            current.addProperty("location",string(previous,"location"));
            current.addProperty("amount",count);
            current.addProperty("taskSource","last-known");
            current.addProperty("taskStatus","last-known");
            current.addProperty("lastTaskSeenEpochMs",seen);
        }
        return current;
    }
    static final class LocalTask
    {
        final String name, location;
        final int remaining, initial;
        LocalTask(String name, String location, int remaining, int initial)
        { this.name=name; this.location=location; this.remaining=remaining; this.initial=initial; }
        boolean known() { return !name.isEmpty(); }
    }
    /** Only confirmed/reconciled labels with the current live count reach local tools. */
    static LocalTask localTask(JsonObject state,int liveCount)
    {
        if(state==null || liveCount<=0) return new LocalTask("","",Math.max(0,liveCount),0);
        boolean verified=bool(state,"taskSignalValid") || "last-known".equals(string(state,"taskStatus"));
        String name=verified && integer(state,"amount")==liveCount ? string(state,"taskName").trim() : "";
        if(integer(state,"rawTaskArea")>0 && string(state,"location").trim().isEmpty()) name="";
        return new LocalTask(name,name.isEmpty()?"":string(state,"location"),liveCount,
            name.isEmpty()?0:Math.max(liveCount,integer(state,"initialAmount")));
    }
    private static boolean bool(JsonObject o,String key)
    { try { return o.has(key) && o.get(key).getAsBoolean(); } catch(RuntimeException e) { return false; } }
    private static String string(JsonObject o,String key)
    { try { return o.has(key)?o.get(key).getAsString():""; } catch(RuntimeException e) { return ""; } }
    private static int integer(JsonObject o,String key) { return (int)number(o,key); }
    private static long number(JsonObject o,String key)
    { try { return o.has(key)?o.get(key).getAsLong():0; } catch(RuntimeException e) { return 0; } }
}
