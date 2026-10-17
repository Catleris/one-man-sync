package lt.oneman.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.cluescrolls.clues.EmoteClue;
import net.runelite.client.plugins.cluescrolls.clues.item.ItemRequirement;

/** Offline classification; membership never proves the actual origin of a particular item. */
final class ClueItemCatalog
{
    static final class Tags
    {
        final Set<ClueTier> required, reward, common;
        final ClueTier scroll;
        Tags(Set<ClueTier> required, Set<ClueTier> reward, Set<ClueTier> common, ClueTier scroll)
        {
            this.required = Collections.unmodifiableSet(required); this.reward = Collections.unmodifiableSet(reward);
            this.common = Collections.unmodifiableSet(common); this.scroll = scroll;
        }
        Set<ClueTier> visible(boolean requirements, boolean rewards, boolean commons)
        {
            Set<ClueTier> result = EnumSet.noneOf(ClueTier.class);
            if (scroll != null) result.add(scroll);
            if (requirements) result.addAll(required);
            if (rewards) { result.addAll(reward); if (commons) result.addAll(common); }
            return result;
        }
        String kind(boolean requirements, boolean rewards, boolean commons)
        {
            if (scroll != null) return "Clue";
            boolean req = requirements && !required.isEmpty();
            boolean rew = rewards && (!reward.isEmpty() || (commons && !common.isEmpty()));
            return req && rew ? "Req / reward" : req ? "Req" : "Reward table";
        }
    }
    private static final class Requirement
    {
        final ClueTier tier;
        final ItemRequirement item;
        Requirement(ClueTier tier, ItemRequirement item) { this.tier = tier; this.item = item; }
    }
    private final List<Requirement> requirements = new ArrayList<>();
    private final Map<String, Set<ClueTier>> rewards = new HashMap<>(), commons = new HashMap<>();
    private final Map<Integer, Tags> cache = new HashMap<>();
    private int unresolvedSteps;
    ClueItemCatalog()
    {
        for (String line : lines("clue-emote-steps.tsv"))
        {
            String[] parts = line.split("\t", 2);
            EmoteClue clue = EmoteClue.forText(parts[1]);
            if (clue == null) { unresolvedSteps++; continue; }
            for (ItemRequirement requirement : clue.getItemRequirements()) requirements.add(new Requirement(ClueTier.valueOf(parts[0]), requirement));
        }
        for (String line : lines("clue-rewards.tsv"))
        {
            String[] parts = line.split("\t", 3);
            Map<String, Set<ClueTier>> target = parts[1].equals("COMMON") ? commons : rewards;
            target.computeIfAbsent(normal(parts[2]), key -> EnumSet.noneOf(ClueTier.class)).add(ClueTier.valueOf(parts[0]));
        }
    }
    int unresolvedSteps() { return unresolvedSteps; }
    void clearCache() { cache.clear(); }
    Tags classify(int id, String name)
    {
        Tags existing = cache.get(id);
        if (existing != null) return existing;
        Set<ClueTier> req = EnumSet.noneOf(ClueTier.class);
        for (Requirement requirement : requirements) if (requirement.item.fulfilledBy(id)) req.add(requirement.tier);
        // Charlie tasks are separate from emotes. Marking the item does not imply the gathering task is complete.
        switch (id)
        {
            case ItemID.PIKE: case ItemID.TROUT: case ItemID.LEATHER_ARMOUR: case ItemID.LEATHER_CHAPS:
            case ItemID.RAW_HERRING: case ItemID.RAW_TROUT: case ItemID.IRON_ORE: case ItemID.IRON_DAGGER:
                req.add(ClueTier.BEGINNER); break;
            default: break;
        }
        Tags tags = new Tags(req, rewardTiers(name, false), rewardTiers(name, true), ClueTier.scrollTier(name));
        if (cache.size() >= 4096) cache.clear();
        cache.put(id, tags); return tags;
    }
    Set<ClueTier> rewardTiers(String name, boolean common)
    {
        Set<ClueTier> result = EnumSet.noneOf(ClueTier.class);
        result.addAll((common ? commons : rewards).getOrDefault(normal(name), Collections.emptySet())); return result;
    }
    static String normal(String name) { return name.replace('\u00a0', ' ').replace('\u2019', '\'').trim().toLowerCase(Locale.ROOT); }
    private static List<String> lines(String file)
    {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            Objects.requireNonNull(ClueItemCatalog.class.getResourceAsStream(file), file), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null) if (!line.isEmpty() && !line.startsWith("#")) lines.add(line);
            return lines;
        }
        catch (IOException error) { throw new IllegalStateException("Cannot read bundled clue data", error); }
    }
}
