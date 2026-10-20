package lt.oneman.sync;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class OneManConfigMigrationTest
{
    @Test public void preservesPreferencesWithoutEnablingNetworkOrOverwritingSync()
    {
        Map<String,String> old = new HashMap<>(), current = new HashMap<>();
        old.put("foodWarning", "8"); old.put("prayerPointsWarning", "25");
        old.put("enabled", "true"); old.put("syncKey", "legacy");
        current.put("foodWarning", "4"); current.put("syncKey", "existing");
        OneManConfigMigration.migrate(old::get, current::get, current::put);
        assertEquals("4", current.get("foodWarning"));
        assertEquals("25", current.get("prayerPointsWarning"));
        assertEquals("existing", current.get("syncKey"));
        assertNull(current.get("enabled"));
        current.remove("prayerPointsWarning");
        OneManConfigMigration.migrate(old::get, current::get, current::put);
        assertNull(current.get("prayerPointsWarning"));
    }
    @Test public void upgradesLegacySuperiorWithoutChangingOptIn()
    {
        for (String value : new String[]{"true", "false"})
        {
            java.util.List<net.runelite.client.config.Notification> saved = new java.util.ArrayList<>();
            OneManConfigMigration.migrateSuperior(value, saved::add);
            assertEquals(1, saved.size());
            net.runelite.client.config.Notification alert = saved.get(0);
            assertEquals(Boolean.parseBoolean(value), alert.isEnabled());
            assertTrue(alert.isInitialized());
            assertTrue(alert.isOverride());
            assertTrue(alert.isSendWhenFocused());
            assertEquals(net.runelite.client.config.FlashNotification.FLASH_TWO_SECONDS, alert.getFlash());
        }
    }

    @Test public void preservesMissingAndCustomizedSuperiorSettings()
    {
        OneManConfigMigration.migrateSuperior(null, value -> fail("Must remain opt-in"));
        OneManConfigMigration.migrateSuperior("{\"enabled\":true,\"initialized\":true}",
            value -> fail("Must preserve custom settings"));
        net.runelite.client.config.Notification custom = new OneManSyncConfig() {}.superiorSpawnAlert()
            .withEnabled(true).withOverride(false).withSendWhenFocused(false);
        assertSame(custom, OneManConfigMigration.superiorNotification(custom));
        assertNull(OneManConfigMigration.superiorNotification(null));
        assertTrue(OneManConfigMigration.superiorNotification(
            net.runelite.client.config.Notification.ON).isSendWhenFocused());
        assertFalse(OneManConfigMigration.superiorNotification(
            net.runelite.client.config.Notification.OFF).isEnabled());
    }
}
