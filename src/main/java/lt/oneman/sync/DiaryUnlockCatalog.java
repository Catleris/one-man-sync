package lt.oneman.sync;
import java.util.*;
import net.runelite.api.gameval.VarbitID;
/** All twelve Achievement Diary regions and their four completion states. */
final class DiaryUnlockCatalog {
    static final class Entry {
        final String area;final int tier,varbit;
        Entry(String area,int tier,int varbit){this.area=area;this.tier=tier;this.varbit=varbit;}
        boolean complete(int value){return DiaryCompletion.complete(area,AchievementAlarmQueue.TIERS[tier],value);}
    }
    static final List<Entry> ENTRIES=create();
    private static List<Entry> create(){List<Entry> out=new ArrayList<>();
        add(out,"Ardougne",VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE,VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE,VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE);
        add(out,"Desert",VarbitID.DESERT_DIARY_EASY_COMPLETE,VarbitID.DESERT_DIARY_MEDIUM_COMPLETE,VarbitID.DESERT_DIARY_HARD_COMPLETE,VarbitID.DESERT_DIARY_ELITE_COMPLETE);
        add(out,"Falador",VarbitID.FALADOR_DIARY_EASY_COMPLETE,VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE,VarbitID.FALADOR_DIARY_HARD_COMPLETE,VarbitID.FALADOR_DIARY_ELITE_COMPLETE);
        add(out,"Fremennik",VarbitID.FREMENNIK_DIARY_EASY_COMPLETE,VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE,VarbitID.FREMENNIK_DIARY_HARD_COMPLETE,VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE);
        add(out,"Kandarin",VarbitID.KANDARIN_DIARY_EASY_COMPLETE,VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE,VarbitID.KANDARIN_DIARY_HARD_COMPLETE,VarbitID.KANDARIN_DIARY_ELITE_COMPLETE);
        add(out,"Karamja",VarbitID.ATJUN_EASY_DONE,VarbitID.ATJUN_MED_DONE,VarbitID.ATJUN_HARD_DONE,VarbitID.KARAMJA_DIARY_ELITE_COMPLETE);
        add(out,"Kourend & Kebos",VarbitID.KOUREND_DIARY_EASY_COMPLETE,VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE,VarbitID.KOUREND_DIARY_HARD_COMPLETE,VarbitID.KOUREND_DIARY_ELITE_COMPLETE);
        add(out,"Lumbridge & Draynor",VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE,VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE,VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE,VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE);
        add(out,"Morytania",VarbitID.MORYTANIA_DIARY_EASY_COMPLETE,VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE,VarbitID.MORYTANIA_DIARY_HARD_COMPLETE,VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE);
        add(out,"Varrock",VarbitID.VARROCK_DIARY_EASY_COMPLETE,VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,VarbitID.VARROCK_DIARY_HARD_COMPLETE,VarbitID.VARROCK_DIARY_ELITE_COMPLETE);
        add(out,"Western Provinces",VarbitID.WESTERN_DIARY_EASY_COMPLETE,VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE,VarbitID.WESTERN_DIARY_HARD_COMPLETE,VarbitID.WESTERN_DIARY_ELITE_COMPLETE);
        add(out,"Wilderness",VarbitID.WILDERNESS_DIARY_EASY_COMPLETE,VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE,VarbitID.WILDERNESS_DIARY_HARD_COMPLETE,VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE);
        return Collections.unmodifiableList(out);
    }
    private static void add(List<Entry> out,String area,int... bits){for(int i=0;i<bits.length;i++)out.add(new Entry(area,i,bits[i]));}
}
