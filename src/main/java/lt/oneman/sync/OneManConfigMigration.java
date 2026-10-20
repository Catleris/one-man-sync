package lt.oneman.sync;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Notification;

/** Copies old Lab preferences without replacing existing Sync preferences.
 * Account journals and bank snapshots deliberately retain their original profile keys. */
final class OneManConfigMigration
{
    private static final String[] KEYS = {"combatStyle", "foodWarning", "doseWarning", "ammoWarning",
        "runeWarning", "notifySupplies", "prayerPointsWarning", "prayerDosesWarning", "notifyPrayer"};
    private OneManConfigMigration() {}

    static void migrate(ConfigManager manager)
    {
        // Boolean notification settings predate per-alert defaults. RuneLite otherwise
        // treats them as uninitialized and falls back to global focus/flash settings.
        migrateSuperior(manager.getConfiguration("one-man-sync", "superiorSpawnAlert"),
            alert -> manager.setConfiguration("one-man-sync", "superiorSpawnAlert", alert));
        migrate(key -> manager.getConfiguration(SlayerLabConfig.GROUP, key),
            key -> manager.getConfiguration("one-man-sync", key),
            (key, value) -> manager.setConfiguration("one-man-sync", key, value));
    }

    static void migrateSuperior(String saved, java.util.function.Consumer<Notification> save)
    {
        if ("true".equals(saved) || "false".equals(saved))
        {
            save.accept(new OneManSyncConfig() {}.superiorSpawnAlert()
                .withEnabled(Boolean.parseBoolean(saved)));
        }
    }

    static Notification superiorNotification(Notification saved)
    {
        // Preserve explicitly customized notifications, including global-settings opt-in.
        return saved != null && !saved.isInitialized()
            ? new OneManSyncConfig() {}.superiorSpawnAlert().withEnabled(saved.isEnabled()) : saved;
    }

    static void migrate(Function<String, String> legacy, Function<String, String> current,
        BiConsumer<String, String> save)
    {
        if ("true".equals(current.apply("labPreferencesMigrated"))) return;
        for (String key : KEYS)
        {
            String value = legacy.apply(key);
            if (value != null && current.apply(key) == null) save.accept(key, value);
        }
        save.accept("labPreferencesMigrated", "true");
    }
}
