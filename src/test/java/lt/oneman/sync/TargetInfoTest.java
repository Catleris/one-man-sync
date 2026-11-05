package lt.oneman.sync;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import net.runelite.api.*;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TargetInfoTest
{
    private Client client;private OneManSyncConfig config;private Player player;private NPC npc;
    private NPCComposition composition;private TargetInfo info;private TargetInfoOverlay overlay;
    private MouseManager mouse;private EventBus bus;private ClientThread thread;
    private Widget inventory;private BufferedImage image;
    @Before public void setup(){
        client=mock(Client.class);config=mock(OneManSyncConfig.class);player=mock(Player.class);
        npc=mock(NPC.class);composition=mock(NPCComposition.class);mouse=mock(MouseManager.class);bus=new EventBus();
        thread=mock(ClientThread.class);
        doAnswer(call->{((Runnable)call.getArgument(0)).run();return null;}).when(thread).invokeLater(any(Runnable.class));
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);when(client.getCanvasHeight()).thenReturn(600);when(client.getCanvasWidth()).thenReturn(400);
        when(client.getLocalPlayer()).thenReturn(player);when(player.getInteracting()).thenReturn(npc);
        when(config.monstersTargetInfo()).thenReturn(true);
        when(npc.getName()).thenReturn("Gargoyle");when(npc.getTransformedComposition()).thenReturn(composition);
        when(composition.getId()).thenReturn(412);
        Injector injector=Guice.createInjector(new AbstractModule(){protected void configure(){
            bind(Client.class).toInstance(client);bind(OneManSyncConfig.class).toInstance(config);
            bind(ClientThread.class).toInstance(thread);bind(MouseManager.class).toInstance(mouse);bind(EventBus.class).toInstance(bus);
            bind(ItemManager.class).toInstance(mock(ItemManager.class));
            bind(ConfigManager.class).toInstance(mock(ConfigManager.class));
            bind(OverlayManager.class).toInstance(mock(OverlayManager.class));
        }});
        info=injector.getInstance(TargetInfo.class);overlay=injector.getInstance(TargetInfoOverlay.class);info.startUp();
        inventory=mock(Widget.class);when(client.getWidget(InterfaceID.INVENTORY,0)).thenReturn(inventory);
        when(inventory.getBounds()).thenReturn(new Rectangle(20,20,190,260));image=new BufferedImage(400,400,BufferedImage.TYPE_INT_ARGB);
    }
    private void render(){Graphics2D g=image.createGraphics();try{overlay.render(g);}finally{g.dispose();}}
    @Test public void switchesByExactTransformedNpcIdAndDoesNotReusePreviousStats(){
        info.tick(null);assertEquals("Gargoyle",overlay.snapshot.name);assertEquals("Crush",overlay.snapshot.stats.meleeText());
        when(composition.getId()).thenReturn(Integer.MAX_VALUE);when(npc.getName()).thenReturn("Unknown variant");
        info.tick(null);assertEquals("Unknown variant",overlay.snapshot.name);assertNull(overlay.snapshot.stats);
    }
    @Test public void retainsLastNpcAfterCombatDeathAndSceneLoadingButClearsSession(){
        info.tick(null);TargetInfo.Snapshot selected=overlay.snapshot;assertNotNull(selected);
        when(npc.isDead()).thenReturn(true);info.tick(null);assertSame(selected,overlay.snapshot);
        when(player.getInteracting()).thenReturn(null);info.tick(null);assertSame(selected,overlay.snapshot);
        info.gameStateChanged(GameState.LOADING);when(client.getGameState()).thenReturn(GameState.LOADING);info.tick(null);assertSame(selected,overlay.snapshot);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);info.tick(null);assertSame(selected,overlay.snapshot);
        info.gameStateChanged(GameState.HOPPING);assertNull(overlay.snapshot);
        when(npc.isDead()).thenReturn(false);when(player.getInteracting()).thenReturn(npc);info.tick(null);
        info.gameStateChanged(GameState.LOGIN_SCREEN);assertNull(overlay.snapshot);
    }
    @Test public void rejectsPlayersAndUnknownOrUnrelatedSlayerTasks(){
        when(player.getInteracting()).thenReturn(mock(Player.class));info.tick(null);assertNull(overlay.snapshot);
        when(player.getInteracting()).thenReturn(npc);when(config.monstersTargetSlayerOnly()).thenReturn(true);
        info.tick(null);assertNull(overlay.snapshot);
        info.tick(new OneManSlayerState.LocalTask("Bloodvelds","",10,10));assertNull(overlay.snapshot);
        info.tick(new OneManSlayerState.LocalTask("Gargoyles","",10,10));assertNotNull(overlay.snapshot);
        when(player.getInteracting()).thenReturn(null);
        info.tick(new OneManSlayerState.LocalTask("Gargoyles","",0,10));assertNull(overlay.snapshot);
    }
    @Test public void combatUpdatesTheSnapshotWithoutOpeningTheInventoryPage()
    {
        info.tick(null);
        render();
        assertEquals("Gargoyle", overlay.snapshot.name);
        assertFalse(overlay.infoView);
        assertFalse(overlay.blocksItems(30, 100));

        info.mousePressed(event(MouseEvent.MOUSE_PRESSED, 240, 150, MouseEvent.BUTTON1));
        assertTrue(overlay.infoView);

        info.showItems();
        info.tick(null);
        assertFalse(overlay.infoView);
    }
    @Test public void drawsInsideInventoryAndBlocksOnlyItsInfoPage(){
        info.tick(null);info.open();when(config.monstersTargetExpanded()).thenReturn(true);render();
        assertEquals(TargetInfoOverlay.BODY,overlay.hit(30,105));assertTrue(overlay.blocksItems(30,100));
        assertEquals(TargetInfoOverlay.OUTSIDE,overlay.hit(15,100));assertEquals(0,image.getRGB(15,100));assertNotEquals(0,image.getRGB(30,100));
        assertEquals(TargetInfoOverlay.ITEMS,overlay.hit(35,30));
        when(inventory.isHidden()).thenReturn(true);render();assertEquals(TargetInfoOverlay.OUTSIDE,overlay.hit(30,100));
        when(inventory.isHidden()).thenReturn(false);when(client.getDraggedWidget()).thenReturn(mock(Widget.class));render();assertEquals(TargetInfoOverlay.OUTSIDE,overlay.hit(30,100));
    }
    @Test public void switchesToItemsAndBackWithoutPassingClicksToHiddenItems(){
        info.tick(null);info.open();render();
        MouseEvent press=event(MouseEvent.MOUSE_PRESSED,35,30,MouseEvent.BUTTON1);info.mousePressed(press);assertTrue(press.isConsumed());assertFalse(overlay.infoView);
        MouseEvent release=event(MouseEvent.MOUSE_RELEASED,35,30,MouseEvent.BUTTON1);info.mouseReleased(release);assertTrue(release.isConsumed());
        render();assertFalse(overlay.blocksItems(30,100));assertEquals(TargetInfoOverlay.OUTSIDE,overlay.hit(30,100));
        MouseEvent ordinary=event(MouseEvent.MOUSE_PRESSED,30,100,MouseEvent.BUTTON1);info.mousePressed(ordinary);assertFalse(ordinary.isConsumed());
        assertEquals(TargetInfoOverlay.INFO,overlay.hit(240,150));
        MouseEvent open=event(MouseEvent.MOUSE_PRESSED,240,150,MouseEvent.BUTTON1);info.mousePressed(open);assertTrue(open.isConsumed());assertTrue(overlay.infoView);
        render();MouseEvent hiddenItem=event(MouseEvent.MOUSE_PRESSED,30,100,MouseEvent.BUTTON3);info.mousePressed(hiddenItem);assertTrue(hiddenItem.isConsumed());
        when(client.getMouseCanvasPosition()).thenReturn(new net.runelite.api.Point(30,100));bus.post(new PostMenuSort());verify(client).setMenuEntries(any(MenuEntry[].class));
    }
    @Test public void detailTogglePreservesSelectionAndClearWaitsForAnotherTarget(){
        info.tick(null);TargetInfo.Snapshot selected=overlay.snapshot;info.configChanged();assertSame(selected,overlay.snapshot);
        info.open();render();info.mousePressed(event(MouseEvent.MOUSE_PRESSED,190,263,MouseEvent.BUTTON1));assertNull(overlay.snapshot);
        info.tick(null);assertNull(overlay.snapshot);when(player.getInteracting()).thenReturn(null);info.tick(null);
        when(player.getInteracting()).thenReturn(npc);info.tick(null);assertNotNull(overlay.snapshot);
        when(config.monstersTargetInfo()).thenReturn(false);info.configChanged();assertNull(overlay.snapshot);
        info.shutDown();verify(mouse).unregisterMouseListener(info);
    }
    private static MouseEvent event(int type,int x,int y,int button){return new MouseEvent(new Canvas(),type,0,0,x,y,1,false,button);}
}
