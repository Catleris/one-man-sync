package lt.oneman.sync;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bundled source facts, never fetched from a network while playing. */
final class SlayerLabKnowledge
{
    static final class Entry
    {
        final String task, level, locations, equipment, other, article;
        final List<String> variants, targets;
        Entry(String[] cells)
        {
            task = cells[0]; level = cells[1]; locations = cells[2]; equipment = cells[3]; other = cells[4];
            variants = split(cells[5]); targets = split(cells[6]); article = cells[7];
        }
        private static List<String> split(String text)
        {
            List<String> values = new ArrayList<>();
            for (String v : text.split("; ")) if (!v.isEmpty() && !v.equals("Superior slayer monster")) values.add(v);
            return Collections.unmodifiableList(values);
        }
        boolean matchesNpc(String npc)
        {
            String name = normalize(npc);
            List<String> aliases = new ArrayList<>(targets);
            aliases.add(task);
            aliases.addAll(variants);
            for (String alias : aliases)
            {
                String a = normalize(alias).replaceFirst("^the ", "");
                if (name.equals(a)) return true;
                if (a.endsWith("s") && !a.endsWith("ss")) a = a.substring(0, a.length() - 1);
                if (!a.isEmpty() && (name.equals(a) || name.startsWith(a + " ") || name.endsWith(" " + a))) return true;
            }
            return false;
        }
        String overview()
        {
            return "Slayer level: " + (level.isEmpty() ? "see specific boss / assignment guide" : level)
                + "\nLocations: " + (locations.isEmpty() ? "see linked specific guide" : locations)
                + "\nSource equipment: " + equipment + "\nAccess / assignment notes: " + other
                + "\nSource notes can apply to specific locations or variants; not all are universal requirements.";
        }
    }
    private static final Map<String, Entry> ENTRIES = load();
    private static Map<String, Entry> load()
    {
        Map<String, Entry> entries = new LinkedHashMap<>();
        try (InputStream stream = SlayerLabKnowledge.class.getResourceAsStream("slayer-knowledge.tsv"))
        {
            if (stream == null) throw new IllegalStateException("Missing Slayer knowledge resource");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith("#") || line.isEmpty()) continue;
                String[] cells = line.split("\t", -1);
                if (cells.length != 8) throw new IllegalStateException("Invalid Slayer knowledge row");
                Entry entry = new Entry(cells);
                entries.put(normalize(entry.task), entry);
            }
        }
        catch (IOException error) { throw new IllegalStateException("Cannot read Slayer knowledge", error); }
        return Collections.unmodifiableMap(entries);
    }
    static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "); }
    static Entry find(String task) { return ENTRIES.get(normalize(task)); }
    static Collection<Entry> all() { return ENTRIES.values(); }
}
