package lt.oneman.sync;

import java.awt.Color;
import java.util.*;

/** Small, explicit task preparation catalog. Never infers a full combat loadout from Wiki prose. */
final class SlayerTaskItems
{
    enum Tier { MANDATORY, UTILITY }
    enum Status { EQUIPPED, CARRIED, BANK, MISSING, UNKNOWN }
    static final Color MANDATORY = new Color(240, 145, 65);
    static final Color UTILITY = new Color(75, 175, 230);
    static final class Rule
    {
        final SlayerLabCatalog.Requirement item;
        final Tier tier;
        final String note;
        Rule(Tier tier, String label, String note, String... names)
        { this.tier=tier; this.item=new SlayerLabCatalog.Requirement(label,false,names); this.note=note; }
        boolean matches(String name)
        {
            String n=SlayerLabCatalog.normalize(name);
            if(item.alternatives.contains("*spray")) return n.matches("fungicide spray(?: [1-9]| 10)?");
            return item.matches(n);
        }
    }
    static final class Row
    {
        final Rule rule;
        final Status status;
        Row(Rule rule, Status status) { this.rule=rule; this.status=status; }
        Color color()
        {
            if(status==Status.EQUIPPED || status==Status.CARRIED) return new Color(100,210,125);
            if(status==Status.BANK) return new Color(240,200,80);
            return status==Status.UNKNOWN ? Color.GRAY : new Color(245,105,105);
        }
        String text()
        {
            String state=status==Status.EQUIPPED ? "Equipped" : status==Status.CARRIED ? "Carried" :
                status==Status.BANK ? "In bank snapshot" : status==Status.UNKNOWN ? "Bank unknown" : "Not in bank snapshot";
            return rule.item.label+" · "+state;
        }
    }
    private static Rule mandatory(String label,String note,String... names) { return new Rule(Tier.MANDATORY,label,note,names); }
    private static Rule utility(String label,String note,String... names) { return new Rule(Tier.UTILITY,label,note,names); }
    static List<Rule> rules(String task, boolean eliteKourend, String variant)
    {
        String key=SlayerLabCatalog.taskKey(task);
        switch(key)
        {
            case "gargoyles": return Collections.singletonList(mandatory("Gargoyle finishing tool","Still required with Gargoyle Smasher.","rock hammer","granite hammer","rock thrownhammer"));
            case "rockslugs": return Collections.singletonList(mandatory("Salt / brine sabre","Salt is consumed; equip the sabre to use its alternative finish.","bag of salt","brine sabre"));
            case "lizards": case "desert lizards": return Collections.singletonList(mandatory("Ice cooler","Consumed per finish; check quantity.","ice cooler"));
            case "mutated zygomites": case "zygomites": return Collections.singletonList(mandatory("Charged fungicide spray","Check charges and carry refills; empty spray is excluded.","*spray"));
            case "banshees": return Collections.singletonList(mandatory("Earmuffs / Slayer helmet","Wear before combat.","earmuffs","*slayer helmet"));
            case "aberrant spectres": return Collections.singletonList(mandatory("Nose peg / Slayer helmet","Wear before combat.","nose peg","*slayer helmet"));
            case "dust devils": case "smoke devils": return Collections.singletonList(mandatory("Facemask / Slayer helmet","Wear before combat.","facemask","*slayer helmet"));
            case "wall beasts": return Collections.singletonList(mandatory("Spiny helmet / Slayer helmet","Wear before passing wall beasts.","spiny helmet","*slayer helmet"));
            case "basilisks": case "cockatrice": return Collections.singletonList(mandatory("Mirror shield / V's shield","Equip; Slayer helmet does not replace shield protection.","mirror shield","v's shield"));
            case "cave horrors": return Collections.singletonList(utility("Witchwood icon","Equip for scream protection; Protect from Melee or safespotting are alternatives. Slayer helmet does not replace it.","witchwood icon"));
            case "kurask": case "turoth": return Collections.singletonList(mandatory("Leaf-bladed / broad / Slayer Dart setup","Check compatible weapon plus ammunition or Slayer Dart runes; presence alone is not a complete setup.","*leaf-bladed","broad arrows","broad bolts","amethyst broad bolts","slayer's staff","slayer's staff (e)","staff of the dead","toxic staff of the dead","staff of light","staff of balance"));
            case "cave kraken": case "kraken":
                return Collections.singletonList(utility("Fishing explosive","Optional Kraken boss activation; not needed for regular cave kraken.","fishing explosive"));
            case "skeletal wyverns": return Collections.singletonList(utility("Wyvern shield","Equip; antifire does not replace a wyvern shield.","elemental shield","mind shield","dragonfire shield","dragonfire ward","ancient wyvern shield"));
            case "fossil island wyverns": return Collections.singletonList(utility("Wyvern shield","Equip; elemental, mind or ancient wyvern shield also work for Ancient wyverns.","elemental shield","mind shield","ancient wyvern shield"));
            case "drakes": case "wyrms": case "hydras":
                return eliteKourend ? Collections.emptyList() : Collections.singletonList(utility("Karuulm floor boots","Wear inside Karuulm Slayer Dungeon; elite Kourend & Kebos Diary removes this requirement.","boots of stone","boots of brimstone","granite boots"));
            case "vampyres": return Collections.singletonList(utility("Ivandis / blisterwood flail","For Vyrewatch sentinels; ordinary vampyres do not require a flail.","ivandis flail","blisterwood flail"));
            // Generic dragon tasks include babies and variants with different protection needs.
            // Do not impose one dragonfire setup on all of them in this MVP.
            default: return Collections.emptyList();
        }
    }
    static List<Row> evaluate(List<Rule> rules, Set<String> inventory, Set<String> equipment, Map<String,Integer> bank, boolean bankKnown)
    {
        List<Row> out=new ArrayList<>();
        for(Rule rule:rules)
        {
            Status status=equipment.stream().anyMatch(rule::matches) ? Status.EQUIPPED :
                inventory.stream().anyMatch(rule::matches) ? Status.CARRIED :
                !bankKnown ? Status.UNKNOWN : bank.entrySet().stream().anyMatch(e->e.getValue()>0 && rule.matches(e.getKey())) ? Status.BANK : Status.MISSING;
            out.add(new Row(rule,status));
        }
        return Collections.unmodifiableList(out);
    }
    static Color highlight(String name, List<Row> rows)
    {
        for(Row row:rows)
            if(row.status!=Status.CARRIED && row.status!=Status.EQUIPPED && row.rule.matches(name))
                return row.rule.tier==Tier.MANDATORY ? MANDATORY : UTILITY;
        return null;
    }
}
