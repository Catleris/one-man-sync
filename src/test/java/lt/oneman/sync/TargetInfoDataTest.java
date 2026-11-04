package lt.oneman.sync;

import org.junit.Test;
import static org.junit.Assert.*;

public class TargetInfoDataTest {
    @Test public void bundledCatalogKeepsExactVariantsAndUnknownIdsUnknown(){
        TargetInfoData data=new TargetInfoData();assertTrue(data.size()>3000);
        TargetInfoData.Stats gargoyle=data.find(412);assertNotNull(gargoyle);
        assertEquals("Crush",gargoyle.meleeText());assertEquals("Earth +40%",gargoyle.elementText());
        assertNull(data.find(Integer.MAX_VALUE));
    }
    @Test public void tiedMeleeBonusesAreNotPresentedAsAUniqueWeakness(){
        String[] cells={"1","Test","?","?","0","0","0","?","?","?","?","?","?","?","?"};
        TargetInfoData.Stats s=new TargetInfoData.Stats(cells);assertEquals("Equal bonuses",s.meleeText());assertEquals("Not listed",s.elementText());
        cells[6]="20";s=new TargetInfoData.Stats(cells);assertEquals("Stab / Slash",s.meleeText());
        cells[4]="?";s=new TargetInfoData.Stats(cells);assertEquals("Unknown",s.meleeText());
        assertNull(TargetInfoData.number("4, 6"));assertEquals("?",TargetInfoData.value(null));
    }
}
