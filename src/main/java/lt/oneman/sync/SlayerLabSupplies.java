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
            StringBuilder out = new StringBuilder("Supplies\nFood: " + food + "\nPotion doses: " + doses
                + "\nAmmo: " + ammo + "\nRunes: " + runes);
            if (!warning.isEmpty()) out.append("\n\nLow supplies\n").append(warning);
            appendItems(out, "Potions · doses", potions);
            appendItems(out, "Runes · quantity", runeStacks);
            out.append("\n\nInventory & equipped items only; stored runes/ammo and weapon charges excluded.");
            return out.toString();
        }
    }
    private static void appendItems(StringBuilder out, String heading, Map<String,Integer> items)
    {
        if (items.isEmpty()) return;
        out.append("\n\n").append(heading);
        items.forEach((name, count) -> out.append("\n").append(name).append(" · ").append(count));
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
