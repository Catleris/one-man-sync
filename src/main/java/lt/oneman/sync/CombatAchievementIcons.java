package lt.oneman.sync;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.lang.ref.WeakReference;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import net.runelite.api.SpriteID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.AsyncBufferedImage;

/** Local game portraits only. No Wiki downloads, account data or network requests. */
@Singleton
final class CombatAchievementIcons
{
    @Inject private SpriteManager sprites;
    @Inject private ItemManager items;
    private final Map<String,Icon> cached=new HashMap<>();
    private final Map<String,List<WeakReference<JLabel>>> waiting=new HashMap<>();
    private boolean active;
    private int generation;
    void startUp(){SwingUtilities.invokeLater(()->{active=true;generation++;});}
    void shutDown(){SwingUtilities.invokeLater(()->{active=false;generation++;waiting.clear();cached.clear();});}
    void addTo(String boss,JLabel label){
        label.setIcon(CombatHubStyle.fallback());label.setPreferredSize(new Dimension(40,38));label.setMinimumSize(new Dimension(40,38));
        label.setHorizontalAlignment(SwingConstants.CENTER);label.setToolTipText(boss);
        if(!active)return;
        String key=normal(boss);Icon known=cached.get(key);if(known!=null){label.setIcon(known);return;}
        int item=item(key);
        if(item>=0){AsyncBufferedImage image=items.getImage(item);if(image!=null)image.addTo(label);return;}
        int sprite=sprite(key);
        if(sprite<0){
            // A real category icon for future/unrecognised groups, rather than an empty portrait.
            AsyncBufferedImage image=items.getImage(ItemID.CA_BOOK);if(image!=null)image.addTo(label);return;
        }
        // Weak references avoid retaining detached cards while a cache sprite is loading.
        if(waiting.containsKey(key)){waiting.get(key).add(new WeakReference<>(label));return;}
        waiting.put(key,new ArrayList<>(Collections.singletonList(new WeakReference<>(label))));int requested=generation;
        sprites.getSpriteAsync(sprite,0,image->SwingUtilities.invokeLater(()->{
            if(!active||generation!=requested)return;
            List<WeakReference<JLabel>> labels=waiting.remove(key);if(image==null||labels==null)return;
            double factor=Math.min(32d/image.getWidth(),32d/image.getHeight());
            int w=Math.max(1,(int)Math.round(image.getWidth()*factor)),h=Math.max(1,(int)Math.round(image.getHeight()*factor));
            BufferedImage portrait=new BufferedImage(36,36,BufferedImage.TYPE_INT_ARGB);Graphics2D g=portrait.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(image,(36-w)/2,(36-h)/2,w,h,null);g.dispose();
            Icon icon=new ImageIcon(portrait);cached.put(key,icon);for(WeakReference<JLabel> ref:labels){JLabel target=ref.get();if(target!=null)target.setIcon(icon);}
        }));
    }
    private static String normal(String boss){return boss.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    static int item(String key){switch(normal(key)){
        case "aberrantspectre":case "aberrantspectres":return ItemID.SLAYERGUIDE_ABERRANTSPECTER;
        case "basiliskknight":case "basilisk":return ItemID.SLAYERGUIDE_BASILISK;
        case "brutus":return ItemID.COW_MASK;
        case "gryphon":case "gryphons":return ItemID.SLAYERGUIDE_GRYPHON;
        case "general":return ItemID.CA_BOOK;
        case "other":case "others":return ItemID.BOOK_OF_THE_DEAD;
        case "kurask":case "kurasks":return ItemID.SLAYERGUIDE_KURASK;
        case "demonicgorilla":case "demonicgorillas":return ItemID.ZENYTE_SHARD;
        case "fragmentofseren":case "thefragmentofseren":return ItemID.PRIF_SEREN_BOOK;
        case "galvek":return ItemID.DRAGONMASK_RED;
        case "glough":return ItemID.MM2_KRUK_GREEGREE;
        case "tormenteddemon":case "tormenteddemons":return ItemID.TORMENTED_SYNAPSE;
        case "dustdevil":case "dustdevils":return ItemID.SLAYERGUIDE_DUSTDEVIL;
        case "abyssaldemon":case "abyssaldemons":return ItemID.SLAYERGUIDE_ABYSSALDEMON;
        case "nechryael":case "nechryaels":return ItemID.SLAYERGUIDE_NECHRYAEL;
        case "darkbeast":case "darkbeasts":return ItemID.SLAYERGUIDE_DARK_BEAST;
        case "bloodveld":case "bloodvelds":return ItemID.SLAYERGUIDE_BLOODVELD;
        case "blackdragon":case "brutalblackdragon":return ItemID.DRAGONMASK_BLACK;
        case "gargoyle":return ItemID.SLAYERGUIDE_GARGOYLE;
        case "greaterdemon":return ItemID.GREATER_DEMON_MASK;
        case "lesserdemon":return ItemID.LESSER_DEMON_MASK;
        case "hellhound":return ItemID.POH_HELLHOUND;
        case "lizardmanshaman":return ItemID.LIZARDMAN_FANG;
        case "skeletalwyvern":return ItemID.SLAYERGUIDE_SKELETALWYVERN;
        default:return -1;
    }}
    static int sprite(String key){switch(normal(key)){
        case "abyssalsire":case "theabyssalsire":return SpriteID.HISCORE_ABYSSAL_SIRE;
        case "alchemicalhydra":case "thealchemicalhydra":return SpriteID.HISCORE_ALCHEMICAL_HYDRA;
        case "barrows":case "barrowsbrothers":return SpriteID.HISCORE_BARROWS_CHESTS;
        case "sarachnis":return SpriteID.HISCORE_SARACHNIS;
        case "shellbanegryphon":case "theshellbanegryphon":return net.runelite.api.gameval.SpriteID.IconBoss25x25.SHELLBANE_GRYPHON;
        case "madangel":case "themadangel":return net.runelite.api.gameval.SpriteID.IconBoss25x25.MAD_ANGEL;
        case "maggotking":case "themaggotking":return net.runelite.api.gameval.SpriteID.IconBoss25x25.MAGGOT_KING;
        case "doomofmokhaiotl":case "thedoomofmokhaiotl":return net.runelite.api.gameval.SpriteID.IconBoss25x25.DOOM_OF_MOKHAIOTL;
        case "tzhaarketrakschallenges":case "tzhaarketrakchallenges":case "tzhaarketrak":return SpriteID.HISCORE_TZTOK_JAD;
        case "bryophyta":return SpriteID.HISCORE_BRYOPHYTA;
        case "obor":return SpriteID.HISCORE_OBOR;
        case "araxxor":return SpriteID.HISCORE_ARAXXOR;
        case "amoxliatl":return SpriteID.HISCORE_AMOXLIATL;
        case "callisto":case "artio":return SpriteID.HISCORE_ARTIO_CALLISTO;
        case "vetion":case "calvarion":return SpriteID.HISCORE_CALVARION_VETION;
        case "venenatis":case "spindel":return SpriteID.HISCORE_SPINDEL_VENENATIS;
        case "cerberus":return SpriteID.HISCORE_CERBERUS;
        case "chaoselemental":return SpriteID.HISCORE_CHAOS_ELEMENTAL;
        case "chaosfanatic":return SpriteID.HISCORE_CHAOS_FANATIC;
        case "crazyarchaeologist":return SpriteID.HISCORE_CRAZY_ARCHAEOLOGIST;
        case "derangedarchaeologist":return SpriteID.HISCORE_DERANGED_ARCHAEOLOGIST;
        case "chambersofxeric":return SpriteID.HISCORE_CHAMBERS_OF_XERIC;
        case "chambersofxericcm":case "chambersofxericchallengemode":return SpriteID.HISCORE_CHAMBERS_OF_XERIC_CHALLENGE_MODE;
        case "commanderzilyana":return SpriteID.HISCORE_COMMANDER_ZILYANA;
        case "corporealbeast":return SpriteID.HISCORE_CORPOREAL_BEAST;
        case "dagannothprime":return SpriteID.HISCORE_DAGANNOTH_PRIME;
        case "dagannothrex":return SpriteID.HISCORE_DAGANNOTH_REX;
        case "dagannothsupreme":return SpriteID.HISCORE_DAGANNOTH_SUPREME;
        case "dukesucellus":return SpriteID.HISCORE_DUKE_SUCELLUS;
        case "generalgraardor":return SpriteID.HISCORE_GENERAL_GRAARDOR;
        case "giantmole":case "thegiantmole":return SpriteID.HISCORE_GIANT_MOLE;
        case "grotesqueguardians":return SpriteID.HISCORE_GROTESQUE_GUARDIANS;
        case "hespori":return SpriteID.HISCORE_HESPORI;
        case "kalphitequeen":return SpriteID.HISCORE_KALPHITE_QUEEN;
        case "kingblackdragon":return SpriteID.HISCORE_KING_BLACK_DRAGON;
        case "kraken":return SpriteID.HISCORE_KRAKEN;
        case "kreearra":return SpriteID.HISCORE_KREEARRA;
        case "kriltsutsaroth":return SpriteID.HISCORE_KRIL_TSUTSAROTH;
        case "mimic":case "themimic":return SpriteID.HISCORE_MIMIC;
        case "nex":return SpriteID.HISCORE_NEX;
        case "nightmare":case "thenightmare":case "phosanisnightmare":return SpriteID.HISCORE_NIGHTMARE;
        case "phantommuspah":return SpriteID.HISCORE_PHANTOM_MUSPAH;
        case "scorpia":return SpriteID.HISCORE_SCORPIA;
        case "scurrius":return SpriteID.HISCORE_SCURRIUS;
        case "skotizo":return SpriteID.HISCORE_SKOTIZO;
        case "tempoross":return SpriteID.HISCORE_TEMPOROSS;
        case "crystallinehunllef":case "thegauntlet":case "gauntlet":return SpriteID.HISCORE_THE_GAUNTLET;
        case "corruptedhunllef":case "corruptedgauntlet":case "thecorruptedgauntlet":return SpriteID.HISCORE_THE_CORRUPTED_GAUNTLET;
        case "theatreofblood":case "theatreofbloodentrymode":case "theatreofbloodhardmode":return SpriteID.HISCORE_THEATRE_OF_BLOOD;
        case "thermonuclearsmokedevil":return SpriteID.HISCORE_THERMONUCLEAR_SMOKE_DEVIL;
        case "tombsofamascut":case "tombsofamascutentrymode":return SpriteID.HISCORE_TOMBS_OF_AMASCUT;
        case "tombsofamascutexpertmode":return SpriteID.HISCORE_TOMBS_OF_AMASCUT_EXPERT;
        case "tzhaarjadtzok":case "tztokjad":case "fightcaves":return SpriteID.HISCORE_TZTOK_JAD;
        case "tzkalzuk":case "inferno":return SpriteID.HISCORE_TZKAL_ZUK;
        case "vardorvis":return SpriteID.HISCORE_VARDORVIS;
        case "theleviathan":case "leviathan":return SpriteID.HISCORE_THE_LEVIATHAN;
        case "thewhisperer":case "whisperer":return SpriteID.HISCORE_THE_WHISPERER;
        case "vorkath":return SpriteID.HISCORE_VORKATH;
        case "wintertodt":return SpriteID.HISCORE_WINTERTODT;
        case "zalcano":return SpriteID.HISCORE_ZALCANO;
        case "zulrah":return SpriteID.HISCORE_ZULRAH;
        case "moonsofperil":case "perilousmoons":case "lunarchests":return SpriteID.HISCORE_LUNAR_CHESTS;
        case "fortiscolosseum":case "solheredit":return SpriteID.HISCORE_SOL_HEREDIT;
        case "thehueycoatl":case "hueycoatl":return SpriteID.HISCORE_THE_HUEYCOATL;
        case "royaltitans":return SpriteID.HISCORE_ROYAL_TITANS;
        case "yama":return SpriteID.HISCORE_YAMA;
        default:return -1;
    }}
}
