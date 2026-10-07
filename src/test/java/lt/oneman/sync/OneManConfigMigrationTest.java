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
}
