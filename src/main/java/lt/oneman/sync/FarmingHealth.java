package lt.oneman.sync;

import java.util.*;
import java.util.regex.*;
import net.runelite.client.plugins.timetracking.farming.CropState;

/** Observed states only: no remote disease prediction or inferred bucket consumption. */
final class FarmingHealth
{
    private final Set<String> unhealthy=new HashSet<>();
    private int bucketCount;
    private int uses=-1;
    private long reported;
    private boolean lowNotified;
    private static final Pattern CHECK=Pattern.compile("^Your bottomless compost bucket (?:is currently holding|has) (one|a single|[0-9,]+) (?:use|uses) of (?:regular ?|super ?|ultra ?)?compost(?: remaining)?[.!]$");
    private static final Pattern FILL=Pattern.compile("^You fill your bottomless compost bucket with .+\\. Your bottomless compost bucket now contains a total of ([0-9,]+) uses[.!]$");
    void reset() { unhealthy.clear();bucketCount=0;uses=-1;reported=0;lowNotified=false; }
    boolean observe(String patch, FarmingPatchDecoder.DecodedPatch state)
    {
        if(state==null) return false;
        boolean bad=state.getCropState()==CropState.DISEASED || state.getCropState()==CropState.DEAD;
        if(!bad) {unhealthy.remove(patch);return false;}
        return unhealthy.add(patch);
    }
    void buckets(int count)
    {
        if(count!=bucketCount) {uses=-1;reported=0;lowNotified=false;}
        bucketCount=count;
    }
    void empty(long now) { if(bucketCount==1 && uses!=0) {uses=0;reported=now;} }
    boolean message(String message, long now)
    {
        if(bucketCount!=1) return false;
        String clean=message.replaceAll("<[^>]*>", "");
        Matcher check=CHECK.matcher(clean), fill=FILL.matcher(clean);
        int value;
        if(check.matches()) value=check.group(1).equals("one") || check.group(1).equals("a single") ? 1 : number(check.group(1));
        else if(fill.matches()) value=number(fill.group(1));
        else if(clean.equals("Your bottomless compost bucket has run out of compost!") || clean.equals("You discard the contents of your bottomless compost bucket.")) value=0;
        else if(clean.equals("Your bottomless compost bucket is now full!")) value=10000;
        else return false;
        if(value<0 || value>10000) return false;
        uses=value;reported=now;
        return true;
    }
    private static int number(String value) { try {return Integer.parseInt(value.replace(",", ""));} catch(NumberFormatException e) {return -1;} }
    boolean low(int threshold) {return bucketCount==1 && uses>=0 && uses<=threshold;}
    boolean notifyLow(int threshold)
    {
        boolean low=low(threshold);
        if(!low) {lowNotified=false;return false;}
        if(lowNotified) return false;
        lowNotified=true;return true;
    }
    String bucketText(int threshold, long now)
    {
        if(bucketCount==0) return "";
        if(bucketCount>1) return "Bottomless bucket · multiple carried\nCheck one bucket at a time; quantities are not combined.";
        if(uses<0) return "Bottomless bucket · amount unknown\nUse its Check option before your farming run.";
        return "Bottomless bucket · "+uses+" last reported uses"+(low(threshold)?" — REFILL":"")
            +"\nReported "+Math.max(0,now-reported)/60+" min ago · Check again after use/refill.";
    }
}
