package lt.oneman.sync;

import java.util.*;

/** Manually selected reference advice; never driven by boss combat events. */
final class SlayerLabAdvice
{
    static final String REGULAR = "Regular task monsters";
    static final class Goal
    {
        final String item, reason;
        Goal(String item, String reason) { this.item=item; this.reason=reason; }
        @Override public String toString() { return item; }
    }
    static List<String> variants(String task)
    {
        LinkedHashSet<String> result = new LinkedHashSet<>(); result.add(REGULAR);
        SlayerLabKnowledge.Entry entry = SlayerLabKnowledge.find(task);
        if (entry != null) { result.addAll(entry.variants); result.addAll(entry.targets); }
        return new ArrayList<>(result);
    }
    static String tactics(String task, String variant)
    {
        String name = REGULAR.equals(variant) ? task : variant;
        String n = SlayerLabKnowledge.normalize(name);
        String advice;
        if (n.contains("alchemical hydra")) advice="95 Slayer and an active Hydra task. Choose a dedicated boss setup, floor-protection boots unless diary-exempt, and poison protection. The room's vents and phase changes are central to the encounter: study the strategy guide before entering. Regular-hydra gear notes are not a boss setup.";
        else if (n.contains("araxxor")) advice="92 Slayer and an eligible araxyte/spider task. Venom protection and crush equipment are useful preparation choices. Learn the egg/spider and phase mechanics in the strategy guide. Ordinary araxyte equipment notes are not a complete boss setup.";
        else if (n.contains("cerberus")) advice="91 Slayer and an eligible hellhound task. Bring a dedicated boss setup and ample supplies; spectral spirit shield is a specialised option, not an entry requirement. Learn the ghost and lava mechanics in the strategy guide.";
        else if (n.contains("sire")) advice="85 Slayer and an abyssal demon assignment. This is a multi-phase encounter; prepare suitable equipment for respiratory systems and the main fight. Learn access, transition and area-hazard mechanics first.";
        else if (n.contains("grotesque") || n.equals("dusk") || n.equals("dawn")) advice="75 Slayer, a gargoyle task and an unlocked rooftop bell. Prepare both ranged and melee equipment, and a gargoyle finishing tool. Learn the two-target phases before entering; a regular gargoyle setup is incomplete.";
        else if (n.contains("kraken")) advice="87 Slayer and a matching task. Prepare magic equipment, visible rune supplies or charged weapon supplies. Regular cave kraken and the Kraken boss have different activation procedures; use the guide for the selected version.";
        else if (n.contains("thermonuclear")) advice="93 Slayer and a smoke devil task; wear face protection. Use a dedicated boss setup and study the strategy guide. This single-target boss differs from ordinary smoke devils.";
        else if (n.contains("demonic gorilla")) advice="Monkey Madness II access. Bring equipment for two combat styles and venom/poison precautions appropriate to your route. Read the switching and movement mechanics before starting; these are harder than ordinary black demons.";
        else if (n.contains("basilisk knight")) advice="60 Slayer and The Fremennik Exiles. A mirror shield or V's shield is essential. Knights have much higher health and a different encounter than ordinary basilisks; choose them deliberately for basilisk-jaw hunting.";
        else if (n.contains("lizardman shaman")) advice="Prepare Shayzien armour protection. A Slayer helmet only gains the relevant armour effect after the hard Kourend & Kebos Diary AND speaking to Captain Cleive. Ranged equipment aids movement; study jumps, acid and summons before fighting.";
        else if (n.contains("wyvern")) advice="Use a suitable wyvern shield; ordinary anti-dragon shields and antifire alone are not equivalent. Choose the precise wyvern variant and check its Slayer/access requirements. Ancient wyverns require more preparation than lower Fossil Island variants.";
        else if (n.contains("hydra") || n.contains("wyrm") || n.contains("drake")) advice="Karuulm floor protection is required unless elite Kourend & Kebos Diary exempt. Select the correct chamber and monster variant; verify dragonfire/poison precautions in the individual guide. Alchemical Hydra is a distinct boss encounter.";
        else advice="Check the chosen monster's level, access, attack types and equipment before travelling. Variants can have different protection, combat difficulty and drops. The source list includes superior monsters: these are incidental spawns, not always a selectable hunting destination.";
        return name+"\n\n"+advice+"\n\nStatic reference only: this panel does not analyse attacks, suggest prayers or guide movement during combat.\n\nStrategy guide:\n"+strategy(name);
    }
    static String strategy(String name)
    {
        if (name.equals("Dawn") || name.equals("Dusk") || name.equals("Guardian of Dawn") || name.equals("Guardian of Dusk")) name="Grotesque Guardians";
        return "https://oldschool.runescape.wiki/w/" + java.net.URLEncoder.encode(name.replace(' ','_')+"/Strategies", java.nio.charset.StandardCharsets.UTF_8);
    }
    static List<Goal> goals(String task)
    {
        String n=SlayerLabKnowledge.normalize(task); List<Goal> result=new ArrayList<>();
        if(n.contains("abyssal")) result.add(new Goal("Abyssal whip","Melee weapon upgrade; regular abyssal demons."));
        if(n.contains("horror")) result.add(new Goal("Black mask","Component of a Slayer helmet; cave horrors."));
        if(n.contains("kraken")) { result.add(new Goal("Trident of the seas","Powered magic weapon; check charged and uncharged forms.")); result.add(new Goal("Kraken tentacle","Component for an abyssal tentacle.")); }
        if(n.contains("hydra")) { result.add(new Goal("Hydra leather","Alchemical Hydra drop used for ferocious gloves.")); result.add(new Goal("Hydra's claw","Alchemical Hydra drop used for a dragon hunter lance.")); }
        if(n.contains("smoke")) result.add(new Goal("Occult necklace","Magic necklace upgrade."));
        if(n.contains("basilisk")) result.add(new Goal("Basilisk jaw","Basilisk Knights drop used for a Neitiznot faceguard."));
        if(n.contains("black demon") || n.contains("monkey")) result.add(new Goal("Zenyte shard","Demonic gorilla drop for zenyte jewellery."));
        if(n.contains("lizardman")) result.add(new Goal("Dragon warhammer","Special-attack weapon upgrade; shamans."));
        if(n.contains("gargoyle")) result.add(new Goal("Granite maul","Special-attack weapon; ordinary gargoyles."));
        if(n.contains("kurask")) result.add(new Goal("Leaf-bladed battleaxe","Weapon option against kurask/turoth."));
        if(n.contains("spiritual")) result.add(new Goal("Dragon boots","Spiritual mage drop; melee boots upgrade."));
        if(n.contains("hellhound") || n.contains("cerberus")) result.add(new Goal("Primordial crystal","Cerberus drop used to upgrade dragon boots."));
        if(n.contains("araxy") || n.contains("araxxor")) result.add(new Goal("Araxyte fang","Araxxor drop used for an amulet of rancour."));
        return result;
    }
    static String loot(String task,String goal,SlayerLabAccount.Bank bank,SlayerLabJournal.Session session)
    {
        StringBuilder out=new StringBuilder("Ironman targets — choose a goal below. These are upgrade ideas, not a guaranteed drop or a personalised progression plan. Boss-only goals require the corresponding variant.\n\n");
        for(Goal g:goals(task)) out.append(g.item).append(" — ").append(g.reason).append("\n");
        out.append("\nSelected goal: ").append(goal.isEmpty()?"none":goal);
        if(!goal.isEmpty()) {
            String key=SlayerLabKnowledge.normalize(goal);
            out.append("\nBank snapshot count: ").append(bank.seen==0?"unknown":Integer.toString(bank.items.getOrDefault(key,0)));
            out.append("\nObserved this session: ").append(session==null?0:session.loot.getOrDefault(key,0));
        }
        out.append("\n\nDated bank counts may be stale; equipped items and alternate item forms are not included in the bank count.\nAttributed session loot:\n").append(session==null?"No session":session.loot);
        return out.toString();
    }
    static String comparison(SlayerLabLocations.Destination d)
    {
        String n=d.name.toLowerCase(Locale.ROOT);
        String cannon=n.contains("catacombs")||n.contains("tower")||n.contains("kraken")||n.contains("god wars")?"Cannon: NOT ALLOWED":n.contains("stronghold slayer")?"Cannon: ALLOWED (matching active task required)":"Cannon: not verified for this chamber — check location guide";
        String bank=n.contains("taverley")?"Falador/Taverley banking route":n.contains("tower")?"Canifis bank":n.contains("karuulm")?"Mount Karuulm bank":n.contains("devil")?"Castle Wars bank":n.contains("iorwerth")?"Prifddinas banks":n.contains("swamp")?"Lumbridge bank":n.contains("edgeville")?"Edgeville bank":n.contains("sewers")?"Varrock banks":"Choose a bank teleport; no nearest-bank path is calculated";
        String multi=n.contains("catacombs")?"Multi-combat":"Combat zones vary by room/variant; check before area attacks";
        return d+"\n"+d.access+"\n"+cannon+"\n"+multi+"\nBank: "+bank+"\nPvP: "+(d.wilderness?"Wilderness — items at risk":"This entrance is outside Wilderness")+"\n";
    }
}
