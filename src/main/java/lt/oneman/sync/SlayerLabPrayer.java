package lt.oneman.sync;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

/** Own resource state only; no NPC, attack or projectile input. */
final class SlayerLabPrayer
{
    private enum ActivePrayer
    {
        THICK_SKIN(VarbitID.PRAYER_THICKSKIN),
        BURST_OF_STRENGTH(VarbitID.PRAYER_BURSTOFSTRENGTH),
        CLARITY_OF_THOUGHT(VarbitID.PRAYER_CLARITYOFTHOUGHT),
        SHARP_EYE(VarbitID.PRAYER_SHARPEYE),
        MYSTIC_WILL(VarbitID.PRAYER_MYSTICWILL),
        ROCK_SKIN(VarbitID.PRAYER_ROCKSKIN),
        SUPERHUMAN_STRENGTH(VarbitID.PRAYER_SUPERHUMANSTRENGTH),
        IMPROVED_REFLEXES(VarbitID.PRAYER_IMPROVEDREFLEXES),
        RAPID_RESTORE(VarbitID.PRAYER_RAPIDRESTORE),
        RAPID_HEAL(VarbitID.PRAYER_RAPIDHEAL),
        PROTECT_ITEM(VarbitID.PRAYER_PROTECTITEM),
        HAWK_EYE(VarbitID.PRAYER_HAWKEYE),
        MYSTIC_LORE(VarbitID.PRAYER_MYSTICLORE),
        STEEL_SKIN(VarbitID.PRAYER_STEELSKIN),
        ULTIMATE_STRENGTH(VarbitID.PRAYER_ULTIMATESTRENGTH),
        INCREDIBLE_REFLEXES(VarbitID.PRAYER_INCREDIBLEREFLEXES),
        PROTECT_FROM_MAGIC(VarbitID.PRAYER_PROTECTFROMMAGIC),
        PROTECT_FROM_MISSILES(VarbitID.PRAYER_PROTECTFROMMISSILES),
        PROTECT_FROM_MELEE(VarbitID.PRAYER_PROTECTFROMMELEE),
        EAGLE_EYE(VarbitID.PRAYER_EAGLEEYE),
        MYSTIC_MIGHT(VarbitID.PRAYER_MYSTICMIGHT),
        RETRIBUTION(VarbitID.PRAYER_RETRIBUTION),
        REDEMPTION(VarbitID.PRAYER_REDEMPTION),
        SMITE(VarbitID.PRAYER_SMITE),
        CHIVALRY(VarbitID.PRAYER_CHIVALRY),
        DEADEYE(VarbitID.PRAYER_DEADEYE),
        MYSTIC_VIGOUR(VarbitID.PRAYER_MYSTICVIGOUR),
        PIETY(VarbitID.PRAYER_PIETY),
        PRESERVE(VarbitID.PRAYER_PRESERVE),
        RIGOUR(VarbitID.PRAYER_RIGOUR),
        AUGURY(VarbitID.PRAYER_AUGURY);
        final int varbit;
        ActivePrayer(int varbit) { this.varbit=varbit; }
    }
    static List<String> active(Client client)
    {
        List<String> result=new ArrayList<>();
        for(ActivePrayer prayer:ActivePrayer.values())
            if(client.getVarbitValue(prayer.varbit)!=0) result.add(prayer.name().replace('_',' '));
        return result;
    }
    static final int WINDOW_TICKS = 50, MIN_TICKS = 20;
    private static final Pattern POTION = Pattern.compile("^(prayer potion|prayer mix|super restore|super restore mix)\\s*\\(([1-4])\\)$");
    static final class Doses
    {
        int prayer, restore;
        int total() { return prayer + restore; }
    }
    static Doses doses(Map<String,Integer> items)
    {
        Doses result = new Doses();
        for(Map.Entry<String,Integer> item:items.entrySet())
        {
            if(item.getValue()<=0) continue;
            Matcher match=POTION.matcher(SlayerLabKnowledge.normalize(item.getKey()));
            if(!match.matches()) continue;
            int count=Integer.parseInt(match.group(2))*item.getValue();
            if(match.group(1).startsWith("prayer")) result.prayer+=count;
            else result.restore+=count;
        }
        return result;
    }
    private final Deque<Integer> losses = new ArrayDeque<>();
    private int previous=-1, lost;
    private String signature="";
    private boolean pointsLow, dosesLow;
    void reset()
    {
        resetEstimate(); pointsLow=false; dosesLow=false;
    }
    void resetEstimate()
    {
        losses.clear(); lost=0; previous=-1; signature="";
    }
    void observe(int points, String activeSignature)
    {
        if(points<0) { resetEstimate(); return; }
        if(!Objects.equals(signature,activeSignature) || (previous>=0 && points>previous)) resetEstimate();
        signature=activeSignature;
        if(activeSignature.isEmpty()) { losses.clear(); lost=0; previous=points; return; }
        if(previous>=0)
        {
            int loss=Math.max(0,previous-points); losses.addLast(loss); lost+=loss;
            if(losses.size()>WINDOW_TICKS) lost-=losses.removeFirst();
        }
        previous=points;
    }
    double remainingSeconds(int points)
    {
        if(points<=0) return 0;
        if(signature.isEmpty() || losses.size()<MIN_TICKS || lost<2) return Double.NaN;
        return points*losses.size()*0.6/lost;
    }
    List<String> warnings(int points, Doses doses, SlayerLabConfig config)
    {
        List<String> out=new ArrayList<>();
        if(config.prayerPointsWarning()>0 && points<config.prayerPointsWarning()) out.add("PRAYER POINTS LOW — below "+config.prayerPointsWarning());
        if(config.prayerDosesWarning()>0 && doses.total()<config.prayerDosesWarning()) out.add("PRAYER / RESTORE DOSES LOW — below "+config.prayerDosesWarning());
        return out;
    }
    List<String> crossed(int points, Doses doses, SlayerLabConfig config)
    {
        boolean newPoints=config.prayerPointsWarning()>0 && points<config.prayerPointsWarning();
        boolean newDoses=config.prayerDosesWarning()>0 && doses.total()<config.prayerDosesWarning();
        List<String> result=new ArrayList<>();
        if(newPoints && !pointsLow) result.add("Prayer points below "+config.prayerPointsWarning());
        if(newDoses && !dosesLow) result.add("Prayer / restore doses below "+config.prayerDosesWarning());
        pointsLow=newPoints; dosesLow=newDoses;
        return result;
    }
    String describe(int points,int level,List<String> active,Doses doses,SlayerLabConfig config)
    {
        List<String> warnings=warnings(points,doses,config);
        double seconds=remainingSeconds(points);
        String estimate=points==0?"empty":active.isEmpty()?"no active prayers":Double.isNaN(seconds)?"collecting stable usage (at least 12 seconds and 2 points lost)":String.format(Locale.ROOT,"~%dm %02ds",(int)seconds/60,(int)seconds%60);
        return "PRAYER RESOURCE MONITOR\n\nPoints: "+points+" / "+level
            +"\n\n"+(warnings.isEmpty()?"No chosen warning threshold crossed.":String.join("\n",warnings))
            +"\n\nPrayer potion / mix doses: "+doses.prayer+"\nSuper restore / mix doses: "+doses.restore
            +"\nCombined carried doses: "+doses.total()
            +"\n\nEstimated time until empty: "+estimate
            +"\n\nACTIVE NOW\n"+(active.isEmpty()?"None":String.join("\n",active))
            +"\n\nThis lists prayers you have already enabled. It does not choose prayers or inspect enemy attacks."
            +"\n\nTime uses observed point losses over up to 30 seconds of stable usage (0.6 s/game tick). Changing prayers, equipment or restoring points resets the sample. External drains, flicking, regeneration and lag can make estimates unreliable."
            +"\n\nDoses are carried unnoted Prayer potions, Super restores and their mixes only. Sanfew serum, blighted potions, bank items and other restoration sources are not included. Doses are not equivalent restoration amounts."
            +"\n\nWarning limits and optional desktop alerts: OneMan Sync settings. This monitor works without a Slayer assignment.";
    }
}
