package lt.oneman.sync;

import java.util.*;

/** Item availability and known quest completion are explicit, never inferred from bank ownership. */
final class SlayerLabTravel
{
    static boolean slayerRing(Set<String> carried)
    { return carried.stream().anyMatch(n -> n.equals("slayer ring (eternal)") || n.matches("slayer ring \\([1-8]\\)")); }
    static String route(SlayerLabLocations.Destination d, Set<String> carried, Set<String> quests)
    {
        String n=d.name.toLowerCase(Locale.ROOT); StringBuilder out=new StringBuilder(d.name+"\n");
        if(n.contains("tower") || n.contains("fremennik") || n.contains("stronghold"))
            out.append("Slayer ring: ").append(slayerRing(carried)?"CARRIED charged/eternal ring":"NOT CARRIED").append(" → use the named dungeon teleport, then follow the entrance/chamber notes.\n");
        if(n.contains("karuulm"))
            out.append("Rada's blessing 3/4: ").append(carried.contains("rada's blessing 3")||carried.contains("rada's blessing 4")?"CARRIED — Mount Karuulm option":"NOT CARRIED").append(". Daily remaining uses of blessing 3 are not checked.\n");
        if(n.contains("taverley") || n.contains("asgar"))
            out.append("Falador teleport tablet: ").append(carried.contains("falador teleport")?"CARRIED":"NOT CARRIED").append(" → walk to the dungeon entrance; compare shortcuts with your Agility/key access.\n");
        if(n.contains("devil"))
            out.append("Ring of dueling: ").append(carried.stream().anyMatch(x->x.matches("ring of dueling \\([1-8]\\)"))?"CARRIED charged ring":"NOT CARRIED").append(" → Castle Wars, then walk south-east to the entrance.\n");
        if(n.contains("stronghold"))
            out.append("Royal seed pod: ").append(carried.contains("royal seed pod") && quests.contains("Monkey Madness II")?"CARRIED + quest completed":"requires carried pod and verified Monkey Madness II").append(" → Grand Tree, then walk to the cave.\n");
        String quest=n.contains("heroes")?"Heroes' Quest":n.contains("iorwerth")?"Song of the Elves":n.contains("harmless")?"Cabin Fever":n.contains("lighthouse")?"Horror from the Deep":n.contains("wyvern cave")?"Bone Voyage":n.contains("tower")||n.contains("spider")?"Priest in Peril":"";
        if(!quest.isEmpty()) out.append("Quest access: ").append(quest).append(" — ").append(quests.contains(quest)?"COMPLETED":"not verified completed").append("\n");
        out.append(d.access).append("\nOther teleports, fairy-ring access, POH destinations, spellbook/runes and shortcuts must be checked manually. No teleport or walking action is performed.\n\n");
        return out.toString();
    }
}
