package lt.oneman.sync;

import org.junit.Test;
import static org.junit.Assert.*;

public class TargetInfoDataTest
{
    @Test public void bundledCatalogKeepsExactVariantsAndUnknownIdsUnknown()
    {
        TargetInfoData data = new TargetInfoData();
        assertTrue(data.size() > 3000);
        TargetInfoData.Stats gargoyle = data.find(412);
        assertNotNull(gargoyle);
        assertEquals("Earth", gargoyle.element);
        assertEquals("Crush -20", gargoyle.melee());
        assertEquals(Integer.valueOf(40), gargoyle.percent);
        assertNull(data.find(Integer.MAX_VALUE));
        assertNull(TargetInfoData.number("4, 6"));
    }
}
