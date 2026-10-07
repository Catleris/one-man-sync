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
    private static boolean bool(JsonObject o,String key)
    { try { return o.has(key) && o.get(key).getAsBoolean(); } catch(RuntimeException e) { return false; } }
    private static String string(JsonObject o,String key)
    { try { return o.has(key)?o.get(key).getAsString():""; } catch(RuntimeException e) { return ""; } }
    private static int integer(JsonObject o,String key) { return (int)number(o,key); }
    private static long number(JsonObject o,String key)
    { try { return o.has(key)?o.get(key).getAsLong():0; } catch(RuntimeException e) { return 0; } }
}
