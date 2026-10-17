package lt.oneman.sync;

import java.util.*;
import java.util.stream.Collectors;
import net.runelite.client.plugins.timetracking.farming.Produce;

/** Crop-to-patch types come from the installed RuneLite catalogue, never guessed from a substring. */
final class FarmingSeedCatalog
{
    static final class Seed
    {
        final String name,category,note;
        final Produce produce;
        final List<String> aliases;
        Seed(Produce produce,String name,String category,String note,String... extra)
        {
            this.produce=produce;this.name=name;this.category=category;this.note=note;
            List<String> aliases=new ArrayList<>(Arrays.asList(extra));aliases.add(name);aliases.add(produce.getName());
            this.aliases=aliases.stream().map(FarmingSeedCatalog::normal).distinct().collect(Collectors.toList());
        }
    }
    private static final Set<String> POTTED=new HashSet<>(Arrays.asList("TREE","FRUIT_TREE","HARDWOOD_TREE","CALQUAT","SPIRIT_TREE","CELASTRUS","REDWOOD"));
    private final List<Seed> seeds=new ArrayList<>();
    FarmingSeedCatalog()
    {
        for(Produce produce:Produce.values()) {
            if(produce.getPatchImplementation()==null || produce.name().equals("ANYHERB")) continue;
            String type=produce.getPatchImplementation().name();
            if(type.equals("COMPOST")||type.equals("BIG_COMPOST")) continue;
            String name=produce.getName()+(type.equals("FRUIT_TREE")&&!produce.name().equals("PINEAPPLE")?" tree seed":" seed");
            switch(produce.name()) {
                case "OAK":name="Acorn";break;
                case "SEAWEED":name="Seaweed spore";break;
                case "MUSHROOM":name="Mushroom spore";break;
                case "GOUTWEED":name="Gout tuber";break;
                case "SPIRIT_TREE":name="Spirit seed";break;
                case "CALQUAT":name="Calquat tree seed";break;
                case "REDWOOD":name="Redwood tree seed";break;
                case "CRYSTAL_TREE":name="Crystal acorn";break;
                case "ELKHORN_CORAL":name="Elkhorn coral fragment";break;
                case "PILLAR_CORAL":name="Pillar coral fragment";break;
                case "UMBRAL_CORAL":name="Umbral coral fragment";break;
                default:break;
            }
            String category=category(type);
            String note=POTTED.contains(type)?"Grow the seed in a filled plant pot and water it. Plant the resulting sapling in a "+category+" patch.":"Plant this material in a "+category+" patch. Check the Farming level and access requirements.";
            String sapling=POTTED.contains(type)?produce.getName()+" sapling":"";
            String extra=produce.name().equals("MUSHROOM")?"Bittercap mushroom spore":produce.name().equals("BELLADONNA")?"Nightshade":"";
            seeds.add(new Seed(produce,name,category,note,sapling,extra));
        }
        seeds.sort(Comparator.comparing(s->s.name));
    }
    List<Seed> search(String query)
    {
        String q=normal(query);if(q.isEmpty()) return Collections.emptyList();
        String[] words=q.split(" ");
        return seeds.stream().filter(seed->seed.aliases.stream().anyMatch(alias->Arrays.stream(words).allMatch(alias::contains)))
            .sorted(Comparator.<Seed>comparingInt(seed->seed.aliases.contains(q)?0:1).thenComparing(seed->seed.name)).collect(Collectors.toList());
    }
    static String category(String implementation)
    {
        switch(implementation) {
            case "HARDWOOD_TREE":return "Hardwood";
            case "FRUIT_TREE":return "Fruit Tree";
            case "SPIRIT_TREE":return "Spirit Tree";
            case "CRYSTAL_TREE":return "Crystal Tree";
            default:return implementation.charAt(0)+implementation.substring(1).toLowerCase(Locale.ROOT);
        }
    }
    static String normal(String text)
    {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\b(seeds?|saplings?|tree)\\b", " ").replaceAll("[^a-z0-9]+"," ").trim();
    }
}
