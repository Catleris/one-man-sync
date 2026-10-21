package lt.oneman.sync;

import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UnlocksHubTest
{
    @Test public void accountChangesAndLogoutClearManualAndItemEvidenceWithoutWritingToOtherAccounts()
    {
        Client client=mock(Client.class);ConfigManager configs=mock(ConfigManager.class);ItemManager items=mock(ItemManager.class);
        AtomicReference<String> profile=new AtomicReference<>("account-a");AtomicInteger tick=new AtomicInteger(100);
        when(configs.getRSProfileKey()).thenAnswer(call->profile.get());
        when(configs.getConfiguration("one-man-sync","account-a","unlocksHubProgress"))
            .thenReturn("{\"manual\":[\"GRACEFUL\"],\"favourites\":[\"FAIRY_RINGS\"],\"observed\":{}}");
        when(client.getTickCount()).thenAnswer(call->tick.getAndIncrement());
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getIntStack()).thenReturn(new int[]{1});
        Player player=mock(Player.class);when(player.getName()).thenReturn("First account");when(client.getLocalPlayer()).thenReturn(player);
        UnlocksHub hub=Guice.createInjector(new AbstractModule() {
            @Override protected void configure() {
                bind(Client.class).toInstance(client);
                bind(ClientThread.class).toInstance(mock(ClientThread.class));
                bind(ConfigManager.class).toInstance(configs);
                bind(ItemManager.class).toInstance(items);
                bind(OneManSyncConfig.class).toInstance(new OneManSyncConfig(){});
                bind(com.google.gson.Gson.class).toInstance(new GsonBuilder().create());
            }
        }).getInstance(UnlocksHub.class);
        try {
            hub.startUp();for(int i=0;i<9;i++)hub.onGameTick();
            assertEquals(UnlockCatalog.State.MANUAL,find(hub,"GRACEFUL").state);
            ItemComposition gloves=mock(ItemComposition.class);
            when(gloves.getName()).thenReturn("Barrows gloves");when(gloves.getNote()).thenReturn(-1);when(gloves.getPlaceholderTemplateId()).thenReturn(-1);
            when(items.getItemComposition(123)).thenReturn(gloves);
            ItemContainer bank=mock(ItemContainer.class);when(bank.getItems()).thenReturn(new Item[]{new Item(123,1)});
            hub.onItemContainerChanged(new ItemContainerChanged(InventoryID.BANK,bank));
            tick.addAndGet(20);hub.onGameTick();
            assertEquals(UnlockCatalog.State.OBSERVED,find(hub,"BARROWS_GLOVES").state);
            verify(configs).setConfiguration(eq("one-man-sync"),eq("account-a"),eq("unlocksHubProgress"),anyString());
            profile.set("account-b");when(player.getName()).thenReturn("Second account");hub.onGameTick();
            assertEquals(UnlockCatalog.State.READY,find(hub,"GRACEFUL").state);
            assertFalse(find(hub,"BARROWS_GLOVES").state==UnlockCatalog.State.OBSERVED);
            verify(configs,never()).setConfiguration(eq("one-man-sync"),eq("account-b"),eq("unlocksHubProgress"),anyString());
            GameStateChanged logout=new GameStateChanged();logout.setGameState(GameState.LOGIN_SCREEN);
            hub.onGameStateChanged(logout);
            assertTrue(hub.results().stream().allMatch(row->row.state==UnlockCatalog.State.UNKNOWN));
        } finally {hub.shutDown();}
    }
    @Test public void missingProfileDoesNotReadOrWriteProgress()
    {
        Client client=mock(Client.class);ConfigManager configs=mock(ConfigManager.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);when(client.getLocalPlayer()).thenReturn(mock(Player.class));
        UnlocksHub hub=Guice.createInjector(new AbstractModule() {
            @Override protected void configure() {
                bind(Client.class).toInstance(client);bind(ClientThread.class).toInstance(mock(ClientThread.class));
                bind(ConfigManager.class).toInstance(configs);bind(ItemManager.class).toInstance(mock(ItemManager.class));
                bind(OneManSyncConfig.class).toInstance(new OneManSyncConfig(){});bind(com.google.gson.Gson.class).toInstance(new GsonBuilder().create());
            }
        }).getInstance(UnlocksHub.class);
        try {hub.startUp();for(int i=0;i<20;i++)hub.onGameTick();assertTrue(hub.results().stream().allMatch(row->row.state==UnlockCatalog.State.UNKNOWN));}
        finally {hub.shutDown();}
        verify(configs,never()).getConfiguration(anyString(),anyString(),anyString());
    }
    private UnlockCatalog.Result find(UnlocksHub hub,String id)
    {
        return hub.results().stream().filter(row->row.entry.id.equals(id)).findFirst().get();
    }
}
