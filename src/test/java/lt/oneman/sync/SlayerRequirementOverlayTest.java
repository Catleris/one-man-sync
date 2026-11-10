package lt.oneman.sync;
import java.awt.Color;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class SlayerRequirementOverlayTest {
 @Test public void idleAndUnrelatedCombatDoNotShowTaskPreparation() {
  net.runelite.api.Client client=org.mockito.Mockito.mock(net.runelite.api.Client.class);
  OneManSyncConfig config=org.mockito.Mockito.mock(OneManSyncConfig.class);
  org.mockito.Mockito.when(config.slayerRequirementsOverlay()).thenReturn(true);
  org.mockito.Mockito.when(client.getGameState()).thenReturn(net.runelite.api.GameState.LOGGED_IN);
  org.mockito.Mockito.when(client.getVarpValue(net.runelite.api.gameval.VarPlayerID.SLAYER_COUNT)).thenReturn(20);
  net.runelite.api.Player player=org.mockito.Mockito.mock(net.runelite.api.Player.class);
  org.mockito.Mockito.when(client.getLocalPlayer()).thenReturn(player);
  SlayerRequirementOverlay overlay=com.google.inject.Guice.createInjector(new com.google.inject.AbstractModule() {
   @Override protected void configure() {
    bind(net.runelite.api.Client.class).toInstance(client);bind(OneManSyncConfig.class).toInstance(config);
   }
  }).getInstance(SlayerRequirementOverlay.class);
  overlay.taskName="Gargoyles";overlay.rows=Collections.singletonList(new SlayerRequirementOverlay.Row("Rock hammer","In bank",Color.YELLOW));
  java.awt.Graphics2D graphics=new java.awt.image.BufferedImage(400,400,2).createGraphics();
  try {
   assertNull(overlay.render(graphics));
   net.runelite.api.NPC npc=org.mockito.Mockito.mock(net.runelite.api.NPC.class);
   org.mockito.Mockito.when(player.getInteracting()).thenReturn(npc);
   org.mockito.Mockito.when(npc.getName()).thenReturn("Cow");assertNull(overlay.render(graphics));
   org.mockito.Mockito.when(npc.getName()).thenReturn("Gargoyle");assertNotNull(overlay.render(graphics));
   org.mockito.Mockito.when(client.getVarpValue(net.runelite.api.gameval.VarPlayerID.SLAYER_COUNT)).thenReturn(0);
   assertNull(overlay.render(graphics));
  } finally {graphics.dispose();}
 }

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
