package lt.oneman.sync;

import java.util.*;
import java.util.regex.*;

final class SlayerLabSupplies
{
    static final class Counts
    {
        int food, doses, ammo, runes;
        final Map<String, Integer> potions = new TreeMap<>();
        final Map<String, Integer> runeStacks = new TreeMap<>();
        String warnings(SlayerLabConfig config)
        {
            List<String> warnings = new ArrayList<>();
            if (config.foodWarning() > 0 && food < config.foodWarning()) warnings.add("FOOD LOW (your threshold " + config.foodWarning() + ")");
            if (config.doseWarning() > 0 && doses < config.doseWarning()) warnings.add("POTION DOSES LOW (total)");
            if (config.combatStyle() == SlayerLabConfig.CombatStyle.RANGED && config.ammoWarning() > 0 && ammo < config.ammoWarning()) warnings.add("VISIBLE AMMO LOW");
            if (config.combatStyle() == SlayerLabConfig.CombatStyle.MAGIC && config.runeWarning() > 0 && runes < config.runeWarning()) warnings.add("VISIBLE RUNES LOW — check each required rune type");
            return String.join("\n", warnings);
        }
        String describe(SlayerLabConfig config)
        {
            String warning = warnings(config);
            return "Combat style: " + config.combatStyle() + " (change in plugin settings)\nFood items: " + food
                + "\nPotion doses: " + doses + "\nAmmo: " + ammo + "\nVisible runes: " + runes
                + "\n\n" + (warning.isEmpty() ? "No chosen threshold is crossed. This is not a combat-readiness assessment." : warning)
                + "\n\nPotion doses by item:\n" + potions + "\n\nRune stacks:\n" + runeStacks
                + "\n\nRune pouch, quiver storage, charged weapons and healing per food are not inspected. Potion totals include different potion types. Thresholds are your preferences, not mandatory supplies.";
        }
    }
    private static final Pattern DOSES = Pattern.compile("\\((\\d+)\\)$");
    static Counts count(Map<String, Integer> items, Set<String> edible, Set<String> drinkable)
    {
        Counts counts = new Counts();
        for (Map.Entry<String,Integer> item : items.entrySet())
        {
            String name = SlayerLabKnowledge.normalize(item.getKey()); int quantity = item.getValue();
            if (edible.contains(name)) counts.food += quantity;
            Matcher dose = DOSES.matcher(name);
            if (drinkable.contains(name) && dose.find())
            {
                int n = Integer.parseInt(dose.group(1));
                if (n >= 1 && n <= 6) { counts.doses += n * quantity; counts.potions.put(name, n * quantity); }
            }
            if (name.endsWith(" rune")) { counts.runes += quantity; counts.runeStacks.put(name, quantity); }
            if (name.matches(".*(?:arrow|arrows|bolt|bolts|dart|darts|javelin|javelins|throwing knife|throwing knives)(?: \\(.*\\))?$") && !name.contains("tips")) counts.ammo += quantity;
        }
        return counts;
    }
}
