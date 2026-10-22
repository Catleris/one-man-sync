package lt.oneman.sync;

import java.awt.Color;
import java.util.*;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.StructComposition;
import net.runelite.client.util.Text;

/** Immutable game-cache catalogue. Read on the client thread; never downloads task data. */
final class CombatAchievementCatalog
{
    // These enum/struct parameters have no named RuneLite gameval constants.
    // Cache mapping independently checked against jimmyununu/combat-achievement-lookup.
    static final int BOSS_NAMES=3971, TASK_ID=1306, NAME=1308, DESCRIPTION=1309, TYPE=1311, BOSS=1312;
    enum Tier {
        EASY("Easy",3981,0x86cf97), MEDIUM("Medium",3982,0x83bdec), HARD("Hard",3983,0xec9292),
        ELITE("Elite",3984,0xc59be7), MASTER("Master",3985,0xe7b47c), GRANDMASTER("Grandmaster",3986,0xe7c65d);
        final String label;final int enumId;final Color color;
        Tier(String label,int enumId,int rgb){this.label=label;this.enumId=enumId;this.color=new Color(rgb);}
        int points(){return ordinal()+1;}
        @Override public String toString(){return label;}
    }
    static final class Task {
        final int id;final String name,description,boss,type;final Tier tier;
        Task(int id,String name,String description,String boss,String type,Tier tier){
            this.id=id;this.name=name;this.description=description;this.boss=boss;this.type=type;this.tier=tier;
        }
    }
    static String type(int value){
        String[] names={"Other","Stamina","Perfection","Kill Count","Mechanical","Restriction","Speed"};
        return value>0&&value<names.length?names[value]:names[0];
    }
    static List<Task> load(Client client) {
        EnumComposition bosses=client.getEnum(BOSS_NAMES);
        if(bosses==null)return Collections.emptyList();
        List<Task> tasks=new ArrayList<>();Set<Integer> ids=new HashSet<>();
        for(Tier tier:Tier.values()) {
            EnumComposition entries=client.getEnum(tier.enumId);
            if(entries==null||entries.getIntVals()==null||entries.getIntVals().length==0)return Collections.emptyList();
            for(int structId:entries.getIntVals()) {
                StructComposition value=client.getStructComposition(structId);
                if(value==null)return Collections.emptyList();
                int id=value.getIntValue(TASK_ID);String name=clean(value.getStringValue(NAME));
                if(id<0||name.isEmpty()||!ids.add(id))return Collections.emptyList();
                String boss=clean(bosses.getStringValue(value.getIntValue(BOSS)));
                if(boss.isEmpty()||"None".equalsIgnoreCase(boss)||"null".equalsIgnoreCase(boss))boss="General";
                tasks.add(new Task(id,name,clean(value.getStringValue(DESCRIPTION)),boss,type(value.getIntValue(TYPE)),tier));
            }
        }
        tasks.sort(Comparator.comparing((Task t)->t.boss,String.CASE_INSENSITIVE_ORDER).thenComparingInt(t->t.tier.ordinal()).thenComparing(t->t.name));
        return Collections.unmodifiableList(tasks);
    }
    private static String clean(String value){return value==null?"":Text.removeTags(value.replace("<br>"," ")).trim();}
    static boolean completed(int id,int[] packed){return id>=0&&id/32<packed.length&&(packed[id/32]&(1<<(id%32)))!=0;}
}
