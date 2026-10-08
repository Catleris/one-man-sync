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
}
