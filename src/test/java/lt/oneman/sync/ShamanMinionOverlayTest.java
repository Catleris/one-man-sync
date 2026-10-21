package lt.oneman.sync;

import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ShamanMinionOverlayTest
{
    @Test public void rendersModelFillOutlineAndLabelWithoutTilesAndStopsOnDespawn()
    {
        Client client=mock(Client.class);
        WorldView world=mock(WorldView.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getTopLevelWorldView()).thenReturn(world);
        OneManSyncConfig config=new OneManSyncConfig() {
            @Override public MonstersProfile monstersProfile() { return MonstersProfile.LIZARDMAN_SHAMAN; }
            @Override public boolean monstersShamanTiles() { return false; }
        };
        ModelOutlineRenderer outlines=mock(ModelOutlineRenderer.class);
        Injector injector=Guice.createInjector(new AbstractModule() {
            @Override protected void configure() {
                bind(Client.class).toInstance(client);
                bind(OneManSyncConfig.class).toInstance(config);
                bind(ConfigManager.class).toInstance(mock(ConfigManager.class));
                bind(OverlayManager.class).toInstance(mock(OverlayManager.class));
                bind(ModelOutlineRenderer.class).toInstance(outlines);
            }
        });
        MonstersHub hub=injector.getInstance(MonstersHub.class);
        ShamanMinionOverlay overlay=injector.getInstance(ShamanMinionOverlay.class);
        hub.startUp(JsonObject::new); overlay.setHub(hub);
        NPC npc=mock(NPC.class);
        when(npc.getId()).thenReturn(NpcID.ZEAH_LIZARDSHAMAN_SPAWN);
        when(npc.getWorldView()).thenReturn(world);
        when(npc.getWorldLocation()).thenReturn(new WorldPoint(3200,3200,0));
        when(npc.getLocalLocation()).thenReturn(new LocalPoint(128,128));
        when(npc.getConvexHull()).thenReturn(new Polygon(new int[]{5,20,20,5},new int[]{5,5,20,20},4));
        when(npc.getCanvasTextLocation(any(Graphics2D.class),eq("Spawn"),anyInt())).thenReturn(new Point(25,25));
        BufferedImage image=new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=image.createGraphics();
        try {
            hub.onNpcSpawned(new NpcSpawned(npc));
            overlay.render(graphics);
            verify(outlines).drawOutline(npc,2,config.monstersShamanModelColor(),2);
            verify(npc).getCanvasTextLocation(any(Graphics2D.class),eq("Spawn"),anyInt());
            assertTrue("Visible silhouette tint must be drawn",(image.getRGB(10,10)>>>24)>0);
            hub.onNpcDespawned(new NpcDespawned(npc));
            overlay.render(graphics);
            verifyNoMoreInteractions(outlines);
        } finally { graphics.dispose(); hub.shutDown(); }
    }

    @Test public void opacityKeepsSeparateColoursAndRespectsSavedAlpha()
    {
        Color tile=new Color(12,34,56,200), model=new Color(210,120,240);
        assertEquals(new Color(12,34,56,100),ShamanMinionOverlay.opacity(tile,50));
        assertEquals(0,ShamanMinionOverlay.opacity(model,0).getAlpha());
        assertEquals(255,ShamanMinionOverlay.opacity(model,100).getAlpha());
        assertEquals(200,ShamanMinionOverlay.opacity(tile,150).getAlpha());
    }
}
