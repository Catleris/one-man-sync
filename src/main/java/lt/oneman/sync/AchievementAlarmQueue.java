package lt.oneman.sync;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Only explicit new-unlock messages are accepted; old log scans never create alarms. */
final class AchievementAlarmQueue
{
    static final Pattern COLLECTION = Pattern.compile("^New item added to your collection log:\\s*(.+?)[.]?$", Pattern.CASE_INSENSITIVE);
    static final Pattern COMBAT = Pattern.compile("^(?:CA_ID:(\\d+)\\|)?Congratulations, you've completed an? (easy|medium|hard|elite|master|grandmaster) combat task:\\s*(.+?)(?:\\s*\\(\\d+ points?\\))?[.]?$", Pattern.CASE_INSENSITIVE);
    static final class Notice {
        final boolean combat, preview;
        final String name, detail, key;
        final int tier;
        Notice(boolean combat, String name, String detail, int tier, boolean preview) {
            this.combat=combat; this.name=name; this.detail=detail; this.tier=tier; this.preview=preview;
            this.key=(combat?"ca:":"cl:")+name.toLowerCase(java.util.Locale.ROOT);
        }
    }
    private final Deque<Notice> pending=new ArrayDeque<>();
    private final Map<String,Long> seen=new LinkedHashMap<>();
    private Notice current;
    private long until;
    static Notice parse(String message) {
        Matcher item=COLLECTION.matcher(message);
        if(item.matches()&&!item.group(1).trim().isEmpty())return new Notice(false,item.group(1).trim(),"NEW ITEM",-1,false);
        Matcher task=COMBAT.matcher(message);
        if(!task.matches())return null;
        String[] tiers={"easy","medium","hard","elite","master","grandmaster"};
        int tier=java.util.Arrays.asList(tiers).indexOf(task.group(2).toLowerCase(java.util.Locale.ROOT));
        return new Notice(true,task.group(3).trim(),tiers[tier].toUpperCase(java.util.Locale.ROOT)+"  •  +"+(tier+1)+" points",tier,false);
    }
    void add(Notice notice,long now) {
        Long last=seen.get(notice.key);
        if(!notice.preview&&last!=null&&now-last<10000)return;
        seen.put(notice.key,now);
        while(seen.size()>128)seen.remove(seen.keySet().iterator().next());
        if(pending.size()<32)pending.addLast(notice);
    }
    Notice advance(long now,int seconds,boolean collection,boolean combat) {
        pending.removeIf(n->n.combat?!combat:!collection);
        if(current!=null&&(now>=until||(current.combat?!combat:!collection)))current=null;
        if(current==null&&!pending.isEmpty()){current=pending.removeFirst();until=now+seconds*1000L;}
        return current;
    }
    void clear(){pending.clear();seen.clear();current=null;until=0;}
}
