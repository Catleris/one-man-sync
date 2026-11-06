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
        final String itemName;
        Row(Rule rule, Status status, String matchedName)
        {
            this.rule=rule;
            this.status=status;
            this.itemName=matchedName == null ? rule.item.label
                : Character.toUpperCase(matchedName.charAt(0)) + matchedName.substring(1);
        }
        Color color()
        {
            if(status==Status.EQUIPPED || status==Status.CARRIED) return new Color(100,210,125);
            if(status==Status.BANK) return new Color(240,200,80);
            return status==Status.UNKNOWN ? Color.GRAY : new Color(245,105,105);
        }
        String statusText()
        {
            return status==Status.EQUIPPED ? "Equipped" : status==Status.CARRIED ? "Carried" :
                status==Status.BANK ? "In bank snapshot" : status==Status.UNKNOWN ? "Bank unknown" : "Not in bank snapshot";
        }
        String text() { return itemName+" · "+statusText(); }
    }
    private static Rule mandatory(String label,String note,String... names) { return new Rule(Tier.MANDATORY,label,note,names); }
    private static Rule utility(String label,String note,String... names) { return new Rule(Tier.UTILITY,label,note,names); }
    static List<Rule> rules(String task, boolean eliteKourend)
    {
        String key=SlayerLabCatalog.taskKey(task);
        switch(key)
        {
            case "gargoyles": return Collections.singletonList(mandatory("Rock hammer / Granite hammer / Rock thrownhammer","Still required with Gargoyle Smasher.","rock hammer","granite hammer","rock thrownhammer"));
            case "rockslugs": return Collections.singletonList(mandatory("Bag of salt / Brine sabre","Salt is consumed; equip the sabre to use its alternative finish.","bag of salt","brine sabre"));
            case "lizards": case "desert lizards": return Collections.singletonList(mandatory("Ice cooler","Consumed per finish; check quantity.","ice cooler"));
            case "mutated zygomites": case "zygomites": return Collections.singletonList(mandatory("Fungicide spray (charged)","Check charges and carry refills; empty spray is excluded.","*spray"));
            case "banshees": return Collections.singletonList(mandatory("Earmuffs / Slayer helmet","Wear before combat.","earmuffs","*slayer helmet"));
            case "aberrant spectres": return Collections.singletonList(mandatory("Nose peg / Slayer helmet","Wear before combat.","nose peg","*slayer helmet"));
            case "dust devils": case "smoke devils": return Collections.singletonList(mandatory("Facemask / Slayer helmet","Wear before combat.","facemask","*slayer helmet"));
            case "wall beasts": return Collections.singletonList(mandatory("Spiny helmet / Slayer helmet","Wear before passing wall beasts.","spiny helmet","*slayer helmet"));
            case "basilisks": case "cockatrice": return Collections.singletonList(mandatory("Mirror shield / V's shield","Equip; Slayer helmet does not replace shield protection.","mirror shield","v's shield"));
            case "cave horrors": return Collections.singletonList(utility("Witchwood icon","Equip for scream protection; Protect from Melee or safespotting are alternatives. Slayer helmet does not replace it.","witchwood icon"));
            case "kurask": case "turoth": return Collections.singletonList(mandatory("Leaf-bladed sword / Broad bolts / Slayer's staff","Check compatible weapon plus ammunition or Slayer Dart runes; presence alone is not a complete setup.","*leaf-bladed","broad arrows","broad bolts","amethyst broad bolts","slayer's staff","slayer's staff (e)","staff of the dead","toxic staff of the dead","staff of light","staff of balance"));
            case "cave kraken": case "kraken":
                return Collections.singletonList(utility("Fishing explosive","Optional Kraken boss activation; not needed for regular cave kraken.","fishing explosive"));
            case "skeletal wyverns": return Collections.singletonList(utility("Elemental shield / Mind shield / Dragonfire shield / Dragonfire ward / Ancient wyvern shield","Equip; antifire does not replace a wyvern shield.","elemental shield","mind shield","dragonfire shield","dragonfire ward","ancient wyvern shield"));
            case "fossil island wyverns": return Collections.singletonList(utility("Elemental shield / Mind shield / Ancient wyvern shield","Equip; elemental, mind or ancient wyvern shield also work for Ancient wyverns.","elemental shield","mind shield","ancient wyvern shield"));
            case "drakes": case "wyrms": case "hydras":
                return eliteKourend ? Collections.emptyList() : Collections.singletonList(utility("Boots of stone / Boots of brimstone / Granite boots","Wear inside Karuulm Slayer Dungeon; elite Kourend & Kebos Diary removes this requirement.","boots of stone","boots of brimstone","granite boots"));
            case "vampyres": return Collections.singletonList(utility("Ivandis flail / Blisterwood flail","For Vyrewatch sentinels; ordinary vampyres do not require a flail.","ivandis flail","blisterwood flail"));
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
            String worn = equipment.stream().filter(rule::matches).sorted().findFirst().orElse(null);
            String carried = inventory.stream().filter(rule::matches).sorted().findFirst().orElse(null);
            String stored = bankKnown ? bank.entrySet().stream()
                .filter(e -> e.getValue()>0 && rule.matches(e.getKey()))
                .map(Map.Entry::getKey).sorted().findFirst().orElse(null) : null;
            if (worn != null) out.add(new Row(rule, Status.EQUIPPED, worn));
            else if (carried != null) out.add(new Row(rule, Status.CARRIED, carried));
            else if (stored != null) out.add(new Row(rule, Status.BANK, stored));
            else out.add(new Row(rule, bankKnown ? Status.MISSING : Status.UNKNOWN, null));
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
