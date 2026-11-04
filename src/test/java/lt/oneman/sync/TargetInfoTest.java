package lt.oneman.sync;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TargetInfoTest
{
    private Client client;private OneManSyncConfig config;private Player player;private NPC npc;
    private NPCComposition composition;private TargetInfo info;private TargetInfoOverlay overlay;
    @Before public void setup(){
        client=mock(Client.class);config=mock(OneManSyncConfig.class);player=mock(Player.class);
        npc=mock(NPC.class);composition=mock(NPCComposition.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getLocalPlayer()).thenReturn(player);when(player.getInteracting()).thenReturn(npc);
        when(config.monstersTargetInfo()).thenReturn(true);
        when(npc.getName()).thenReturn("Gargoyle");when(npc.getTransformedComposition()).thenReturn(composition);
        when(composition.getId()).thenReturn(412);
        Injector injector=Guice.createInjector(new AbstractModule(){protected void configure(){
            bind(Client.class).toInstance(client);bind(OneManSyncConfig.class).toInstance(config);
            bind(ConfigManager.class).toInstance(mock(ConfigManager.class));
            bind(OverlayManager.class).toInstance(mock(OverlayManager.class));
            // Overlay is shared between the controller and this test, as RuneLite's overlay manager expects.
            bind(TargetInfoOverlay.class).asEagerSingleton();
        }});
        info=injector.getInstance(TargetInfo.class);overlay=injector.getInstance(TargetInfoOverlay.class);info.startUp();
    }
    @Test public void switchesByExactTransformedNpcIdAndDoesNotReusePreviousStats(){
        info.tick(null);assertEquals("Gargoyle",overlay.snapshot.name);assertEquals("Crush",overlay.snapshot.stats.meleeText());
        when(composition.getId()).thenReturn(Integer.MAX_VALUE);when(npc.getName()).thenReturn("Unknown variant");
        info.tick(null);assertEquals("Unknown variant",overlay.snapshot.name);assertNull(overlay.snapshot.stats);
    }
    @Test public void clearsOnDeathInteractionEndDisableLoadingAndShutdown(){
        info.tick(null);assertNotNull(overlay.snapshot);
        when(npc.isDead()).thenReturn(true);info.tick(null);assertNull(overlay.snapshot);
        when(npc.isDead()).thenReturn(false);when(player.getInteracting()).thenReturn(null);info.tick(null);assertNull(overlay.snapshot);
        when(player.getInteracting()).thenReturn(npc);when(config.monstersTargetInfo()).thenReturn(false);info.tick(null);assertNull(overlay.snapshot);
        when(config.monstersTargetInfo()).thenReturn(true);when(client.getGameState()).thenReturn(GameState.LOADING);info.tick(null);assertNull(overlay.snapshot);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);info.tick(null);assertNotNull(overlay.snapshot);info.shutDown();assertNull(overlay.snapshot);
        info.tick(null);assertNull(overlay.snapshot);
    }
    @Test public void rejectsPlayersAndUnknownOrUnrelatedSlayerTasks(){
        when(player.getInteracting()).thenReturn(mock(Player.class));info.tick(null);assertNull(overlay.snapshot);
        when(player.getInteracting()).thenReturn(npc);when(config.monstersTargetSlayerOnly()).thenReturn(true);
        info.tick(null);assertNull(overlay.snapshot);
        info.tick(new OneManSlayerState.LocalTask("Bloodvelds","",10,10));assertNull(overlay.snapshot);
        info.tick(new OneManSlayerState.LocalTask("Gargoyles","",10,10));assertNotNull(overlay.snapshot);
        info.tick(new OneManSlayerState.LocalTask("Gargoyles","",0,10));assertNull(overlay.snapshot);
    }
    @Test public void inventoryVisibilityAndExpandedRendering(){
        info.tick(null);Graphics2D g=new BufferedImage(400,400,BufferedImage.TYPE_INT_ARGB).createGraphics();
        try{
            assertNull(overlay.render(g));
            Widget inventory=mock(Widget.class);when(client.getWidget(InterfaceID.INVENTORY,0)).thenReturn(inventory);
            java.awt.Dimension compact=overlay.render(g);assertNotNull(compact);
            when(config.monstersTargetExpanded()).thenReturn(true);
            java.awt.Dimension expanded=overlay.render(g);assertNotNull(expanded);assertTrue(expanded.height>compact.height);
            when(inventory.isHidden()).thenReturn(true);assertNull(overlay.render(g));
        }finally{g.dispose();}
    }
}
