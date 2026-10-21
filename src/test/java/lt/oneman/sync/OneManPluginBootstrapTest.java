package lt.oneman.sync;

import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import net.runelite.api.MenuAction;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.eventbus.Subscribe;
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
        EventBus bus=new EventBus();
        Injector parent=Guice.createInjector(new AbstractModule()
        {
            @Override protected void configure()
            {
                bind(Client.class).toInstance(client);
                bind(EventBus.class).toInstance(bus);
                bind(ConfigManager.class).toInstance(configs);
                bind(ClientThread.class).toInstance(mock(ClientThread.class));
                bind(ItemManager.class).toInstance(mock(ItemManager.class));
                bind(ClientToolbar.class).toInstance(toolbar);
                bind(net.runelite.client.ui.overlay.outline.ModelOutlineRenderer.class).toInstance(mock(net.runelite.client.ui.overlay.outline.ModelOutlineRenderer.class));
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
        SettingsRequests settingsRequests=new SettingsRequests();
        bus.register(settingsRequests);
        try
        {
            plugin.startUp();
            bus.register(plugin);
            SwingUtilities.invokeAndWait(()->{});
            verify(toolbar).addNavigation(navigation.capture());
            SwingUtilities.invokeAndWait(()->{
                Container panel=navigation.getValue().getPanel();
                JComboBox<?> hubs=findHubSelector(panel);
                assertNotNull("The real sidebar must expose all hubs and sync settings",hubs);
                assertEquals("Overview",hubs.getItemAt(0));
                assertEquals("Slayer",hubs.getItemAt(1));
                assertEquals("Farming Hub",hubs.getItemAt(2));
                assertEquals("Clue Scroll Hub",hubs.getItemAt(3));
                assertEquals("Monsters Hub",hubs.getItemAt(4));
                JButton monstersShortcut=findNamed(panel,"monstersHubShortcut",JButton.class);
                assertNotNull("Monsters Hub must also have a permanent direct button",monstersShortcut);
                monstersShortcut.doClick();
                assertEquals("Monsters Hub",hubs.getSelectedItem());
                assertNotNull(findNamed(panel,"monstersProfile",JComboBox.class));
                assertNotNull(findNamed(panel,"monstersShamanOutline",javax.swing.JCheckBox.class));
                assertNotNull(findNamed(panel,"monstersShamanFill",javax.swing.JCheckBox.class));
                assertNotNull(findNamed(panel,"monstersShamanLabel",javax.swing.JCheckBox.class));
                assertNotNull(findNamed(panel,"monstersShamanTileOpacity",javax.swing.JSlider.class));
                assertNotNull(findNamed(panel,"monstersShamanModelOpacity",javax.swing.JSlider.class));
                assertSame("The sidebar must fill its viewport without an outer scrolling wrapper", panel, navigation.getValue().getPanel().getWrappedPanel());
                panel.setSize(242, 700);
                hubs.setSelectedItem("Clue Scroll Hub");
                for(int pass=0;pass<4;pass++) layoutTree(panel);
                assertVisibleChildrenFit(panel);
                for(int section=0;section<hubs.getItemCount();section++) {
                    hubs.setSelectedIndex(section);
                    for(int pass=0;pass<4;pass++) layoutTree(panel);
                    assertVisibleChildrenFit(panel);
                }
                hubs.setSelectedItem("Clue Scroll Hub");
                assertTrue("STASH needs its own browser tab",selectTab(panel,"STASH"));
                JComboBox<?> tier=findNamed(panel,"stashTierFilter",JComboBox.class);
                JComboBox<?> stashList=findNamed(panel,"stashLocation:Beginner",JComboBox.class);
                assertNotNull(tier);tier.setSelectedItem("Beginner");
                stashList=findNamed(panel,"stashLocation:Beginner",JComboBox.class);assertNotNull(stashList);
                assertEquals(3,stashList.getItemCount());
                tier.setSelectedItem("Easy");
                stashList=findNamed(panel,"stashLocation:Easy",JComboBox.class);assertNotNull(stashList);
                for(int row=0;row<stashList.getItemCount();row++) assertEquals("Easy",((OneManMapCatalog.Site)stashList.getItemAt(row)).category);
                for(int pass=0;pass<4;pass++) layoutTree(panel);assertVisibleChildrenFit(panel);
                hubs.setSelectedItem("Farming Hub");
                JComboBox<?> type=findNamed(panel,"farmingTypeFilter",JComboBox.class);
                assertNotNull(type);type.setSelectedItem("Tree");
                JComboBox<?> patchList=findNamed(panel,"farmingLocation:Tree",JComboBox.class);
                assertNotNull(patchList);assertEquals(7,patchList.getItemCount());patchList.setSelectedIndex(0);
                for(int pass=0;pass<4;pass++) layoutTree(panel);assertVisibleChildrenFit(panel);
                assertTrue("Seeds needs its own lookup tab",selectTab(panel,"Seeds"));
                JTextField seedQuery=findNamed(panel,"farmingSeedSearch",JTextField.class);assertNotNull(seedQuery);
                seedQuery.setText("ranarr seed");
                JButton browse=findNamed(panel,"seedBrowse:RANARR",JButton.class);assertNotNull(browse);browse.doClick();
                assertEquals("Herb",type.getSelectedItem());
                JComboBox<?> herbs=findNamed(panel,"farmingLocation:Herb",JComboBox.class);assertNotNull(herbs);assertEquals(10,herbs.getItemCount());
                type.setSelectedItem("All");
                assertTrue(selectTab(panel,"Seeds"));seedQuery.setText("a");
                assertEquals("A long card list must not enlarge the fixed-mode game frame",0,panel.getPreferredSize().height);
                assertEquals(0,panel.getMinimumSize().height);
                panel.setSize(242,503);
                for(int pass=0;pass<6;pass++) layoutTree(panel);assertVisibleChildrenFit(panel);
                assertTrue("The sidebar must identify the loaded build",hasBuildLabel(panel));
                JButton settings=findSettingsButton(panel);
                assertNotNull("Native settings must be reachable from every hub",settings);
                settings.doClick();
            });
            assertNotNull("The actual settings button must dispatch RuneLite's configuration action", settingsRequests.request);
            assertEquals(MenuAction.RUNELITE_OVERLAY_CONFIG, settingsRequests.request.getEntry().getMenuAction());
            assertSame("The native config action must target the loaded plugin", plugin, settingsRequests.request.getOverlay().getPlugin());
            verify(overlays).add(isA(StashMinimapOverlay.class));
            verify(overlays).add(isA(ClueGroundOverlay.class));
            verify(overlays).add(isA(ClueWidgetOverlay.class));
            verify(overlays).add(isA(ShamanMinionOverlay.class));
        }
        finally
        {
            bus.unregister(plugin);
            bus.unregister(settingsRequests);
            plugin.shutDown();
            SwingUtilities.invokeAndWait(()->{});
        }
        verify(toolbar).removeNavigation(same(navigation.getValue()));
        verify(overlays).remove(isA(StashMinimapOverlay.class));
        verify(overlays).remove(isA(ClueGroundOverlay.class));
        verify(overlays).remove(isA(ClueWidgetOverlay.class));
        verify(overlays).remove(isA(ShamanMinionOverlay.class));
    }
    private static final class SettingsRequests
    {
        private OverlayMenuClicked request;
        @Subscribe public void onOverlayMenuClicked(OverlayMenuClicked event) { request=event; }
    }
    private static boolean selectTab(Container parent,String title)
    {
        for(Component child:parent.getComponents()) {
            if(child instanceof JTabbedPane) {
                JTabbedPane tabs=(JTabbedPane)child;
                for(int index=0;index<tabs.getTabCount();index++) if(title.equals(tabs.getTitleAt(index))) {tabs.setSelectedIndex(index);return true;}
            }
            if(child instanceof Container && selectTab((Container)child,title)) return true;
        }
        return false;
    }
    private static <T extends Component> T findNamed(Container parent,String name,Class<T> type)
    {
        for(Component child:parent.getComponents()) {
            if(name.equals(child.getName())&&type.isInstance(child)) return type.cast(child);
            if(child instanceof Container) {T found=findNamed((Container)child,name,type);if(found!=null) return found;}
        }
        return null;
    }
    private static JButton findSettingsButton(Container parent)
    {
        for(Component child:parent.getComponents()) {
            if(child instanceof JButton && "OneMan Sync settings".equals(((JButton)child).getText())) return (JButton)child;
            if(child instanceof Container) { JButton found=findSettingsButton((Container)child); if(found!=null) return found; }
        }
        return null;
    }
    private static void layoutTree(Container parent)
    {
        // Wrapped text changes preferred height after width is known.
        parent.doLayout();
        for(Component child:parent.getComponents()) if(child instanceof Container) layoutTree((Container)child);
    }
    private static void assertVisibleChildrenFit(Container parent)
    {
        for(Component child:parent.getComponents()) {
            if(!child.isVisible()) continue;
            // Native scroll-tab headers intentionally pan horizontally behind their arrow controls.
            // Content viewports still have to fit the sidebar width.
            if(parent instanceof javax.swing.JViewport && parent.getParent() instanceof JTabbedPane) continue;
            assertTrue(child.getClass()+" extends past the sidebar's right edge", child.getX()+child.getWidth()<=parent.getWidth());
            assertTrue(child.getClass()+" extends past the sidebar's left edge",child.getX()>=0);
            if(child instanceof Container) assertVisibleChildrenFit((Container)child);
        }
    }
    private static JComboBox<?> findHubSelector(Container parent)
    {
        for(Component component:parent.getComponents())
        {
            if(component instanceof JComboBox)
            {
                JComboBox<?> box=(JComboBox<?>)component;
                if(box.getItemCount()==5 && "Clue Scroll Hub".equals(box.getItemAt(3))) return box;
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
