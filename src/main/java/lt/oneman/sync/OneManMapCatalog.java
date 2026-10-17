package lt.oneman.sync;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.cluescrolls.clues.emote.STASHUnit;

/** Offline static sites, independent of account unlocks and crop observations. */
final class OneManMapCatalog
{
    enum Group { STASH, FARMING }
    static final class Site
    {
        final String id, category, name;
        final List<WorldPoint> points;
        final Color color;
        Site(String id, String category, String name, Color color, WorldPoint... points)
        {
            this.id=id; this.category=category; this.name=name; this.color=color;
            this.points=Collections.unmodifiableList(Arrays.asList(points.clone()));
        }
        @Override public String toString() { return name; }
    }
    static List<Site> stashes()
    {
        Map<String, ClueTier> tiers=new HashMap<>();
        for(String[] row:rows("stash-tiers.tsv")) tiers.put(row[0],ClueTier.valueOf(row[1]));
        List<Site> sites=new ArrayList<>();
        for(STASHUnit unit:STASHUnit.values()) {
            ClueTier tier=tiers.get(unit.name());
            if(tier!=null) sites.add(new Site(unit.name(),tier.label,pretty(unit.name()),tier.color,unit.getWorldPoints()));
        }
        sites.sort(Comparator.comparing(s->s.name)); return Collections.unmodifiableList(sites);
    }
    static List<Site> farming()
    {
        List<Site> sites=new ArrayList<>();
        for(String[] row:rows("farming-locations.tsv")) {
            String category=row[0];
            Color color=category.equals("Tree")?new Color(85,190,105):category.equals("Herb")?new Color(85,205,170):new Color(170,205,80);
            sites.add(new Site(category+":"+row[1],category,row[1],color,
                new WorldPoint(Integer.parseInt(row[2]),Integer.parseInt(row[3]),Integer.parseInt(row[4]))));
        }
        return Collections.unmodifiableList(sites);
    }
    static List<Site> filter(List<Site> sites, String category)
    {
        if("All".equals(category)) return sites;
        List<Site> result=new ArrayList<>();
        for(Site site:sites) if(site.category.equals(category)) result.add(site);
        return Collections.unmodifiableList(result);
    }
    private static List<String[]> rows(String file)
    {
        List<String[]> rows=new ArrayList<>();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(
            Objects.requireNonNull(OneManMapCatalog.class.getResourceAsStream(file)),StandardCharsets.UTF_8))) {
            String line;
            while((line=reader.readLine())!=null) if(!line.startsWith("#")&&!line.trim().isEmpty()) rows.add(line.split("\t"));
        } catch(IOException error) { throw new IllegalStateException("Cannot load map catalogue "+file,error); }
        return rows;
    }
    private static String pretty(String name)
    {
        StringBuilder result=new StringBuilder();
        for(String word:name.replaceFirst("_\\d+$", "").replaceFirst("^_", "").toLowerCase(Locale.ROOT).split("_")) {
            if(result.length()>0) result.append(' ');
            if(!word.isEmpty()) result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
