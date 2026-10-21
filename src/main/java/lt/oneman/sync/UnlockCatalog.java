package lt.oneman.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;

/** Curated unlock evidence, separate from acquisition and current item possession. */
final class UnlockCatalog
{
    enum State
    {
        UNLOCKED("Unlocked"), PENDING("Requirements missing"), READY("Check progress"),
        OBSERVED("Previously seen"), MANUAL("Marked complete"), UNKNOWN("Not checked"), STARTED("In progress");
        final String label;
        State(String label) { this.label=label; }
        @Override public String toString() { return label; }
    }
    static final class Entry
    {
        final String id, category, title, mode, signal, note, wiki;
        final List<Quest> quests = new ArrayList<>();
        final Map<Skill,Integer> skills = new LinkedHashMap<>();
        final int varbit;
        Entry(String id,String category,String title,String mode,String signal,String questList,String skillList,String note,String wiki,int varbit)
        {
            this.id=id;this.category=category;this.title=title;this.mode=mode;this.signal=signal;this.note=note;this.varbit=varbit;
            this.wiki="https://oldschool.runescape.wiki/w/"+wiki.replace(" ","_").replace("'","%27");
            if(!questList.isEmpty()) for(String q:questList.split(";"))quests.add(Quest.valueOf(q));
            if(!skillList.isEmpty()) for(String s:skillList.split(";")) {
                String[] pair=s.split(":");skills.put(Skill.valueOf(pair[0].toUpperCase(Locale.ROOT)),Integer.parseInt(pair[1]));
            }
        }
        boolean manualAllowed() { return "CHECK".equals(mode)||"ITEM".equals(mode); }
    }
    static final class Facts
    {
        boolean live;
        final Map<Quest,QuestState> quests=new EnumMap<>(Quest.class);
        final Map<Skill,Integer> skills=new EnumMap<>(Skill.class);
        final Map<Integer,Integer> bits=new HashMap<>();
    }
    static final class Result
    {
        final Entry entry;
        final State state;
        final String detail;
        Result(Entry entry,State state,String detail) {this.entry=entry;this.state=state;this.detail=detail;}
    }
    final List<Entry> entries=new ArrayList<>();
    final Map<String,Entry> byId=new LinkedHashMap<>();
    private final Map<String,List<Entry>> itemIndex=new HashMap<>();
    final Map<String,Integer> flags=new LinkedHashMap<>();
    UnlockCatalog()
    {
        flags.put("FAIRY_RINGS",VarbitID.FAIRYRING_PERMISSION);
        flags.put("RIGOUR",VarbitID.PRAYER_RIGOUR_UNLOCKED);
        flags.put("AUGURY",VarbitID.PRAYER_AUGURY_UNLOCKED);
        flags.put("PRESERVE",VarbitID.PRAYER_PRESERVE_UNLOCKED);
        flags.put("DEADEYE",VarbitID.PRAYER_DEADEYE_UNLOCKED);
        flags.put("MYSTIC_VIGOUR",VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED);
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(Objects.requireNonNull(
            getClass().getResourceAsStream("unlocks.tsv")),StandardCharsets.UTF_8)))
        {
            String line;
            while((line=reader.readLine())!=null) {
                if(line.isEmpty()||line.startsWith("#"))continue;
                String[] c=line.split("\t",-1);if(c.length!=9)throw new IllegalArgumentException("Malformed unlock entry");
                int bit="FLAG".equals(c[3])?Objects.requireNonNull(flags.get(c[4])):-1;
                add(new Entry(c[0],c[1],c[2],c[3],c[4],c[5],c[6],c[7],c[8],bit));
            }
        } catch(IOException error) { throw new IllegalStateException("Cannot load bundled unlock catalog",error); }
        addAdditional();
        for(Quest quest:Quest.values())add(new Entry("QUEST_"+quest.name(),"Quest journal",quest.getName(),"JOURNAL",quest.name(),"","",
            "Quest completion only. Open the guide for the full quest requirements and individual rewards.",quest.getName(),-1));
    }
    private void add(Entry entry)
    {
        if(byId.putIfAbsent(entry.id,entry)!=null)throw new IllegalArgumentException("Duplicate unlock "+entry.id);
        // Resolve quest signals at startup, never silently hide a misspelt quest.
        if("QUEST".equals(entry.mode)||"JOURNAL".equals(entry.mode))Quest.valueOf(entry.signal);
        entries.add(entry);
        if("ITEM".equals(entry.mode))for(String name:entry.signal.split(";"))
            itemIndex.computeIfAbsent(normalize(name),key->new ArrayList<>()).add(entry);
    }
    private void flag(String key,String title,int bit,String note)
    {
        add(new Entry("SLAYER_"+key,"Slayer purchases",title,"FLAG",key,"","",note,"Slayer_Rewards",bit));
    }
    private void diary(String area,int...bits)
    {
        String[] tiers={"Easy","Medium","Hard","Elite"};
        for(int i=0;i<4;i++)add(new Entry("DIARY_"+area+"_"+tiers[i],"Achievement Diaries",area+" "+tiers[i],"DIARY",area+"|"+tiers[i],"","",
            "Diary completion flag. Physical reward ownership and remaining daily charges are separate.",area+"_Diary",bits[i]));
    }
    private void ca(String tier,int bit)
    {
        add(new Entry("CA_"+tier,"Combat Achievements",tier+" rewards claimed","CA",tier,"","",
            "Reward-claim status. This does not evaluate boss readiness or individual combat mechanics.","Combat_Achievements",bit));
    }
    private void addAdditional()
    {
        diary("Ardougne",VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE,VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE,VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE);
        diary("Desert",VarbitID.DESERT_DIARY_EASY_COMPLETE,VarbitID.DESERT_DIARY_MEDIUM_COMPLETE,VarbitID.DESERT_DIARY_HARD_COMPLETE,VarbitID.DESERT_DIARY_ELITE_COMPLETE);
        diary("Falador",VarbitID.FALADOR_DIARY_EASY_COMPLETE,VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE,VarbitID.FALADOR_DIARY_HARD_COMPLETE,VarbitID.FALADOR_DIARY_ELITE_COMPLETE);
        diary("Fremennik",VarbitID.FREMENNIK_DIARY_EASY_COMPLETE,VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE,VarbitID.FREMENNIK_DIARY_HARD_COMPLETE,VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE);
        diary("Kandarin",VarbitID.KANDARIN_DIARY_EASY_COMPLETE,VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE,VarbitID.KANDARIN_DIARY_HARD_COMPLETE,VarbitID.KANDARIN_DIARY_ELITE_COMPLETE);
        diary("Karamja",VarbitID.ATJUN_EASY_DONE,VarbitID.ATJUN_MED_DONE,VarbitID.ATJUN_HARD_DONE,VarbitID.KARAMJA_DIARY_ELITE_COMPLETE);
        diary("Kourend & Kebos",VarbitID.KOUREND_DIARY_EASY_COMPLETE,VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE,VarbitID.KOUREND_DIARY_HARD_COMPLETE,VarbitID.KOUREND_DIARY_ELITE_COMPLETE);
        diary("Lumbridge & Draynor",VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE,VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE,VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE,VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE);
        diary("Morytania",VarbitID.MORYTANIA_DIARY_EASY_COMPLETE,VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE,VarbitID.MORYTANIA_DIARY_HARD_COMPLETE,VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE);
        diary("Varrock",VarbitID.VARROCK_DIARY_EASY_COMPLETE,VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,VarbitID.VARROCK_DIARY_HARD_COMPLETE,VarbitID.VARROCK_DIARY_ELITE_COMPLETE);
        diary("Western Provinces",VarbitID.WESTERN_DIARY_EASY_COMPLETE,VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE,VarbitID.WESTERN_DIARY_HARD_COMPLETE,VarbitID.WESTERN_DIARY_ELITE_COMPLETE);
        diary("Wilderness",VarbitID.WILDERNESS_DIARY_EASY_COMPLETE,VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE,VarbitID.WILDERNESS_DIARY_HARD_COMPLETE,VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE);
        ca("Easy",VarbitID.CA_TIER_STATUS_EASY);
        ca("Medium",VarbitID.CA_TIER_STATUS_MEDIUM);
        ca("Hard",VarbitID.CA_TIER_STATUS_HARD);
        ca("Elite",VarbitID.CA_TIER_STATUS_ELITE);
        ca("Master",VarbitID.CA_TIER_STATUS_MASTER);
        ca("Grandmaster",VarbitID.CA_TIER_STATUS_GRANDMASTER);
        flag("SUPERIOR","Bigger and Badder",VarbitID.SLAYER_UNLOCK_SUPERIORMOBS,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("SALT","Slug Salter",VarbitID.SLAYER_AUTOKILL_ROCKSLUGS,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("FREEZE","Reptile Freezer",VarbitID.SLAYER_AUTOKILL_DESERTLIZARDS,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("GARGOYLES","Gargoyle smasher",VarbitID.SLAYER_AUTOKILL_GARGOYLES,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("ZYGOMITES","Shroom Sprayer",VarbitID.SLAYER_AUTOKILL_ZYGOMITES,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("BOSSES","Like a boss",VarbitID.SLAYER_UNLOCK_BOSSES,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("LIZARDMEN","Reptile got ripped",VarbitID.SLAYER_UNLOCK_LIZARDMEN,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("BASILISK","Basilocked",VarbitID.SLAYER_UNLOCK_BASILISK,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("AVIANSIES","Aviansie tasks",VarbitID.SLAYER_UNLOCK_AVIANSIES,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("REDDRAGONS","Red dragon tasks",VarbitID.SLAYER_UNLOCK_REDDRAGONS,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("TZHAAR","TzHaar tasks",VarbitID.SLAYER_UNLOCK_TZHAAR,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("WYVERN_BLOCK","Fossil Island wyvern block",VarbitID.SLAYER_UNLOCK_FOSSILWYVERNBLOCK,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("NOTED_BARS","Noted mithril bars",VarbitID.SLAYER_UNLOCK_NOTEDMITHRILBARS,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("GROTESQUES","Double Trouble",VarbitID.SLAYER_UNLOCK_GROTESQUEKILLS,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("VAMPYRES","Vampyre task unlock",VarbitID.SLAYER_UNLOCK_VAMPYRES,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("STORAGE","Task storage purchase",VarbitID.SLAYER_UNLOCK_STORAGE,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("WARPED","Warped creature tasks",VarbitID.SLAYER_UNLOCK_WARPED_CREATURES,"Purchase flag only; task eligibility, current toggles and stored tasks are separate.");
        flag("HELM_BLACK","Black Slayer helmet recolour",VarbitID.SLAYER_UNLOCK_HELM_BLACK,"Recolour purchase flag; does not prove the helmet or required trophy is currently owned.");
        flag("HELM_GREEN","Green Slayer helmet recolour",VarbitID.SLAYER_UNLOCK_HELM_GREEN,"Recolour purchase flag; does not prove the helmet or required trophy is currently owned.");
        flag("HELM_RED","Red Slayer helmet recolour",VarbitID.SLAYER_UNLOCK_HELM_RED,"Recolour purchase flag; does not prove the helmet or required trophy is currently owned.");
        flag("HELM_PURPLE","Purple Slayer helmet recolour",VarbitID.SLAYER_UNLOCK_HELM_PURPLE,"Recolour purchase flag; does not prove the helmet or required trophy is currently owned.");
        flag("HELM_TURQUOISE","Turquoise Slayer helmet recolour",VarbitID.SLAYER_UNLOCK_HELM_TURQUOISE,"Recolour purchase flag; does not prove the helmet or required trophy is currently owned.");
        flag("HELM_HYDRA","Hydra Slayer helmet recolour",VarbitID.SLAYER_UNLOCK_HELM_HYDRA,"Recolour purchase flag; does not prove the helmet or required trophy is currently owned.");
        flag("HELM_TWISTED","Twisted Slayer helmet recolour",VarbitID.SLAYER_UNLOCK_HELM_TWISTED,"Recolour purchase flag; does not prove the helmet or required trophy is currently owned.");
        flag("HELM_ARAXYTE","Araxyte Slayer helmet recolour",VarbitID.SLAYER_UNLOCK_HELM_ARAXYTE,"Recolour purchase flag; does not prove the helmet or required trophy is currently owned.");
    }
    Result evaluate(Entry entry,Facts facts,Set<String> manual,Map<String,Long> observed)
    {
        if(!facts.live)return new Result(entry,State.UNKNOWN,"Log in to load this account's progress.");
        List<String> lines=new ArrayList<>();
        State automatic=null;
        if("QUEST".equals(entry.mode)||"JOURNAL".equals(entry.mode)) {
            Quest quest=Quest.valueOf(entry.signal);QuestState state=facts.quests.get(quest);
            automatic=state==null?State.UNKNOWN:state==QuestState.FINISHED?State.UNLOCKED:state==QuestState.IN_PROGRESS?State.STARTED:State.PENDING;
            lines.add("Quest: "+quest.getName()+" — "+(state==null?"unknown":state.toString().toLowerCase(Locale.ROOT).replace('_',' ')));
        } else if(entry.varbit>=0) {
            Integer value=facts.bits.get(entry.varbit);
            boolean done=false;
            if(value!=null) {
                if("DIARY".equals(entry.mode)) {String[] parts=entry.signal.split("\\|");done=DiaryCompletion.complete(parts[0],parts[1],value);}
                else done="CA".equals(entry.mode)?value==2:value>0;
            }
            automatic=value==null?State.UNKNOWN:done?State.UNLOCKED:"CA".equals(entry.mode)&&value==1?State.READY:State.PENDING;
            lines.add("Source: game "+("CA".equals(entry.mode)?"reward status":"completion / purchase flag"));
        }
        boolean missing=false,unknown=false;
        for(Quest quest:entry.quests) {
            QuestState state=facts.quests.get(quest);
            if(state==null)unknown=true;else if(state!=QuestState.FINISHED)missing=true;
            lines.add((state==QuestState.FINISHED?"✓ ":"• ")+quest.getName()+": "+(state==null?"unknown":state.toString().toLowerCase(Locale.ROOT).replace('_',' ')));
        }
        for(Map.Entry<Skill,Integer> gate:entry.skills.entrySet()) {
            Integer level=facts.skills.get(gate.getKey());
            if(level==null)unknown=true;else if(level<gate.getValue())missing=true;
            lines.add(gate.getKey().getName()+": "+(level==null?"?":level)+" / "+gate.getValue()+" base level");
        }
        State state=automatic;
        if(state==null) {
            if(observed.containsKey(entry.id)) {
                state=State.OBSERVED;
                lines.add("Item seen: "+java.time.Instant.ofEpochMilli(observed.get(entry.id)).toString()+". Historical observation, not proof of current ownership or charges.");
            } else if(manual.contains(entry.id)) {state=State.MANUAL;lines.add("Source: your manual confirmation, not game verification.");}
            else state=unknown?State.UNKNOWN:missing?State.PENDING:State.READY;
        }
        if(lines.isEmpty())lines.add("No automatic completion signal is configured. Check the guide and confirm manually if completed.");
        else if(automatic==null)lines.add("Known gates only; the guide contains additional acquisition or access steps.");
        lines.add(entry.note);
        return new Result(entry,state,String.join("\n",lines));
    }
    Collection<Entry> matchingItems(String name)
    {
        String key=normalize(name);
        List<Entry> exact=itemIndex.get(key);
        if(exact!=null)return exact;
        // Charged variants commonly append a number; do not merge unrelated named variants.
        return itemIndex.getOrDefault(key.replaceFirst("[0-9]+$",""),Collections.emptyList());
    }
    static String normalize(String name) {return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    static boolean matchesItem(Entry entry,String name)
    {
        if(!"ITEM".equals(entry.mode))return false;
        String actual=normalize(name);
        for(String variant:entry.signal.split(";")) {
            String base=normalize(variant);
            if(actual.equals(base))return true;
            if(actual.startsWith(base)&&actual.substring(base.length()).matches("[0-9]+"))return true;
        }
        return false;
    }
}
