package lt.oneman.sync;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.client.config.ConfigManager;

/** Copies old Lab preferences without replacing existing Sync preferences.
 * Account journals and bank snapshots deliberately retain their original profile keys. */
final class OneManConfigMigration
{
    private static final String[] KEYS = {"combatStyle", "foodWarning", "doseWarning", "ammoWarning",
        "runeWarning", "notifySupplies", "prayerPointsWarning", "prayerDosesWarning", "notifyPrayer"};
    private OneManConfigMigration() {}

    static void migrate(ConfigManager manager)
    {
        migrate(key -> manager.getConfiguration(SlayerLabConfig.GROUP, key),
            key -> manager.getConfiguration("one-man-sync", key),
            (key, value) -> manager.setConfiguration("one-man-sync", key, value));
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
