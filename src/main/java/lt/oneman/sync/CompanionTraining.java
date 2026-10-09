package lt.oneman.sync;
import java.util.*;
import net.runelite.api.Experience;
/** Goal-specific static plan, filtered using real levels, quest access and observed task. */
final class CompanionTraining {
    static boolean isSkill(CompanionPlanner.Step s){return s!=null&&s.skills.size()==1&&("training".equals(s.type)||"skill".equals(s.type));}
    static boolean achieved(CompanionPlanner.Step s,Map<String,Integer> levels,Set<String> quests,Set<String> unlocks) {
        return quests.contains(s.id)||unlocks.contains(s.id)||isSkill(s)&&s.skills.entrySet().stream().allMatch(e->levels.containsKey(e.getKey())&&levels.get(e.getKey())>=e.getValue());
    }
    static List<String> masters(int combat,int slayer,Set<String> quests) {
        List<String> choices=new ArrayList<>();
        if(combat>=100&&slayer>=50&&quests.contains("SHILO_VILLAGE"))choices.add("Duradel / Kuradal — Shilo Village");
        if(combat>=85)choices.add("Nieve / Steve — Gnome Stronghold");
        if(combat>=75)choices.add("Konar — Mount Karuulm; assignments restrict the location");
        if(combat>=70&&quests.contains("LOST_CITY"))choices.add("Chaeldar — Zanaris");
        if(combat>=40)choices.add("Vannaka — Edgeville Dungeon");
        choices.add("Turael / Spria — low-level tasks; check streak-reset rules before replacing a task");
        return choices;
    }
    static String describe(CompanionPlanner.Step s,Map<String,Integer> levels,Set<String> quests,int combat,String accountType,
        SlayerLabJournal.Session task,int slayerXp,Map<String,List<String>> methods) {
        if(!isSkill(s))return "";
        String skill=s.skills.keySet().iterator().next();int target=s.skills.get(skill);Integer current=levels.get(skill);
        StringBuilder text=new StringBuilder("Goal: "+skill+" "+target+"\nCurrent: "+(current==null?"unknown":current)+" / "+target+"\n");
        if(current!=null)text.append("Levels remaining: ").append(Math.max(0,target-current)).append("\n");
        text.append("Account: ").append(accountType).append(" · combat ").append(combat).append("\n\n");
        if(skill.equals("Slayer")) {
            text.append("XP remaining: ").append(Math.max(0,Experience.getXpForLevel(target)-slayerXp)).append("\n\n");
            if(task!=null&&task.ended==0)text.append("1. Continue your current task: ").append(task.task).append(" — ").append(task.remaining).append(" remaining.\nAssigned area: ").append(task.assignedArea==null||task.assignedArea.isEmpty()?"any eligible area":task.assignedArea).append("\n");
            else text.append("1. Obtain an assignment from an accessible Slayer master. Do not kill arbitrary monsters for Slayer XP.\n");
            text.append("2. Open OneMan → Slayer & Prayer for this task's places, cannon rules, protection and carried supplies.\n")
                .append("3. Complete tasks and return to your chosen master until ").append(target).append(" Slayer. Progress updates from real XP; task rolls are not predicted.\n\nEligible master options from known requirements:\n");
            masters(combat,current==null?0:current,quests).forEach(m->text.append("• ").append(m).append("\n"));
            text.append("\nCannon is optional: Dwarf Cannon completion, all parts, ammunition and a permitted chamber are needed. No ownership or cannonball quantity is assumed. Use your own supplies; Ironman accounts obtain them themselves.\n");
            if(task!=null)text.append("\nObserved session Slayer XP: ").append(task.slayerXp).append(". No completion-time estimate until a representative rate is observed.");
        } else {
            List<String> method=methods.get(skill);text.append("1. ").append(method==null||method.isEmpty()?"Choose an accessible training method in the skill guide.":method.get(method.size()>1?1:0))
                .append("\n2. Check that method's actual access and supplies; bank ownership and usable gear are separate.\n3. Train until ").append(target).append("; real levels update this target automatically.\n");
        }
        return text.toString();
    }
}
