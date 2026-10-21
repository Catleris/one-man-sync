package lt.oneman.sync;

import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.config.RequestFocusType;
import net.runelite.client.config.FlashNotification;
import java.awt.Color;

@ConfigGroup("one-man-sync")
public interface OneManSyncConfig extends SlayerLabConfig
{
    @ConfigSection(name="Sync / Account", description="Website connection and upload interval", position=0, closedByDefault=true)
    String syncSection = "syncSection";

    @ConfigSection(name="Roadmap / Screenshots", description="Companion preferences and milestone captures", position=1, closedByDefault=true)
    String roadmapSection = "roadmapSection";

    @ConfigSection(name="Slayer / Alerts / Supplies", description="Task hints, Superior notifications and supply warnings", position=2, closedByDefault=true)
    String slayerSection = "slayerSection";

    @ConfigSection(name="Farming", description="Farming and birdhouse notifications", position=3, closedByDefault=true)
    String farmingSection = "farmingSection";

    @ConfigSection(name="Clue Hub", description="Clue labels, tier dots and item categories", position=4, closedByDefault=true)
    String clueSection = "clueSection";

    @ConfigSection(name="Monsters / Lizardman Shaman", description="The same Shaman preferences used by Monsters Hub", position=5, closedByDefault=true)
    String monstersSection = "monstersSection";

    @ConfigSection(name="Unlocks Hub", description="Account unlock catalogue and journal visibility", position=6, closedByDefault=true)
    String unlocksSection = "unlocksSection";
    @ConfigSection(name="Optional views", description="Extra planning and history views", position=7, closedByDefault=true)
    String optionalViewsSection = "optionalViewsSection";
    @ConfigItem(keyName="companionRoadmapVisible", name="Show Roadmap", description="Add the optional Next / Prep planner to the Hub selector.", position=120, section=optionalViewsSection)
    default boolean companionRoadmapVisible() { return false; }
    @ConfigItem(keyName="slayerHistoryVisible", name="Show Slayer session / history", description="Show optional Session and History tabs inside Slayer Hub.", position=121, section=optionalViewsSection)
    default boolean slayerHistoryVisible() { return false; }

    @ConfigItem(keyName="unlocksQuestJournal", name="Include quest journal entries", description="Also include every quest/miniquest milestone exposed by RuneLite. Useful unlock categories remain visible without this.", position=110, section=unlocksSection)
    default boolean unlocksQuestJournal() { return false; }

    @ConfigItem(
        keyName = "enabled",
        name = "Enable OneMan Sync",
        description = "Sends your OSRS display name, skill levels/XP, quest states, Achievement Diaries, Combat Achievements, boss kill counts, Collection Log events, pet/personal-best/loot events, bank item IDs/names/quantities when you open your bank, Clue Scroll STASH built/filled states, current inventory/equipment and selected storage snapshots, Slayer state, POH feature observations, currencies and daily-account timers to oneman.lt.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 0, section=syncSection)
    default boolean enabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "syncKey",
        name = "Sync Key",
        description = "Generate this in oneman.lt -> Account -> RuneLite Sync. This is a OneMan API token, not a Jagex credential.",
        secret = true,
        position = 1, section=syncSection)
    default String syncKey()
    {
        return "";
    }

    @Range(min = 2, max = 30)
    @ConfigItem(
        keyName = "intervalMinutes",
        name = "Full sync every",
        description = "Periodic full sync interval in minutes.",
        position = 2, section=syncSection)
    default int intervalMinutes()
    {
        return 5;
    }
    @ConfigItem(keyName="highlightGoalItems", name="Highlight goal items", description="Outline curated preparation items in your bank.", position=3, section=roadmapSection)
    default boolean highlightGoalItems(){return true;}

    @ConfigItem(keyName="companionLanguage", name="Companion language", description="Language for roadmap reasons: LT or EN.", position=4, section=roadmapSection)
    default String companionLanguage(){return "EN";}

    @ConfigItem(keyName="milestoneScreenshots", name="Save milestone screenshots", description="Save local 99-level and quest-completion screenshots. Images include the visible game window.", position=5, section=roadmapSection)
    default boolean milestoneScreenshots(){return false;}

    @ConfigItem(keyName="uploadMilestoneScreenshots", name="Upload milestone screenshots", description="Send captured screenshots to your OneMan account. Requires local capture and OneMan Sync enabled.", warning="This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers", position=6, section=roadmapSection)
    default boolean uploadMilestoneScreenshots(){return false;}
    @ConfigItem(keyName="slayerRequirementsOverlay", name="Slayer item hints", description="Show task protection and finishing items on the left: red missing, purple in last observed bank, green carried/equipped, grey bank unknown.", position=7, section=slayerSection)
    default boolean slayerRequirementsOverlay(){return true;}

    @ConfigItem(keyName="clueHubEnabled", name="Clue Hub (test)", description="Local clue item tiers and hub. Does not require website sync.", position=80, section=clueSection)
    default boolean clueHubEnabled() { return true; }
    @ConfigItem(keyName="clueGroundMarkers", name="Clue ground labels", description="Separate ground labels with tier dots after item names. Adjust height if Ground Items overlaps.", position=81, section=clueSection)
    default boolean clueGroundMarkers() { return true; }
    @ConfigItem(keyName="clueWidgetMarkers", name="Clue inventory / bank dots", description="Tier dots on inventory, equipment and bank item icons.", position=82, section=clueSection)
    default boolean clueWidgetMarkers() { return true; }
    @ConfigItem(keyName="clueMenuMarkers", name="Clue item menu labels", description="Append clue role and coloured tier dots to item menu text.", position=83, section=clueSection)
    default boolean clueMenuMarkers() { return true; }
    @ConfigItem(keyName="clueRequiredItems", name="Clue required items", description="Mark items needed for emote steps and beginner Charlie tasks. Sherlock skill tasks are not indexed in this test.", position=84, section=clueSection)
    default boolean clueRequiredItems() { return true; }
    @ConfigItem(keyName="clueRewardItems", name="Clue reward table items", description="Mark items in special/shared clue reward tables. Possible sources, not the actual origin of an individual item.", position=85, section=clueSection)
    default boolean clueRewardItems() { return true; }
    @ConfigItem(keyName="clueCommonRewards", name="Include common clue rewards", description="Also mark ordinary supplies such as coins and runes listed in clue reward tables; can be noisy.", position=86, section=clueSection)
    default boolean clueCommonRewards() { return false; }
    @Range(min=0, max=200)
    @ConfigItem(keyName="clueGroundLabelHeight", name="Clue ground label height", description="Height above a ground item's layer; adjust to separate from Ground Items labels.", position=87, section=clueSection)
    default int clueGroundLabelHeight() { return 70; }

    @ConfigItem(keyName="monstersProfile", name="Monsters Hub profile", description="Select a local monster helper. Works without a Slayer task or website sync.", position=90, section=monstersSection)
    default MonstersProfile monstersProfile() { return MonstersProfile.NONE; }
    @ConfigItem(keyName="monstersShamanTiles", name="Shaman minion tiles", description="Mark only the current occupied tile of observed Shaman spawns. No explosion radius or prediction.", position=91, section=monstersSection)
    default boolean monstersShamanTiles() { return true; }
    @net.runelite.client.config.Alpha
    @ConfigItem(keyName="monstersShamanColor", name="Shaman tile colour", description="Colour of current minion tile outlines.", position=92, section=monstersSection)
    default Color monstersShamanColor() { return new Color(190,110,240,220); }

    @ConfigItem(keyName="monstersShamanOutline", name="Shaman model outline", description="Outline already visible Shaman spawns.", position=93, section=monstersSection)
    default boolean monstersShamanOutline() { return true; }
    @ConfigItem(keyName="monstersShamanFill", name="Shaman model fill", description="Tint the silhouette of visible Shaman spawns.", position=94, section=monstersSection)
    default boolean monstersShamanFill() { return true; }
    @ConfigItem(keyName="monstersShamanLabel", name="Shaman Spawn label", description="Show Spawn above each visible minion.", position=95, section=monstersSection)
    default boolean monstersShamanLabel() { return true; }
    @ConfigItem(keyName="monstersShamanModelColor", name="Shaman model colour", description="Separate colour for the model outline, fill and Spawn label.", position=96, section=monstersSection)
    default Color monstersShamanModelColor() { return new Color(220,110,255); }
    @Range(min=0, max=100)
    @ConfigItem(keyName="monstersShamanTileOpacity", name="Shaman tile opacity %", description="Tile visibility; 0 is invisible and 100 is fully opaque. Multiplies the saved tile colour alpha.", position=97, section=monstersSection)
    default int monstersShamanTileOpacity() { return 100; }
    @Range(min=0, max=100)
    @ConfigItem(keyName="monstersShamanModelOpacity", name="Shaman outline opacity %", description="Visibility of model outline and Spawn label.", position=98, section=monstersSection)
    default int monstersShamanModelOpacity() { return 100; }
    @Range(min=0, max=100)
    @ConfigItem(keyName="monstersShamanFillOpacity", name="Shaman fill opacity %", description="Separate visibility of the model silhouette fill.", position=99, section=monstersSection)
    default int monstersShamanFillOpacity() { return 25; }
    @Range(min=1, max=6)
    @ConfigItem(keyName="monstersShamanOutlineWidth", name="Shaman outline width", description="Model outline width in pixels.", position=100, section=monstersSection)
    default int monstersShamanOutlineWidth() { return 2; }

    @ConfigItem(keyName="monstersShamanPulse", name="Shaman gentle outline pulse", description="Constant 2.4-second cosmetic cycle shared by all visible spawns; never speeds up and is unrelated to explosion timing.", position=101, section=monstersSection)
    default boolean monstersShamanPulse() { return false; }

    // RuneLite initializes defaults only for methods declared on this config.
    @ConfigItem(keyName="combatStyle", name="Supplies combat style", description="Controls ammunition or rune quantity warnings; does not select combat actions.", position=10, section=slayerSection)
    default CombatStyle combatStyle() { return CombatStyle.MELEE; }
    @Range(min=0,max=28)
    @ConfigItem(keyName="foodWarning", name="Food warning below", description="Your chosen food quantity threshold. Zero disables this warning.", position=11, section=slayerSection)
    default int foodWarning() { return 3; }
    @Range(min=0,max=200)
    @ConfigItem(keyName="doseWarning", name="Potion doses warning below", description="Total carried potion doses; not a required task quantity. Zero disables this warning.", position=12, section=slayerSection)
    default int doseWarning() { return 0; }
    @Range(min=0,max=10000)
    @ConfigItem(keyName="ammoWarning", name="Ammo warning below", description="Carried + equipped visible ammunition quantity; no weapon charge inspection.", position=13, section=slayerSection)
    default int ammoWarning() { return 100; }
    @Range(min=0,max=10000)
    @ConfigItem(keyName="runeWarning", name="Rune warning below", description="Visible inventory rune quantity only; rune pouch contents are not counted.", position=14, section=slayerSection)
    default int runeWarning() { return 100; }
    @ConfigItem(keyName="notifySupplies", name="Desktop supplies alerts", description="Notify once when a chosen threshold is crossed. Local notifications only.", position=15, section=slayerSection)
    default boolean notifySupplies() { return false; }
    @Range(min=0,max=99)
    @ConfigItem(keyName="prayerPointsWarning", name="Prayer points warning below", description="Visual warning below this number of your Prayer points. Zero disables it.", position=16, section=slayerSection)
    default int prayerPointsWarning() { return 15; }
    @Range(min=0,max=112)
    @ConfigItem(keyName="prayerDosesWarning", name="Prayer / restore doses below", description="Combined carried Prayer potion and Super restore doses, including mixes. Zero disables it.", position=17, section=slayerSection)
    default int prayerDosesWarning() { return 2; }
    @ConfigItem(keyName="notifyPrayer", name="Desktop Prayer alerts", description="Optional local alert when your points or carried doses cross their threshold. Rearms after recovery.", position=18, section=slayerSection)
    default boolean notifyPrayer() { return false; }
    @ConfigItem(keyName="superiorSpawnAlert", name="Superior spawn alert", description="Optional local Superior alert. Default when enabled: red two-second flash, sound and desktop notification even while focused. Customize notification settings here. Uses your selected settings independently of the built-in Slayer alert. Disable one of the two alerts to avoid duplicate notifications.", position=8, section=slayerSection)
    default Notification superiorSpawnAlert(){
        return Notification.OFF.withInitialized(true).withOverride(true)
            .withTray(true).withSound(NotificationSound.NATIVE).withRequestFocus(RequestFocusType.TASKBAR)
            .withFlash(FlashNotification.FLASH_TWO_SECONDS).withFlashColor(new Color(255,0,0,90))
            .withSendWhenFocused(true).withVolume(100).withTimeout(5);
    }
    @ConfigItem(keyName="notifyBirdhousesReady", name="Notify birdhouses ready", description="Optional local notification when an observed birdhouse becomes ready.", position=19, section=farmingSection)
    default boolean notifyBirdhousesReady() { return false; }
}
