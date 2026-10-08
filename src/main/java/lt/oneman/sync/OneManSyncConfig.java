package lt.oneman.sync;

import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("one-man-sync")
public interface OneManSyncConfig extends SlayerLabConfig
{
    @ConfigItem(
        keyName = "enabled",
        name = "Enable OneMan Sync",
        description = "Sends your OSRS display name, skill levels/XP, quest states, Achievement Diaries, Combat Achievements, boss kill counts, Collection Log events, pet/personal-best/loot events, bank item IDs/names/quantities when you open your bank, Clue Scroll STASH built/filled states, current inventory/equipment and selected storage snapshots, Slayer state, POH feature observations, currencies and daily-account timers to oneman.lt.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 0
    )
    default boolean enabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "syncKey",
        name = "Sync Key",
        description = "Generate this in oneman.lt -> Account -> RuneLite Sync. This is a OneMan API token, not a Jagex credential.",
        secret = true,
        position = 1
    )
    default String syncKey()
    {
        return "";
    }

    @Range(min = 2, max = 30)
    @ConfigItem(
        keyName = "intervalMinutes",
        name = "Full sync every",
        description = "Periodic full sync interval in minutes.",
        position = 2
    )
    default int intervalMinutes()
    {
        return 5;
    }
    @ConfigItem(keyName="highlightGoalItems", name="Highlight goal items", description="Outline curated preparation items in your bank.", position=3)
    default boolean highlightGoalItems(){return true;}

    @ConfigItem(keyName="companionLanguage", name="Companion language", description="Language for roadmap reasons: LT or EN.", position=4)
    default String companionLanguage(){return "EN";}

    @ConfigItem(keyName="milestoneScreenshots", name="Save milestone screenshots", description="Save local 99-level and quest-completion screenshots. Images include the visible game window.", position=5)
    default boolean milestoneScreenshots(){return false;}

    @ConfigItem(keyName="uploadMilestoneScreenshots", name="Upload milestone screenshots", description="Send captured screenshots to your OneMan account. Requires local capture and OneMan Sync enabled.", warning="This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers", position=6)
    default boolean uploadMilestoneScreenshots(){return false;}
    @ConfigItem(keyName="slayerRequirementsOverlay", name="Slayer item hints", description="Show task protection and finishing items on the left: red missing, purple in last observed bank, green carried/equipped, grey bank unknown.", position=7)
    default boolean slayerRequirementsOverlay(){return true;}

    // RuneLite initializes defaults only for methods declared on this config.
    @ConfigItem(keyName="combatStyle", name="Supplies combat style", description="Controls ammunition or rune quantity warnings; does not select combat actions.", position=10)
    default CombatStyle combatStyle() { return CombatStyle.MELEE; }
    @Range(min=0,max=28)
    @ConfigItem(keyName="foodWarning", name="Food warning below", description="Your chosen food quantity threshold. Zero disables this warning.", position=11)
    default int foodWarning() { return 3; }
    @Range(min=0,max=200)
    @ConfigItem(keyName="doseWarning", name="Potion doses warning below", description="Total carried potion doses; not a required task quantity. Zero disables this warning.", position=12)
    default int doseWarning() { return 0; }
    @Range(min=0,max=10000)
    @ConfigItem(keyName="ammoWarning", name="Ammo warning below", description="Carried + equipped visible ammunition quantity; no weapon charge inspection.", position=13)
    default int ammoWarning() { return 100; }
    @Range(min=0,max=10000)
    @ConfigItem(keyName="runeWarning", name="Rune warning below", description="Visible inventory rune quantity only; rune pouch contents are not counted.", position=14)
    default int runeWarning() { return 100; }
    @ConfigItem(keyName="notifySupplies", name="Desktop supplies alerts", description="Notify once when a chosen threshold is crossed. Local notifications only.", position=15)
    default boolean notifySupplies() { return false; }
    @Range(min=0,max=99)
    @ConfigItem(keyName="prayerPointsWarning", name="Prayer points warning below", description="Visual warning below this number of your Prayer points. Zero disables it.", position=16)
    default int prayerPointsWarning() { return 15; }
    @Range(min=0,max=112)
    @ConfigItem(keyName="prayerDosesWarning", name="Prayer / restore doses below", description="Combined carried Prayer potion and Super restore doses, including mixes. Zero disables it.", position=17)
    default int prayerDosesWarning() { return 2; }
    @ConfigItem(keyName="notifyPrayer", name="Desktop Prayer alerts", description="Optional local alert when your points or carried doses cross their threshold. Rearms after recovery.", position=18)
    default boolean notifyPrayer() { return false; }
}
