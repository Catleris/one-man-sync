package lt.oneman.sync;

import java.util.*;

/** Short reference routes. Carried items do not imply charges, spell access or diary completion. */
final class SlayerLabTravel
{
    static boolean slayerRing(Set<String> carried)
    { return carried.stream().anyMatch(n -> n.equals("slayer ring (eternal)") || n.matches("slayer ring \\([1-8]\\)")); }

    static String route(SlayerLabLocations.Destination d, Set<String> carried, Set<String> quests)
    {
        String n=d.name.toLowerCase(Locale.ROOT);
        StringBuilder out=new StringBuilder(d.name+"\n");
        // Specific destinations only: Stronghold of Security has no Slayer-ring teleport.
        if(n.equals("slayer tower") || n.equals("fremennik slayer dungeon") || n.equals("stronghold slayer cave"))
            out.append("Slayer ring → ").append(d.name).append(slayerRing(carried)?" [carried]\n":" [charged ring needed]\n");
        if(n.equals("catacombs of kourend"))
            out.append("Xeric's talisman → Xeric's Heart → statue entrance\nNeeds charges + Heart unlocked\n")
                .append("Kourend Castle Teleport → statue entrance\nStandard · 48 Magic · Client of Kourend\n");
        if(n.equals("slayer tower")) out.append("Fairy ring CKS → walk north to tower\nNeeds fairy-ring access\n");
        if(n.equals("fremennik slayer dungeon")) out.append("Fairy ring AJR → nearby cave entrance\nNeeds fairy-ring access\n");
        if(n.contains("karuulm"))
            out.append("Rada's blessing 3/4 → Mount Karuulm\nNeeds hard/elite Kourend & Kebos Diary; 3 has daily limit\nFairy ring CIR → walk north up volcano\nNeeds fairy-ring access\n");
        if(n.equals("chasm of fire")) out.append("Fairy ring DJR → nearby chasm entrance\nNeeds fairy-ring access\n");
        if(n.contains("taverley"))
            out.append("Falador teleport → walk north-west to dungeon\nStandard · 37 Magic, or teleport tablet\n");
        if(n.equals("smoke devil dungeon"))
            out.append("Ring of dueling → Castle Wars → walk south-west\nNeeds charged ring\n");
        if(n.equals("stronghold slayer cave"))
            out.append("Royal seed pod → Grand Tree → cave\n")
                .append(carried.contains("royal seed pod") && quests.contains("Monkey Madness II")?"CARRIED + quest completed\n":"Needs pod + verified Monkey Madness II\n");
        String quest=n.contains("heroes")?"Heroes' Quest":n.contains("iorwerth")?"Song of the Elves":n.contains("harmless")?"Cabin Fever":n.contains("lighthouse")?"Horror from the Deep":n.contains("wyvern cave")?"Bone Voyage":n.equals("slayer tower")||n.contains("spider")?"Priest in Peril":"";
        if(!quest.isEmpty()) out.append(quest).append(quests.contains(quest)?" ✓\n":" required\n");
        // Uncurated destinations keep their entrance note, rather than inventing a fastest route.
        if(out.toString().equals(d.name+"\n") || !quest.isEmpty()) out.append(d.access).append('\n');
        if(d.wilderness) out.append("Wilderness · PvP and item-loss risk\n");
        return out.append('\n').toString();
    }
}
