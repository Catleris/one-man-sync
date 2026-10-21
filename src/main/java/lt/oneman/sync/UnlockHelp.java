package lt.oneman.sync;

import java.util.*;

/** Short player-facing copy. Evidence and developer diagnostics stay out of the cards. */
final class UnlockHelp
{
    private static final Map<String,String> BENEFITS=new HashMap<>();
    private static final Map<String,Integer> COSTS=new HashMap<>();
    static {
        BENEFITS.put("SUPERIOR","Encounter superior versions of eligible Slayer monsters.");
        BENEFITS.put("SALT","Finish rockslugs automatically while carrying salt.");
        BENEFITS.put("FREEZE","Finish desert lizards automatically while carrying an ice cooler.");
        BENEFITS.put("GARGOYLES","Finish gargoyles automatically while carrying a rock hammer.");
        BENEFITS.put("ZYGOMITES","Finish zygomites automatically while carrying fungicide supplies.");
        BENEFITS.put("BOSSES","Receive boss Slayer assignments.");
        BENEFITS.put("LIZARDMEN","Receive lizardmen Slayer assignments.");
        BENEFITS.put("BASILISK","Receive basilisk Slayer assignments.");
        BENEFITS.put("AVIANSIES","Receive aviansie Slayer assignments.");
        BENEFITS.put("REDDRAGONS","Receive red dragon Slayer assignments.");
        BENEFITS.put("TZHAAR","Receive TzHaar Slayer assignments.");
        BENEFITS.put("WYVERN_BLOCK","Block Fossil Island wyvern assignments.");
        BENEFITS.put("NOTED_BARS","Unlock noted mithril-bar drops.");
        BENEFITS.put("GROTESQUES","Count Grotesque Guardians kills as two task kills.");
        BENEFITS.put("VAMPYRES","Receive vampyre Slayer assignments.");
        BENEFITS.put("STORAGE","Store a Slayer task for later.");
        BENEFITS.put("WARPED","Receive warped creature Slayer assignments.");
        // Current costs confirmed against the Summer Sweep Up / current reward tables.
        COSTS.put("SUPERIOR",50);COSTS.put("SALT",10);COSTS.put("FREEZE",10);COSTS.put("REDDRAGONS",50);
    }
    private UnlockHelp() {}
    static String title(UnlockCatalog.Entry entry)
    {
        if(entry.id.equals("SLAYER_REDDRAGONS"))return "Seeing Red · Red dragons";
        return entry.title.replace(" quest gate"," access").replace(" purchase","");
    }
    static String status(UnlockCatalog.Result result)
    {
        String label=result.state.label;
        if(result.state==UnlockCatalog.State.PENDING) {
            if(result.entry.id.startsWith("SLAYER_"))label="Locked";
            else if("QUEST".equals(result.entry.mode)||"JOURNAL".equals(result.entry.mode))label="Quest unfinished";
            else if("DIARY".equals(result.entry.mode))label="Diary unfinished";
        }
        Integer cost=COSTS.get(result.entry.id.replaceFirst("^SLAYER_",""));
        if(cost!=null&&result.state==UnlockCatalog.State.PENDING)label+=" · "+cost+" Slayer points";
        return label;
    }
    static String details(UnlockCatalog.Result result)
    {
        UnlockCatalog.Entry entry=result.entry;
        if(entry.id.startsWith("SLAYER_")) {
            String key=entry.id.substring(7);
            String benefit=BENEFITS.getOrDefault(key,"Unlock this Slayer helmet recolour.");
            String action=result.state==UnlockCatalog.State.UNLOCKED?"Manage at a Slayer master → Rewards.":"Slayer master → Rewards → Unlock.";
            return benefit+"\n\n"+action;
        }
        List<String> lines=new ArrayList<>();
        for(String line:result.detail.split("\n")) {
            if(line.startsWith("Source:")||line.startsWith("Known gates")||line.startsWith("No automatic")||line.startsWith("✓ "))continue;
            if(line.startsWith("Item seen:")){lines.add("Previously seen in your items.");continue;}
            if(line.startsWith("Quest:")&&line.contains("finished"))continue;
            if(line.startsWith("• ")){lines.add("Complete: "+line.substring(2).replaceFirst(":.*$",""));continue;}
            if(line.endsWith(" base level")) {
                String plain=line.replace(" base level","");
                java.util.regex.Matcher m=java.util.regex.Pattern.compile(".*: (\\d+) / (\\d+)$").matcher(plain);
                if(m.matches()&&Integer.parseInt(m.group(1))>=Integer.parseInt(m.group(2)))continue;
                lines.add(plain);continue;
            }
            for(String sentence:line.split("(?<=[.!?])\\s+")) {
                String lower=sentence.toLowerCase(Locale.ROOT);
                if(lower.contains("not prove")||lower.contains("not proof")||lower.contains("not inferred")
                    ||lower.contains("not a claim")||lower.contains("prediction")||lower.contains("readiness")
                    ||lower.contains("flag")||lower.contains("source:")||lower.contains("not game verification"))continue;
                if(!sentence.trim().isEmpty())lines.add(sentence);
            }
        }
        if(lines.isEmpty())lines.add(result.state==UnlockCatalog.State.UNLOCKED?"Unlocked.":"Open the guide for the next step.");
        return String.join("\n",new LinkedHashSet<>(lines));
    }
}
