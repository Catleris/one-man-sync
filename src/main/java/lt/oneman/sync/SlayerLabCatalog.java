package lt.oneman.sync;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Curated preparation notes. Unknown tasks deliberately fall back to the Wiki. */
final class SlayerLabCatalog
{
    static final class Requirement
    {
        final String label;
        final boolean equip;
        final List<String> alternatives;
        Requirement(String label, boolean equip, String... alternatives)
        {
            this.label = label;
            this.equip = equip;
            this.alternatives = Arrays.asList(alternatives);
        }
        boolean matches(String item)
        {
            String name = normalize(item);
            for (String alternative : alternatives)
            {
                if (alternative.equals("*slayer helmet") && name.contains("slayer helmet")) return true;
                if (alternative.equals("*leaf-bladed") && name.startsWith("leaf-bladed ")) return true;
                if (name.equals(alternative)) return true;
            }
            return false;
        }
        String status(Set<String> inventory, Set<String> equipment)
        {
            if (equipment.stream().anyMatch(this::matches)) return "READY — equipped";
            if (inventory.stream().anyMatch(this::matches))
            {
                boolean onlyWeapon = inventory.stream().filter(this::matches).allMatch(n -> normalize(n).equals("brine sabre"));
                return equip || onlyWeapon ? "EQUIP — in inventory" : "READY — in inventory";
            }
            return "MISSING — not carried";
        }
    }
    static final class Guide
    {
        final String task, locations, advice;
        final List<Requirement> requirements;
        Guide(String task, String locations, String advice, Requirement... requirements)
        {
            this.task = task;
            this.locations = locations;
            this.advice = advice;
            this.requirements = Arrays.asList(requirements);
        }
        String wikiUrl() { return wiki(task); }
    }
    private static final Map<String, Guide> GUIDES = new LinkedHashMap<>();
    private static Requirement worn(String label, String... names) { return new Requirement(label, true, names); }
    private static Requirement carried(String label, String... names) { return new Requirement(label, false, names); }
    private static void add(String task, String locations, String advice, Requirement... requirements)
    { GUIDES.put(normalize(task), new Guide(task, locations, advice, requirements)); }
    static
    {
        add("Blue dragons", "Taverley Dungeon: blue dragon chamber. Enter south of Taverley; dusty key on the long route OR 70 Agility pipe shortcut. Heroes' Guild basement: Heroes' Quest required.",
            "Adults: prepare dragonfire protection appropriate to your setup (for example anti-dragon shield plus antifire). Baby blue dragons also count and do not breathe dragonfire. Food, ammo/runes and a bank teleport are recommended; quantities depend on your setup.");
        add("Gargoyles", "Slayer Tower: upper floor; basement is task-only. Reach the tower north-west of Canifis (Morytania access).", "75 Slayer. Carry a finishing tool; Gargoyle Smasher does not remove the tool requirement.", carried("Rock hammer / granite hammer / rock thrownhammer", "rock hammer", "granite hammer", "rock thrownhammer"));
        add("Banshees", "Slayer Tower: ground floor, north-west of Canifis. Twisted banshees: Catacombs of Kourend.", "15 Slayer. Wear sound protection before fighting.", worn("Earmuffs or Slayer helmet", "earmuffs", "*slayer helmet"));
        add("Aberrant spectres", "Slayer Tower: middle floor. Stronghold Slayer Cave: beneath the Gnome Stronghold. Deviant spectres: Catacombs of Kourend.", "60 Slayer. Wear nose protection; herb storage and food are useful.", worn("Nose peg or Slayer helmet", "nose peg", "*slayer helmet"));
        add("Dust devils", "Catacombs of Kourend: enter beneath the Kourend Castle statue. Smoke Dungeon: desert well west of Pollnivneach; requires starting Desert Treasure I.", "65 Slayer. Wear face protection. Food, ammo/runes and teleport are setup-dependent.", worn("Facemask or Slayer helmet", "facemask", "*slayer helmet"));
        add("Basilisks", "Fremennik Slayer Dungeon: east of Rellekka. Basilisk Knights: Jormungand's Prison after The Fremennik Exiles.", "40 Slayer for regular basilisks; knights require 60 Slayer and quest access. Slayer helmet does not replace the shield.", worn("Mirror shield or V's shield", "mirror shield", "v's shield"));
        add("Cockatrice", "Fremennik Slayer Dungeon: east of Rellekka, cockatrice chamber.", "25 Slayer. Equip a protective shield; Slayer helmet does not replace it.", worn("Mirror shield or V's shield", "mirror shield", "v's shield"));
        add("Rockslugs", "Fremennik Slayer Dungeon: east of Rellekka, second chamber. Lumbridge Swamp Caves: cave lighting and gas precautions required.", "20 Slayer. Salt is consumed on each finishing kill unless using a brine sabre. Check quantity for your task.", carried("Bag of salt or brine sabre", "bag of salt", "brine sabre"));
        add("Lizards", "Kharidian Desert: desert lizards near the area south-east of Shantay Pass. Check the task Wiki for exact spawns.", "22 Slayer for desert lizards. Ice coolers finish desert lizards; quantity depends on kills. Desert heat supplies recommended.", carried("Ice cooler", "ice cooler"));
        add("Turoth", "Fremennik Slayer Dungeon: east of Rellekka, turoth chamber.", "55 Slayer. Only leaf-bladed weapons, broad arrows/bolts, or Slayer Dart can damage them. For ranged/magic, also check a compatible weapon and ammo/runes manually.");
        add("Kurask", "Fremennik Slayer Dungeon: final chamber. Iorwerth Dungeon: requires Song of the Elves.", "70 Slayer. Only leaf-bladed weapons, broad arrows/bolts, or Slayer Dart can damage them; check the entire setup, not only ammunition.");
        add("Cave horrors", "Mos Le'Harmless Cave: requires Cabin Fever. Bring a light source.", "58 Slayer. Witchwood icon protects against the special attack; Slayer helmet does not replace it.", worn("Witchwood icon", "witchwood icon"));
        add("Wall beasts", "Lumbridge Swamp Caves: enter through the swamp hole south of Lumbridge. Use a safe light source and check cave hazards.", "Wear head protection before passing wall beasts.", worn("Spiny helmet or Slayer helmet", "spiny helmet", "*slayer helmet"));
        add("Cave crawlers", "Fremennik Slayer Dungeon: first chamber east of Rellekka.", "10 Slayer. Poison protection/antipoison recommended; bring food.");
        add("Bloodvelds", "Slayer Tower: middle floor (Morytania access). Stronghold Slayer Cave: beneath Gnome Stronghold. Mutated bloodvelds: Catacombs of Kourend.", "50 Slayer. Food and your chosen combat supplies recommended.");
        add("Nechryael", "Slayer Tower: upper floor. Greater nechryael: Catacombs of Kourend beneath Kourend Castle.", "80 Slayer. Food and combat supplies recommended; no special finishing tool.");
        add("Abyssal demons", "Slayer Tower: upper floor. Catacombs of Kourend: beneath Kourend Castle.", "85 Slayer. No special finishing tool. Food and combat supplies recommended.");
        add("Hill giants", "Edgeville Dungeon: brass key for the small hut west of Varrock; alternate entrance through Edgeville. Giants' Den: near Shayzien.", "Food, combat supplies and teleport recommended.");
        add("Moss giants", "Varrock Sewers: enter through the manhole east of Varrock Palace. Giants' Den: near Shayzien.", "Food, combat supplies and teleport recommended.");
        add("Fire giants", "Waterfall Dungeon: Waterfall Quest access, rope and Glarial's amulet for entry. Catacombs of Kourend: beneath Kourend Castle.", "Food and combat supplies recommended. Entry requirements depend on location.");
        add("Lesser demons", "Karamja Volcano: enter the volcano west of Musa Point. Catacombs of Kourend: beneath Kourend Castle.", "Food, combat supplies and teleport recommended.");
        add("Greater demons", "Chasm of Fire: north-west of Shayzien; combat areas are task-only. Catacombs of Kourend: beneath Kourend Castle.", "Food and combat supplies recommended. Wilderness variants have PvP risk; no Wilderness route is selected automatically.");
        add("Black demons", "Taverley Dungeon: dusty key or 70 Agility route to the deeper dungeon. Chasm of Fire: north-west of Shayzien, task-only combat areas.", "Demonic gorillas are an alternative after Monkey Madness II, with a different preparation setup. Food and combat supplies recommended.");
        add("Dagannoth", "Lighthouse basement: Horror from the Deep required. Catacombs of Kourend: beneath Kourend Castle.", "These notes cover regular dagannoth, not Dagannoth Kings. Food and combat supplies recommended.");
        add("Kalphites", "Kalphite Lair: west of Shantay Pass; rope for entry if not already attached. Kalphite Cave: east of Shantay Pass, task-only.", "Workers count toward the task. These notes do not cover the Kalphite Queen. Desert supplies and food recommended.");
    }
    static String normalize(String name) { return name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "); }
    static Guide find(String name) { return GUIDES.get(normalize(name)); }
    static Collection<Guide> all() { return Collections.unmodifiableCollection(GUIDES.values()); }
    static String wiki(String name)
    { return "https://oldschool.runescape.wiki/w/Special:Search?search=" + URLEncoder.encode("Slayer task/" + (name == null ? "" : name), StandardCharsets.UTF_8); }
}
