package lt.oneman.sync;
import java.util.*;
/** UTC-day observed progress only; reconnects never count offline XP. Saved per RS profile. */
final class CompanionDay {
    String day="";
    long xp;
    Map<String,Integer> gainedLevels=new LinkedHashMap<>(),lastLevels=new LinkedHashMap<>();
    Set<String> quests=new LinkedHashSet<>();
    private transient boolean baseline;
    private transient long lastXp;
    private transient Set<String> previousQuests=new HashSet<>();
    void resume(){baseline=false;}
    void observe(String utcDay,Map<String,Integer> levels,Set<String> done,long totalXp) {
        if(!utcDay.equals(day)) {day=utcDay;xp=0;gainedLevels.clear();lastLevels.clear();quests.clear();baseline=false;}
        if(baseline) {
            xp+=Math.max(0,totalXp-lastXp);
            for(String q:done)if(!previousQuests.contains(q))quests.add(q);
            levels.forEach((k,v)->{int delta=Math.max(0,v-lastLevels.getOrDefault(k,v));if(delta>0)gainedLevels.merge(k,delta,Integer::sum);});
        }
        lastLevels=new LinkedHashMap<>(levels);
        previousQuests=new HashSet<>(done);lastXp=totalXp;baseline=true;
    }
    String text(Map<String,CompanionPlanner.Step> catalog) {
        StringBuilder out=new StringBuilder("Today (UTC "+day+"): "+xp+" observed XP\n");
        gainedLevels.forEach((skill,gain)->out.append(skill).append(": +").append(gain).append(" observed levels (latest ").append(lastLevels.get(skill)).append(")\n"));
        quests.forEach(q->{if(catalog.containsKey(q))out.append("Quest: ").append(catalog.get(q).name).append("\n");});
        return out.toString();
    }
}
