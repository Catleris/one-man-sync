package lt.oneman.sync;

import java.util.*;

/** Local attributed-loot session. Loot events do not establish inventory pickup or RDT provenance. */
final class SlayerSessionDrops
{
    private static final Set<String> SUPERIORS=new HashSet<>(Arrays.asList(
        "crushing hand","chasm crawler","screaming banshee","screaming twisted banshee","giant rockslug",
        "cockathrice","flaming pyrelord","infernal pyrelord","monstrous basilisk","basilisk sentinel","malevolent mage",
        "insatiable bloodveld","insatiable mutated bloodveld","vitreous jelly","vitreous warped jelly","vitreous chilled jelly",
        "cave abomination","abhorrent spectre","repugnant spectre","choke devil","king kurask","marble gargoyle",
        "nechryarch","greater abyssal demon","night beast","nuclear smoke devil","spiked turoth","shadow wyrm",
        "guardian drake","colossal hydra","dreadborn araxyte","ancient custodian","elder aquanite","dire gryphon",
        "blood-starved venator","mutated terrorbird","mutated tortoise"));
    private static final Set<String> NOTABLE=new HashSet<>(Arrays.asList(
        "imbued heart","eternal gem","dust battlestaff","mist battlestaff","eternal crystal","pegasian crystal",
        "primordial crystal","smouldering stone","abyssal whip","abyssal dagger","abyssal head","granite maul",
        "black mask","black mask (10)","leaf-bladed sword","leaf-bladed battleaxe","dark bow","trident of the seas",
        "uncharged trident","kraken tentacle","occult necklace","smoke battlestaff","dragon thrownaxe","dragon knife",
        "hydra's claw","hydra leather","hydra tail","alchemical hydra heads","brittle key","granite hammer",
        "dragon harpoon","drake's claw","drake's tooth","dragon sword"));
    static final class Drop {
        final String item, source;
        final int quantity;
        final long time;
        Drop(String item,int quantity,String source,long time) {this.item=item;this.quantity=quantity;this.source=source;this.time=time;}
    }
    private long started;
    private int superiors, notableDrops;
    private final Deque<Drop> recent=new ArrayDeque<>();
    private final LinkedHashSet<String> seen=new LinkedHashSet<>();
    void start(long now) {if(started==0) started=now;}
    void reset(long now) {started=now;superiors=0;notableDrops=0;recent.clear();seen.clear();}
    static boolean superior(String name) {return SUPERIORS.contains(SlayerLabCatalog.normalize(name));}
    boolean loot(String eventKey,String source,Map<String,Integer> items,boolean matchesTask,long now)
    {
        boolean superior=superior(source);
        if(!matchesTask && !superior) return false;
        if(!seen.add(eventKey)) return false;
        while(seen.size()>512) seen.remove(seen.iterator().next());
        start(now);
        if(superior) superiors++;
        for(Map.Entry<String,Integer> item:items.entrySet()) {
            if(item.getValue()<=0 || !NOTABLE.contains(SlayerLabCatalog.normalize(item.getKey()))) continue;
            notableDrops++;
            recent.addFirst(new Drop(item.getKey(),item.getValue(),source,now));
            while(recent.size()>5) recent.removeLast();
        }
        return true;
    }
    int superiorKills() {return superiors;}
    int notableDrops() {return notableDrops;}
    List<Drop> recent() {return Collections.unmodifiableList(new ArrayList<>(recent));}
    String summary(long now)
    {
        if(started==0) return "Waiting for a Slayer task.\nSuperior kills: 0 · Notable drops: 0";
        long seconds=Math.max(0,now-started)/1000;
        StringBuilder out=new StringBuilder("Superior kills: "+superiors+" · "+seconds/3600+"h "+(seconds/60)%60+"m\nNotable drops: "+notableDrops);
        if(recent.isEmpty()) out.append("\nNo notable loot observed this session.");
        int shown=0;
        for(Drop drop:recent) {
            if(shown++==3) break;
            out.append("\n").append(drop.item).append(" × ").append(drop.quantity).append(" · ").append(Math.max(0,now-drop.time)/60000).append("m ago");
        }
        return out.append("\nLoot observed · pickup not verified").toString();
    }
    String detail()
    {
        StringBuilder out=new StringBuilder("Last five notable loot observations. Session time includes banking/travel; resets on logout or Reset session.");
        for(Drop drop:recent) out.append("\n").append(drop.item).append(" × ").append(drop.quantity).append(" — ").append(drop.source);
        return out.toString();
    }
}
