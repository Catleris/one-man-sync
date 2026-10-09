package lt.oneman.sync;

import com.google.gson.*;
import java.util.*;

/** Deterministic local view of the OneMan catalog. Never projects quest XP or gear ownership. */
final class CompanionPlanner {
    static final class Step {
        String id, name, type, wiki, completedByQuest;
        double rank; int qp, questPoints;
        boolean autoSkills;
        Map<String,Integer> skills = new LinkedHashMap<>();
        List<String> quests = new ArrayList<>();
        List<String> why = new ArrayList<>();
        public String toString() { return name; }
    }
    static final class Catalog { List<Step> nodes; Map<String,List<String>> methods; }
    final Map<String,Step> nodes = new LinkedHashMap<>();
    final List<Step> ordered;
    final Map<String,List<String>> methods;
    CompanionPlanner(Catalog data) {
        methods=data.methods==null?Collections.emptyMap():data.methods;
        ordered = new ArrayList<>(data.nodes);
        ordered.sort(Comparator.comparingDouble(n -> n.rank));
        ordered.forEach(n -> nodes.put(n.id,n));
    }
    List<Step> plan(Map<String,Integer> levels, Set<String> completed, Set<String> manual, int qp) {
        List<Step> out = new ArrayList<>(); Set<String> visited = new HashSet<>();
        for (Step n : ordered) {
            if (n.id.startsWith("MAX_")) continue; // Maxing is a separate long-term goal, not an early next step.
            visit(n, levels, completed, manual, qp, visited, out);
        }
        return out;
    }
    Step trainingFromId(String id) {
        if(!id.startsWith("TRAIN_"))return null;
        int split=id.lastIndexOf('_');if(split<=6)return null;
        String skill=id.substring(6,split);if(!methods.containsKey(skill))return null;
        try{int level=Integer.parseInt(id.substring(split+1));if(level<2||level>99)return null;
            Step step=new Step();step.id=id;step.type="training";step.name=skill+" → "+level;
            step.skills.put(skill,level);step.wiki="https://oldschool.runescape.wiki/w/"+skill;return step;
        }catch(NumberFormatException ignored){return null;}
    }
    List<Step> forGoal(Step goal,Map<String,Integer> levels,Set<String> done,Set<String> confirmed,int qp) {
        List<Step> steps=new ArrayList<>();visit(goal,levels,done,confirmed,qp,new HashSet<>(),steps);return steps;
    }
    private void visit(Step n, Map<String,Integer> levels, Set<String> done, Set<String> manual,
        int qp, Set<String> visited, List<Step> out) {
        if (!visited.add(n.id) || done.contains(n.id) || manual.contains(n.id)
            || n.completedByQuest != null && done.contains(n.completedByQuest)) return;
        if (n.autoSkills && n.skills.entrySet().stream().allMatch(e -> levels.containsKey(e.getKey()) && levels.get(e.getKey()) >= e.getValue())) return;
        for (String q : n.quests) { Step prior = nodes.get(q); if (prior != null) visit(prior,levels,done,manual,qp,visited,out); }
        for (Map.Entry<String,Integer> e : n.skills.entrySet()) {
            Integer have = levels.get(e.getKey());
            if (!n.autoSkills && !"training".equals(n.type) && (have == null || have < e.getValue())) {
                String id = "TRAIN_"+e.getKey()+"_"+e.getValue();
                if (visited.add(id)) {
                    if (e.getKey().equals("Herblore") && nodes.containsKey("DRUIDIC_RITUAL")) visit(nodes.get("DRUIDIC_RITUAL"),levels,done,manual,qp,visited,out);
                    Step training = new Step(); training.id=id; training.type="training";
                    training.name=e.getKey()+" → "+e.getValue(); training.skills.put(e.getKey(),e.getValue());
                    List<String> method=methods.getOrDefault(e.getKey(),Arrays.asList("Pasirink prieinamą training metodą.","Choose an accessible training method."));
                    training.why=Arrays.asList("Reikia tikslui: "+n.name+". "+method.get(0),"Needed for: "+n.name+". "+method.get(Math.min(1,method.size()-1)));
                    training.wiki="https://oldschool.runescape.wiki/w/"+e.getKey(); out.add(training);
                }
            }
        }
        out.add(n);
    }
    String requirements(Step n, Map<String,Integer> levels, Set<String> done, int qp) {
        StringBuilder s=new StringBuilder();
        n.skills.forEach((k,v)->s.append(k).append(": ").append(levels.containsKey(k)?levels.get(k):"unknown").append(" / ").append(v).append("\n"));
        for(String q:n.quests) s.append(done.contains(q)?"✓ ":"Missing quest: ").append(nodes.containsKey(q)?nodes.get(q).name:q).append("\n");
        if(n.qp>0)s.append("Quest points: ").append(qp).append(" / ").append(n.qp).append("\n");
        s.append("Check supplies and encounters; this is not a combat-readiness claim.");
        return s.toString();
    }
}
