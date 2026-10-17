package lt.oneman.sync;

import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import net.runelite.client.ui.NavigationButton;
import org.mockito.ArgumentCaptor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.Notifier;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import okhttp3.OkHttpClient;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Real OneMan injection/module and lifecycle, with only RuneLite services replaced by test doubles. */
public class OneManPluginBootstrapTest
{
    @Test public void subscribersMeetRuneLiteEventBusContract()
    {
        EventBus bus=new EventBus();
        OneManSyncPlugin plugin=new OneManSyncPlugin();
        try { bus.register(plugin); }
        finally { bus.unregister(plugin); }
    }

    @Test public void pluginLoadsAndCreatesSidebarWithoutLoggingIntoGame() throws Exception
    {
        Client client=mock(Client.class);
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        ConfigManager configs=mock(ConfigManager.class);
        when(configs.getConfig(OneManSyncConfig.class)).thenReturn(new OneManSyncConfig(){});
        ClientToolbar toolbar=mock(ClientToolbar.class);
        OverlayManager overlays=mock(OverlayManager.class);
        Injector parent=Guice.createInjector(new AbstractModule()
        {
            @Override protected void configure()
            {
                bind(Client.class).toInstance(client);
                bind(ConfigManager.class).toInstance(configs);
                bind(ClientThread.class).toInstance(mock(ClientThread.class));
                bind(ItemManager.class).toInstance(mock(ItemManager.class));
                bind(ClientToolbar.class).toInstance(toolbar);
                bind(OverlayManager.class).toInstance(overlays);
                bind(WorldMapPointManager.class).toInstance(mock(WorldMapPointManager.class));
                bind(Notifier.class).toInstance(mock(Notifier.class));
                bind(DrawManager.class).toInstance(mock(DrawManager.class));
                bind(com.google.gson.Gson.class).toInstance(new GsonBuilder().create());
                bind(OkHttpClient.class).toInstance(new OkHttpClient.Builder().build());
            }
        });
        OneManSyncPlugin plugin=new OneManSyncPlugin();
        // Matches RuneLite PluginModuleFactory: bind the existing plugin instance and install its @Provides module.
        Injector child=parent.createChildInjector(new AbstractModule()
        {
            @Override protected void configure()
            {
                bind(OneManSyncPlugin.class).toInstance(plugin);
                install(plugin);
            }
        });
        assertSame(plugin,child.getInstance(OneManSyncPlugin.class));
        assertNotNull(child.getInstance(OneManSyncConfig.class));
        ArgumentCaptor<NavigationButton> navigation=ArgumentCaptor.forClass(NavigationButton.class);
        EventBus bus=new EventBus();
        try
        {
            plugin.startUp();
            bus.register(plugin);
            SwingUtilities.invokeAndWait(()->{});
            verify(toolbar).addNavigation(navigation.capture());
            SwingUtilities.invokeAndWait(()->{
                Container panel=navigation.getValue().getPanel();
                JComboBox<?> hubs=findHubSelector(panel);
                assertNotNull("The real sidebar must expose all four hubs",hubs);
                assertEquals("Overview",hubs.getItemAt(0));
                assertEquals("Slayer",hubs.getItemAt(1));
                assertEquals("Farming Hub",hubs.getItemAt(2));
                assertEquals("Clue Scroll Hub",hubs.getItemAt(3));
                assertTrue("The sidebar must identify the loaded build",hasBuildLabel(panel));
            });
            verify(overlays).add(isA(ClueGroundOverlay.class));
            verify(overlays).add(isA(ClueWidgetOverlay.class));
        }
        finally
        {
            bus.unregister(plugin);
            plugin.shutDown();
            SwingUtilities.invokeAndWait(()->{});
        }
        verify(toolbar).removeNavigation(same(navigation.getValue()));
        verify(overlays).remove(isA(ClueGroundOverlay.class));
        verify(overlays).remove(isA(ClueWidgetOverlay.class));
    }
    private static JComboBox<?> findHubSelector(Container parent)
    {
        for(Component component:parent.getComponents())
        {
            if(component instanceof JComboBox)
            {
                JComboBox<?> box=(JComboBox<?>)component;
                if(box.getItemCount()==4 && "Clue Scroll Hub".equals(box.getItemAt(3))) return box;
            }
            if(component instanceof Container)
            {
                JComboBox<?> found=findHubSelector((Container)component);
                if(found!=null) return found;
            }
        }
        return null;
    }
    private static boolean hasBuildLabel(Container parent)
    {
        for(Component component:parent.getComponents())
        {
            if(component instanceof JLabel && ((JLabel)component).getText()!=null
                && ((JLabel)component).getText().contains(OneManCompanion.VERSION)) return true;
            if(component instanceof Container && hasBuildLabel((Container)component)) return true;
        }
        return false;
    }
}
