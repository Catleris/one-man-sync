package net.runelite.client.plugins.config;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ChatColorConfig;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import static org.mockito.Mockito.*;

/** Real ConfigPlugin lifecycle/subscriber; only its destination UI and services are doubles. */
public final class NativeConfigTestSupport implements AutoCloseable
{
    private final EventBus bus;
    private final ConfigPlugin plugin = new ConfigPlugin();
    private final ClientToolbar toolbar = mock(ClientToolbar.class);
    private final TopLevelConfigPanel destination = mock(TopLevelConfigPanel.class);

    public NativeConfigTestSupport(EventBus bus) throws Exception
    {
        this.bus = bus;
        Guice.createInjector(new AbstractModule() {
            @Override protected void configure() {
                bind(ClientToolbar.class).toInstance(toolbar);
                bind(PluginListPanel.class).toInstance(mock(PluginListPanel.class));
                bind(TopLevelConfigPanel.class).toInstance(destination);
                bind(ConfigManager.class).toInstance(mock(ConfigManager.class));
                bind(RuneLiteConfig.class).toInstance(new RuneLiteConfig(){});
                bind(ChatColorConfig.class).toInstance(mock(ChatColorConfig.class));
            }
        }).injectMembers(plugin);
        SwingUtilities.invokeAndWait(() -> {
            try { plugin.startUp(); }
            catch(Exception error) { throw new RuntimeException(error); }
        });
        bus.register(plugin);
    }

    public void assertOpened(String name) throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {});
        verify(toolbar).openPanel(isA(NavigationButton.class));
        verify(destination).openConfigurationPanel(name);
    }

    @Override public void close() throws Exception
    {
        bus.unregister(plugin);
        SwingUtilities.invokeAndWait(() -> {
            try { plugin.shutDown(); }
            catch(Exception error) { throw new RuntimeException(error); }
        });
    }
}
