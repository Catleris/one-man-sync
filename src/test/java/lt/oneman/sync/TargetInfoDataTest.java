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
    @Test public void sideDockIsCompactAndNeverOverlapsInventoryOrNativeTabRows(){
        java.awt.Rectangle inv=new java.awt.Rectangle(100,60,190,260);
        java.awt.Rectangle left=TargetInfoOverlay.sideButtonBounds(inv,800,600);
        assertEquals(32,left.width);assertEquals(36,left.height);assertFalse(left.intersects(inv));
        assertTrue(left.x+left.width<inv.x);assertTrue(left.y>inv.y);assertTrue(left.y+left.height<inv.y+inv.height);
        java.awt.Rectangle right=TargetInfoOverlay.sideButtonBounds(new java.awt.Rectangle(20,60,190,260),400,600);
        assertTrue(right.x>210);assertEquals(32,right.width);
        assertTrue(TargetInfoOverlay.sideButtonBounds(new java.awt.Rectangle(0,0,190,260),200,280).isEmpty());
    }
}
