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
    @ConfigSection(name="Collection / Combat alarms", description="Legendary unlock notifications", position=8, closedByDefault=true)
    String alarmSection="alarmSection";
    @ConfigItem(keyName="alarmCollection", name="Collection Log alarm", description="Show a Legendary banner for newly unlocked Collection Log items. Enable collection notifications in OSRS settings.", position=130, section=alarmSection)
    default boolean alarmCollection(){return false;}
    @ConfigItem(keyName="alarmCombat", name="Combat task alarm", description="Show a Legendary banner when the game announces a completed combat task.", position=131, section=alarmSection)
    default boolean alarmCombat(){return false;}
    @ConfigItem(keyName="alarmCombatTiers", name="Combat tier unlock alarm", description="Show a banner when your points reach a new Combat Achievements tier. Existing tiers on login are not replayed.", position=135, section=alarmSection)
    default boolean alarmCombatTiers(){return false;}
    @ConfigItem(keyName="alarmDiaries", name="Achievement Diary alarm", description="Show a banner when a Diary region tier becomes complete. All twelve regions and Easy through Elite are supported.", position=136, section=alarmSection)
    default boolean alarmDiaries(){return false;}
    @ConfigItem(keyName="alarmQuests", name="Quest completion window style", description="Apply a Legendary background and readable text colors to the original OSRS quest-completion window. Preserves real rewards, model, ribbon and controls; no extra quest banner.", position=137, section=alarmSection)
    default boolean alarmQuests(){return false;}
    @ConfigItem(keyName="alarmSound", name="Alarm sound", description="Play a short game UI sound when a banner begins", position=134, section=alarmSection)
    default boolean alarmSound(){return false;}
    @Range(min=3,max=15)
    @ConfigItem(keyName="alarmSeconds", name="Display seconds", description="How long each queued notification is displayed", position=132, section=alarmSection)
    default int alarmSeconds(){return 7;}
    @Range(min=60,max=140)
    @ConfigItem(keyName="alarmScale", name="Banner scale (%)", description="Notification size; automatically fits narrower viewports", position=133, section=alarmSection)
    default int alarmScale(){return 100;}

    @ConfigSection(name="Sync / Account", description="Website connection and upload interval", position=0, closedByDefault=true)
    String syncSection = "syncSection";

    @ConfigSection(name="Roadmap / Screenshots", description="Companion preferences and milestone captures", position=1, closedByDefault=true)
    String roadmapSection = "r…3734 tokens truncated…ult int prayerDosesWarning() { return 2; }
    @ConfigItem(keyName="notifyPrayer", name="Desktop Prayer alerts", description="Optional local alert when your points or carried doses cross their threshold. Rearms after recovery.", position=18, section=slayerSection)
    default boolean notifyPrayer() { return false; }
    @ConfigItem(keyName="superiorSpawnAlert", name="Superior spawn alert", description="Optional local Superior alert. Default when enabled: red two-second flash, sound and desktop notification even while focused. Customize notification settings here. Uses your selected settings independently of the built-in Slayer alert. Disable one of the two alerts to avoid duplicate notifications.", position=8, section=slayerSection)
    default Notification superiorSpawnAlert(){
        return Notification.OFF.withInitialized(true).withOverride(true)
            .withTray(true).withSound(NotificationSound.NATIVE).withRequestFocus(RequestFocusType.TASKBAR)
            .withFlash(FlashNotification.FLASH_TWO_SECONDS).withFlashColor(new Color(255,0,0,90))
            .withSendWhenFocused(true).withVolume(100).withTimeout(5);
    }
    @ConfigItem(keyName="farmingSeedMenu", name="Farming Hub seed menu",
        description="Add Farming Hub next to Examine for recognised seeds in the inventory and bank.",
        position=24, section=farmingSection)
    default boolean farmingSeedMenu() { return false; }

    @ConfigItem(keyName="notifyBirdhousesReady", name="Notify birdhouses ready", description="Show a desktop notification when a tracked birdhouse becomes ready.", position=19, section=farmingSection)
    default boolean notifyBirdhousesReady() { return false; }
    @ConfigItem(keyName="birdhouseReadySound", name="Birdhouse ready sound",
        description="Play the RuneLite notification sound when Notify birdhouses ready is enabled.",
        position=25, section=farmingSection)
    default boolean birdhouseReadySound() { return true; }

    @ConfigItem(keyName="farmingHealthWarnings", name="Farming health & compost hints", description="Show observed disease/death and last reported bottomless bucket amount in Farming Hub.", position=20, section=farmingSection)
    default boolean farmingHealthWarnings() { return true; }
    @ConfigItem(keyName="notifyFarmingDisease", name="Notify diseased/dead patches", description="One local alert per observed unhealthy episode; requires Time Tracking observations.", position=21, section=farmingSection)
    default boolean notifyFarmingDisease() { return false; }
    @Range(min=0,max=10000)
    @ConfigItem(keyName="bottomlessRefillBelow", name="Bottomless refill at/below", description="Warn at or below this last reported use count. Zero warns only when empty.", position=22, section=farmingSection)
    default int bottomlessRefillBelow() { return 50; }
    @ConfigItem(keyName="notifyBottomlessRefill", name="Notify bottomless refill", description="Optional local alert from explicit game bucket messages; no inferred consumption.", position=23, section=farmingSection)
    default boolean notifyBottomlessRefill() { return false; }
}
