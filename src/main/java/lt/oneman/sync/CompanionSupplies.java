package lt.oneman.sync;
import java.util.*;
/** Curated quest start items. Check the linked quest guide for stage-specific extras. */
final class CompanionSupplies {
    static List<String> forStep(CompanionPlanner.Step s){
        if(s==null)return Collections.emptyList();
        switch(s.id){
            case "COOKS_ASSISTANT":return Arrays.asList("Bucket of milk","Egg","Pot of flour");
            case "DORICS_QUEST":return Arrays.asList("Clay","Copper ore","Iron ore");
            case "DRUIDIC_RITUAL":return Arrays.asList("Raw beef","Raw bear meat","Raw rat meat","Raw chicken");
            case "SHEEP_SHEARER":return Arrays.asList("Ball of wool");
            case "GOBLIN_DIPLOMACY":return Arrays.asList("Goblin mail","Orange dye","Blue dye");
            case "CHRONICLE":return Arrays.asList("Chronicle","Teleport card");
            default:
                if("training".equals(s.type)){
                    if(s.skills.containsKey("Mining"))return Arrays.asList("Pickaxe");
                    if(s.skills.containsKey("Woodcutting"))return Arrays.asList("Axe");
                }
                return Collections.emptyList();
        }
    }
    static boolean hasChronicle(Map<String,Integer> inventory,Map<String,Integer> equipment,Map<String,Integer> bank) {
        return java.util.stream.Stream.of(inventory,equipment,bank).anyMatch(m->m.entrySet().stream().anyMatch(e->e.getValue()>0&&matches(e.getKey(),"Chronicle")));
    }
    static int quantity(CompanionPlanner.Step s,String need) {
        if(s==null)return 1;
        if(s.id.equals("DORICS_QUEST"))return need.equals("Clay")?6:need.equals("Copper ore")?4:2;
        if(s.id.equals("SHEEP_SHEARER"))return 20;
        if(s.id.equals("GOBLIN_DIPLOMACY")&&need.equals("Goblin mail"))return 3;
        return 1;
    }
    static boolean matches(String item,String need){
        item=item.toLowerCase(Locale.ROOT);need=need.toLowerCase(Locale.ROOT);
        return need.equals("pickaxe")?item.endsWith(" pickaxe"):need.equals("axe")?item.endsWith(" axe"):item.equals(need);
    }
}
