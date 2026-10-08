package lt.oneman.sync;
import java.awt.Color;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class SlayerRequirementOverlayTest {
 @Test public void ownershipDoesNotConfuseCarriedWithEquipped() {
  SlayerLabCatalog.Requirement r=SlayerLabCatalog.find("Basilisks").requirements.get(0);
  Set<String> empty=Collections.emptySet(), shield=Collections.singleton("mirror shield");
  assertEquals("Equipped",SlayerRequirementOverlay.row(r,empty,shield,Collections.emptyMap(),false).status);
  assertEquals("Equip",SlayerRequirementOverlay.row(r,shield,empty,Collections.emptyMap(),false).status);
  assertEquals(Color.GREEN,SlayerRequirementOverlay.row(r,shield,empty,Collections.emptyMap(),false).color);
  assertEquals("In bank",SlayerRequirementOverlay.row(r,empty,empty,Collections.singletonMap("mirror shield",1),true).status);
  assertEquals("Missing",SlayerRequirementOverlay.row(r,empty,empty,Collections.singletonMap("mirror shield",0),true).status);
  assertEquals("Bank unknown",SlayerRequirementOverlay.row(r,empty,empty,Collections.emptyMap(),false).status);
  assertEquals("Missing",SlayerRequirementOverlay.row(r,empty,Collections.singleton("slayer helmet"),Collections.emptyMap(),true).status);
 }
}
