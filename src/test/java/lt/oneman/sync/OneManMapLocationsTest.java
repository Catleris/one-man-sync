package lt.oneman.sync;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.util.*;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.cluescrolls.clues.emote.STASHUnit;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OneManMapLocationsTest
{
    @Test public void catalogCoversInstalledStashesAndDistinctFarmingTypes()
    {
        List<OneManMapCatalog.Site> stashes=OneManMapCatalog.stashes();
        assertEquals("A new RuneLite STASH needs a tier mapping",STASHUnit.values().length,stashes.size());
        assertEquals(3,OneManMapCatalog.filter(stashes,"Beginner").size());
        Set<String> ids=new HashSet<>();
        for(OneManMapCatalog.Site site:stashes) {
            assertTrue(ids.add(site.id));assertFalse(site.points.isEmpty());
            assertTrue(Arrays.stream(ClueTier.values()).anyMatch(t->t.label.equals(site.category)));
        }
        List<OneManMapCatalog.Site> farming=OneManMapCatalog.farming();
        assertEquals(7,OneManMapCatalog.filter(farming,"Tree").size());
        assertEquals(7,OneManMapCatalog.filter(farming,"Fruit Tree").size());
        assertEquals(10,OneManMapCatalog.filter(farming,"Herb").size());
        OneManMapCatalog.Site varrock=OneManMapCatalog.filter(farming,"Tree").stream().filter(s->s.name.equals("Varrock")).findFirst().get();
        assertEquals(new WorldPoint(3226,3457,0),varrock.points.get(0));
        assertTrue(OneManMapCatalog.filter(farming,"Tree").stream().anyMatch(s->s.name.equals("Auburnvale")));
        ids.clear();for(OneManMapCatalog.Site site:farming) assertTrue(ids.add(site.id));
    }

    @Test public void filtersReplacePinsPersistAndNeverRemoveForeignMarkers()
    {
        Fixture f=new Fixture();f.maps.startUp();
        f.maps.configure(OneManMapCatalog.Group.STASH,"Beginner",true);
        assertEquals(3,f.maps.minimapStashes().size());
        assertEquals(3,f.points.stream().filter(p->p.getName()!=null&&p.getName().startsWith("OneMan Beginner:")).count());
        assertEquals(7,f.points.stream().filter(p->p.getName()!=null&&p.getName().startsWith("OneMan Tree:")).count());
        f.maps.configure(OneManMapCatalog.Group.STASH,"Easy",true);
        assertTrue(f.maps.minimapStashes().stream().allMatch(s->s.category.equals("Easy")));
        assertFalse(f.points.stream().anyMatch(p->p.getName()!=null&&p.getName().startsWith("OneMan Beginner:")));
        f.maps.configure(OneManMapCatalog.Group.STASH,"Medium",false);
        assertTrue(f.maps.minimapStashes().isEmpty());
        f.maps.shutDown();assertEquals(Collections.singleton(f.foreign),f.points);
        f.maps.startUp();assertEquals("Medium",f.maps.category(OneManMapCatalog.Group.STASH));
        assertFalse(f.maps.visible(OneManMapCatalog.Group.STASH));
        f.maps.shutDown();assertEquals(Collections.singleton(f.foreign),f.points);
    }

    @Test public void delayedClientCallbacksCannotRestorePinsAfterShutdown()
    {
        Fixture f=new Fixture(true);f.maps.startUp();
        f.maps.configure(OneManMapCatalog.Group.STASH,"Easy",true);
        f.maps.shutDown();
        while(!f.callbacks.isEmpty()) f.callbacks.remove().run();
        assertEquals(Collections.singleton(f.foreign),f.points);
        assertTrue(f.maps.minimapStashes().isEmpty());
    }

    @Test public void selectedPatchWaitsForAnOpenMapAndCentresOnlyOnce()
    {
        Fixture f=new Fixture();f.maps.startUp();
        OneManMapCatalog.Site site=f.maps.sites(OneManMapCatalog.Group.FARMING,"Tree").stream().filter(s->s.name.equals("Varrock")).findFirst().get();
        WorldMap map=mock(WorldMap.class);when(f.client.getWorldMap()).thenReturn(map);
        when(f.client.getGameState()).thenReturn(GameState.LOGGED_IN);
        f.maps.focus(OneManMapCatalog.Group.FARMING,site);
        verifyNoInteractions(map);
        when(f.client.getWidget(InterfaceID.Worldmap.MAP_DISPLAY)).thenReturn(mock(Widget.class));
        f.maps.tick();f.maps.tick();
        verify(map,times(1)).setWorldMapPositionTarget(new WorldPoint(3226,3457,0));
        f.maps.focus(OneManMapCatalog.Group.FARMING,site);
        when(f.client.getWidget(InterfaceID.Worldmap.MAP_DISPLAY)).thenReturn(null);
        f.maps.focus(OneManMapCatalog.Group.FARMING,site);
        f.maps.configure(OneManMapCatalog.Group.FARMING,"Herb",true);
        when(f.client.getWidget(InterfaceID.Worldmap.MAP_DISPLAY)).thenReturn(mock(Widget.class));
        f.maps.tick();verify(map,times(2)).setWorldMapPositionTarget(new WorldPoint(3226,3457,0));
        f.maps.shutDown();
    }

    private static final class Fixture
    {
        final Client client=mock(Client.class);
        final WorldMapPoint foreign=new WorldMapPoint(new WorldPoint(1,1,0),null);
        final Set<WorldMapPoint> points=new HashSet<>(Collections.singleton(foreign));
        final Map<String,String> saved=new HashMap<>();
        final OneManMapLocations maps;
        final Deque<Runnable> callbacks=new ArrayDeque<>();
        Fixture() { this(false); }
        Fixture(boolean delay)
        {
            ClientThread thread=mock(ClientThread.class);
            doAnswer(call->{Runnable action=call.getArgument(0,Runnable.class);if(delay) callbacks.add(action);else action.run();return null;}).when(thread).invokeLater(any(Runnable.class));
            WorldMapPointManager manager=mock(WorldMapPointManager.class);
            doAnswer(call->{points.add(call.getArgument(0));return null;}).when(manager).add(any(WorldMapPoint.class));
            doAnswer(call->{points.remove(call.getArgument(0));return null;}).when(manager).remove(any(WorldMapPoint.class));
            ConfigManager configs=mock(ConfigManager.class);
            when(configs.getConfiguration(anyString(),anyString())).thenAnswer(call->saved.get(call.getArgument(1)));
            doAnswer(call->{saved.put(call.getArgument(1),String.valueOf((Object)call.getArgument(2)));return null;})
                .when(configs).setConfiguration(anyString(),anyString(),any(Object.class));
            doAnswer(call->{saved.put(call.getArgument(1),call.getArgument(2));return null;})
                .when(configs).setConfiguration(anyString(),anyString(),anyString());
            maps=Guice.createInjector(new AbstractModule() {
                @Override protected void configure() {
                    bind(Client.class).toInstance(client);bind(ClientThread.class).toInstance(thread);
                    bind(ConfigManager.class).toInstance(configs);bind(WorldMapPointManager.class).toInstance(manager);
                }
            }).getInstance(OneManMapLocations.class);
        }
    }
}
