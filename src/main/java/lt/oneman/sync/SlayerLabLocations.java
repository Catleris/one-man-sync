package lt.oneman.sync;

import java.util.*;
import net.runelite.api.coords.WorldPoint;

/** Entrance coordinates from RuneLite's DungeonLocation; access is not auto-verified. */
final class SlayerLabLocations
{
    static final class Destination
    {
        final String name, access;
        final WorldPoint entrance;
        final boolean wilderness;
        final Set<String> aliases;
        Destination(String name, int x, int y, String access, boolean wilderness, String... aliases)
        {
            this.name = name;
            this.access = access;
            this.entrance = new WorldPoint(x, y, 0);
            this.wilderness = wilderness;
            this.aliases = new HashSet<>();
            this.aliases.add(normalize(name));
            for (String alias : aliases) this.aliases.add(normalize(alias));
        }
        @Override public String toString() { return (wilderness ? "[WILDERNESS] " : "") + name; }
        boolean matches(String assigned) { return aliases.contains(normalize(assigned)); }
    }
    private static final Destination TAVERLEY = new Destination("Taverley Dungeon", 2883, 3397, "Blue dragons: dusty key OR 70 Agility pipe. Deeper routes depend on the target chamber.", false);
    private static final Destination HEROES = new Destination("Heroes' Guild", 2891, 3507, "Heroes' Quest required. Blue dragon in the basement.", false, "Heroes' Guild basement");
    private static final Destination TOWER = new Destination("Slayer Tower", 3416, 3535, "Morytania access. This marker is the tower basement entrance; for upper floors use the tower stairs. See task notes for the floor.", false, "Morytania Slayer Tower");
    private static final Destination CATACOMBS = new Destination("Catacombs of Kourend", 1636, 3673, "Enter beneath Kourend Castle statue; use task notes to locate the correct chamber.", false, "Kourend Catacombs", "the Catacombs of Kourend");
    private static final Destination FREMENNIK = new Destination("Fremennik Slayer Dungeon", 2796, 3615, "East of Rellekka. See task notes for the chamber and protective equipment.", false, "Fremennik Slayer Cave");
    private static final Destination STRONGHOLD = new Destination("Stronghold Slayer Cave", 2427, 3424, "Beneath Gnome Stronghold. Check task-only areas before entering.", false, "Stronghold Slayer Dungeon", "Gnome Stronghold");
    private static final Destination SMOKE = new Destination("Smoke Dungeon", 3309, 2962, "Desert Treasure I started; face protection and desert travel preparation.", false);
    private static final Destination SWAMP = new Destination("Lumbridge Swamp Caves", 3168, 3172, "Bring a safe light source; cave gas and entrance hazards. See task notes.", false, "Lumbridge Swamp Cave");
    private static final Destination HORRORS = new Destination("Mos Le'Harmless Cave", 3747, 2973, "Cabin Fever access; light source and appropriate cave horror protection.", false, "Mos Le'Harmless", "Mos Le Harmless Cave");
    private static final Destination IORWERTH = new Destination("Iorwerth Dungeon", 3224, 6044, "Song of the Elves; enter from Prifddinas.", false);
    private static final Destination EDGEVILLE = new Destination("Edgeville Dungeon", 3096, 3469, "Stairs in Edgeville; navigate to hill giants. The alternate Varrock hut entrance uses a brass key.", false);
    private static final Destination DEN = new Destination("Giants' Den", 1419, 3588, "Near Shayzien; select the correct giant type inside.", false);
    private static final Destination SEWERS = new Destination("Varrock Sewers", 3236, 3458, "Manhole east of Varrock Palace; navigate to moss giants inside.", false);
    private static final Destination CHASM = new Destination("Chasm of Fire", 1432, 3670, "North-west of Shayzien; combat areas are task-only. Descend to the correct demon floor.", false);
    private static final Destination VOLCANO = new Destination("Karamja Volcano", 2855, 3168, "Enter the volcano dungeon west of Musa Point.", false, "Karamja Dungeon");
    private static final Destination LIGHTHOUSE = new Destination("Lighthouse", 2508, 3644, "Horror from the Deep; descend to the basement.", false, "Lighthouse basement");
    private static final Destination KALPHITE_CAVE = new Destination("Kalphite Cave", 3319, 3122, "East of Shantay Pass; task-only. Desert travel supplies recommended.", false);
    private static final Destination KALPHITE_LAIR = new Destination("Kalphite Lair", 3226, 3108, "West of Shantay Pass; rope needed unless an entry rope is attached. Workers count; this route is not to the Queen.", false);
    private static final Destination WILD = new Destination("Wilderness Slayer Cave", 3259, 3666, "South entrance. PvP area: players can attack you and items may be lost. Select deliberately; never selected automatically.", true);
    private static final Destination KARUULM = new Destination("Karuulm Slayer Dungeon",1308,3807,"Floor-protection boots unless elite Kourend & Kebos Diary exempt; check Slayer level and task-only chambers.",false);
    private static final Destination KRAKEN = new Destination("Kraken Cove",2277,3611,"87 Slayer and a matching task. Select regular cave kraken or Kraken boss deliberately.",false);
    private static final Destination DEVIL = new Destination("Smoke Devil Dungeon",2411,3061,"93 Slayer, matching task and face protection.",false);
    private static final Destination ICE = new Destination("Asgarnian Ice Dungeon",3007,3150,"Wyvern chamber: 72 Slayer and appropriate wyvern shield.",false);
    private static final Destination WYVERN = new Destination("Wyvern Cave",3745,3779,"Bone Voyage; Slayer level depends on variant; appropriate wyvern shield.",false);
    private static final Destination JORMUNGAND = new Destination("Jormungand's Prison",2464,4012,"The Fremennik Exiles; 60 Slayer for knights and protective shield.",false);
    private static final Destination SPIDER = new Destination("Morytania Spider Cave",3656,3409,"Morytania access; 92 Slayer for araxytes/Araxxor. Venom precautions.",false,"Morytania Spider Nest");
    private static final Destination ANCIENT = new Destination("Ancient Cavern",2511,3508,"Barbarian training access; dangerous dungeon. Appropriate dragonfire protection for dragon targets.",false);
    private static final Destination BRIMHAVEN = new Destination("Brimhaven Dungeon",2743,3154,"Entry fee unless exempt; axe for vines. Chamber access depends on route.",false);
    private static final Map<String, List<Destination>> TASKS = new HashMap<>();
    private static void add(String task, Destination... places) { TASKS.put(SlayerLabCatalog.normalize(task), Collections.unmodifiableList(Arrays.asList(places))); }
    static
    {
        add("Blue dragons", TAVERLEY, HEROES);
        add("Gargoyles", TOWER);
        add("Banshees", TOWER, CATACOMBS);
        add("Aberrant spectres", TOWER, STRONGHOLD, CATACOMBS);
        add("Dust devils", CATACOMBS, SMOKE);
        add("Basilisks", FREMENNIK);
        add("Cockatrice", FREMENNIK);
        add("Rockslugs", FREMENNIK, SWAMP);
        add("Turoth", FREMENNIK);
        add("Kurask", FREMENNIK, IORWERTH);
        add("Cave horrors", HORRORS);
        add("Wall beasts", SWAMP);
        add("Cave crawlers", FREMENNIK);
        add("Bloodvelds", TOWER, STRONGHOLD, CATACOMBS);
        add("Nechryael", TOWER, CATACOMBS);
        add("Abyssal demons", TOWER, CATACOMBS);
        add("Hill giants", EDGEVILLE, DEN);
        add("Moss giants", SEWERS, DEN);
        add("Fire giants", CATACOMBS);
        add("Lesser demons", VOLCANO, CATACOMBS);
        add("Greater demons", CHASM, CATACOMBS, WILD);
        add("Black demons", TAVERLEY, CHASM, WILD);
        add("Dagannoth", LIGHTHOUSE, CATACOMBS);
        add("Kalphites", KALPHITE_CAVE, KALPHITE_LAIR);
        add("Hydras",KARUULM); add("Wyrms",KARUULM); add("Drakes",KARUULM);
        add("Cave kraken",KRAKEN); add("Smoke devils",DEVIL); add("Skeletal wyverns",ICE);
        add("Fossil Island wyverns",WYVERN); add("Araxytes",SPIDER);
        add("Basilisks",FREMENNIK,JORMUNGAND);
        Destination[] known = {TAVERLEY,HEROES,TOWER,CATACOMBS,FREMENNIK,STRONGHOLD,SMOKE,SWAMP,HORRORS,IORWERTH,EDGEVILLE,DEN,SEWERS,CHASM,VOLCANO,LIGHTHOUSE,KALPHITE_CAVE,KALPHITE_LAIR,WILD,KARUULM,KRAKEN,DEVIL,ICE,WYVERN,JORMUNGAND,SPIDER,ANCIENT,BRIMHAVEN};
        for (SlayerLabKnowledge.Entry entry : SlayerLabKnowledge.all())
        {
            List<Destination> existing = new ArrayList<>(TASKS.getOrDefault(normalize(entry.task), Collections.emptyList()));
            for (String place : entry.locations.split("; "))
                for (Destination destination : known)
                    if (destination.matches(place) && !existing.contains(destination)) existing.add(destination);
            if (!existing.isEmpty()) TASKS.put(normalize(entry.task), Collections.unmodifiableList(existing));
        }
    }
    static List<Destination> forTask(String task, String assignedLocation)
    {
        List<Destination> available = TASKS.getOrDefault(SlayerLabCatalog.normalize(task), Collections.emptyList());
        if (normalize(assignedLocation).isEmpty()) return available;
        List<Destination> filtered = new ArrayList<>();
        for (Destination destination : available) if (destination.matches(assignedLocation)) filtered.add(destination);
        return Collections.unmodifiableList(filtered);
    }
    private static String normalize(String value)
    {
        String s = SlayerLabCatalog.normalize(value).replace("’", "'");
        return s.startsWith("the ") ? s.substring(4) : s;
    }
    static String direction(WorldPoint from, WorldPoint to)
    {
        if (from.getPlane() != to.getPlane()) return "different floor";
        int dx = to.getX() - from.getX(), dy = to.getY() - from.getY();
        if (Math.abs(dx) <= 5 && Math.abs(dy) <= 5) return "at entrance";
        String northSouth = Math.abs(dy) > 5 ? (dy > 0 ? "N" : "S") : "";
        String eastWest = Math.abs(dx) > 5 ? (dx > 0 ? "E" : "W") : "";
        return northSouth + eastWest;
    }
}
