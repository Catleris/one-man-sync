package lt.oneman.sync;

import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.config.RequestFocusType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MonstersHubTest
{
    private final Client client = mock(Client.class);
    private final WorldView world = mock(WorldView.class);
    private final OneManSyncConfig config = mock(OneManSyncConfig.class);
    private final Notifier notifier = mock(Notifier.class);
    private final AudioPlayer audioPlayer = mock(AudioPlayer.class);
    private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    private MonstersHub hub;

    @Before public void setUp()
    {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getTopLevelWorldView()).thenReturn(world);
        when(config.monstersProfile()).thenReturn(MonstersProfile.LIZARDMAN_SHAMAN);
        when(config.monstersShamanTiles()).thenReturn(true);
        hub = Guice.createInjector(new AbstractModule()
        {
            @Override protected void configure()
            {
                bind(Client.class).toInstance(client);
                bind(net.runelite.client.callback.ClientThread.class).toInstance(mock(net.runelite.client.callback.ClientThread.class));
                bind(OneManSyncConfig.class).toInstance(config);
                bind(ConfigManager.class).toInstance(mock(ConfigManager.class));
                bind(net.runelite.client.ui.overlay.outline.ModelOutlineRenderer.class).toInstance(mock(net.runelite.client.ui.overlay.outline.ModelOutlineRenderer.class));
                bind(OverlayManager.class).toInstance(mock(OverlayManager.class));
                bind(Notifier.class).toInstance(notifier);
                bind(ScheduledExecutorService.class).toInstance(executor);
                bind(AudioPlayer.class).toInstance(audioPlayer);
                bind(net.runelite.client.game.ItemManager.class).toInstance(mock(net.runelite.client.game.ItemManager.class));
                bind(net.runelite.client.input.MouseManager.class).toInstance(mock(net.runelite.client.input.MouseManager.class));
            }
        }).getInstance(MonstersHub.class);
        hub.startUp(JsonObject::new);
    }

    @Test public void alertsOnceWhenEngagedCrabLeavesRemains() throws Exception
    {
        when(config.monstersShamanTiles()).thenReturn(false);
        when(config.monstersGemCrabAlert()).thenReturn(true);
        when(config.monstersGemCrabSound()).thenReturn(GemCrabSound.CRYSTAL_CHIME);
        doAnswer(call -> {
            call.<Runnable>getArgument(0).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        Player player = mock(Player.class);
        NPC crab = mock(NPC.class);
        NPC shell = mock(NPC.class);
        WorldPoint nest = new WorldPoint(1400, 3100, 0);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getWorldLocation()).thenReturn(nest);
        when(crab.getId()).thenReturn(NpcID.GEMSTONE_CRAB);
        when(crab.getWorldLocation()).thenReturn(nest);
        when(shell.getId()).thenReturn(NpcID.GEMSTONE_CRAB_REMAINS);
        when(shell.getWorldLocation()).thenReturn(nest);

        hub.onNpcSpawned(new NpcSpawned(shell));
        verifyNoInteractions(notifier);
        when(player.getInteracting()).thenReturn(crab);
        hub.onGameTick();
        hub.onNpcDespawned(new NpcDespawned(crab));
        verifyNoInteractions(notifier);
        hub.onNpcSpawned(new NpcSpawned(shell));
        hub.onNpcSpawned(new NpcSpawned(shell));
        verify(notifier).notify(argThat((Notification alert) -> alert.isTray()
            && alert.getRequestFocus() == RequestFocusType.TASKBAR
            && alert.getSound() == NotificationSound.OFF), eq("OneMan: Gemstone Crab burrowed!"));
        verify(audioPlayer).play(GemCrabAlert.class, GemCrabSound.CRYSTAL_CHIME.resource, 0f);
    }

    private NPC minion(int tile)
    {
        NPC npc = mock(NPC.class);
        when(npc.getId()).thenReturn(NpcID.ZEAH_LIZARDSHAMAN_SPAWN);
        when(npc.getWorldView()).thenReturn(world);
        when(npc.getLocalLocation()).thenReturn(new LocalPoint(tile * 128, tile * 128));
        when(npc.getWorldLocation()).thenReturn(new WorldPoint(3200 + tile, 3200 + tile, 0));
        return npc;
    }

    @Test public void tracksEachNpcAndRemovesOnlyTheDespawnedInstance()
    {
        NPC first = minion(5), second = minion(6), third = minion(7);
        hub.onNpcSpawned(new NpcSpawned(first));
        hub.onNpcSpawned(new NpcSpawned(second));
        hub.onNpcSpawned(new NpcSpawned(third));
        hub.onNpcSpawned(new NpcSpawned(first));
        assertEquals(3, hub.markers().size());
        hub.onNpcDespawned(new NpcDespawned(second));
        assertEquals(2, hub.markers().size());
        hub.onNpcDespawned(new NpcDespawned(first));
        hub.onNpcDespawned(new NpcDespawned(third));
        assertTrue(hub.markers().isEmpty());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test public void scansOnceOnEnableThenFollowsTheObservedNpcEachTick()
    {
        NPC npc = minion(5);
        IndexedObjectSet npcs = mock(IndexedObjectSet.class);
        doReturn(npcs).when(world).npcs();
        when(npcs.iterator()).thenAnswer(call -> Arrays.asList(npc).iterator());
        hub.onGameTick();
        assertEquals(1, hub.markers().size());
        LocalPoint moved = new LocalPoint(9 * 128, 10 * 128);
        when(npc.getLocalLocation()).thenReturn(moved);
        hub.onGameTick();
        assertSame(moved, hub.markers().get(0).location);
        verify(world, times(1)).npcs();
        when(config.monstersShamanTiles()).thenReturn(false);
        hub.onConfigChanged();
        assertTrue(hub.markers().isEmpty());
        when(config.monstersShamanTiles()).thenReturn(true);
        hub.onConfigChanged();
        hub.onGameTick();
        assertEquals(1, hub.markers().size());
        verify(world, times(2)).npcs();
        hub.shutDown();
        assertFalse(hub.enabled());
        assertTrue(hub.markers().isEmpty());
    }

    @Test public void clearsOnSceneLoadLogoutAndDisableAndIgnoresOtherSpawnTypes()
    {
        NPC npc = minion(5);
        hub.onNpcSpawned(new NpcSpawned(npc));
        GameStateChanged loading = new GameStateChanged();
        loading.setGameState(GameState.LOADING);
        hub.onGameStateChanged(loading);
        assertTrue(hub.markers().isEmpty());
        hub.onNpcSpawned(new NpcSpawned(npc));
        when(config.monstersProfile()).thenReturn(MonstersProfile.NONE);
        hub.onConfigChanged();
        assertTrue(hub.markers().isEmpty());
        hub.onNpcSpawned(new NpcSpawned(npc));
        assertTrue(hub.markers().isEmpty());
        when(config.monstersProfile()).thenReturn(MonstersProfile.LIZARDMAN_SHAMAN);
        when(npc.getId()).thenReturn(NpcID.RAIDS_LIZARDSHAMAN_BLOCKER);
        hub.onNpcSpawned(new NpcSpawned(npc));
        assertTrue(hub.markers().isEmpty());
        when(npc.getId()).thenReturn(NpcID.ZEAH_LIZARDSHAMAN_SPAWN);
        hub.onNpcSpawned(new NpcSpawned(npc));
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        GameStateChanged logout = new GameStateChanged();
        logout.setGameState(GameState.LOGIN_SCREEN);
        hub.onGameStateChanged(logout);
        assertTrue(hub.markers().isEmpty());
    }

    @Test public void excludesChambersAndForeignWorldViews()
    {
        NPC npc = minion(5);
        when(client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON)).thenReturn(1);
        hub.onNpcSpawned(new NpcSpawned(npc));
        assertTrue(hub.markers().isEmpty());
        when(client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON)).thenReturn(0);
        when(npc.getWorldView()).thenReturn(mock(WorldView.class));
        hub.onNpcSpawned(new NpcSpawned(npc));
        assertTrue(hub.markers().isEmpty());
    }

    @Test public void modelHighlightsWorkWithTilesOffAndClearWhenEverythingIsOff()
    {
        when(config.monstersShamanTiles()).thenReturn(false);
        when(config.monstersShamanOutline()).thenReturn(true);
        hub.onConfigChanged();
        hub.onNpcSpawned(new NpcSpawned(minion(5)));
        assertTrue(hub.enabled());
        assertEquals(1, hub.markers().size());
        when(config.monstersShamanOutline()).thenReturn(false);
        when(config.monstersShamanFill()).thenReturn(true);
        assertTrue(hub.enabled());
        when(config.monstersShamanFill()).thenReturn(false);
        when(config.monstersShamanLabel()).thenReturn(true);
        assertTrue(hub.enabled());
        when(config.monstersShamanLabel()).thenReturn(false);
        hub.onConfigChanged();
        assertFalse(hub.enabled());
        assertTrue(hub.markers().isEmpty());
    }

    @Test public void suggestsOnlyMatchingActiveTasksWithoutChangingProfile()
    {
        assertTrue(MonstersHub.suggestsShaman(new OneManSlayerState.LocalTask("Lizardmen", "", 30, 50)));
        assertFalse(MonstersHub.suggestsShaman(new OneManSlayerState.LocalTask("Lizards", "", 30, 50)));
        assertFalse(MonstersHub.suggestsShaman(new OneManSlayerState.LocalTask("Lizardmen", "", 0, 50)));
        assertFalse(MonstersHub.suggestsShaman(new OneManSlayerState.LocalTask("", "", 30, 50)));
        verify(config, never()).monstersProfile();
    }
}
