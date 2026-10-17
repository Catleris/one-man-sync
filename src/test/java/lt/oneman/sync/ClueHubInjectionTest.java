package lt.oneman.sync;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.util.Providers;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Test;
import static org.junit.Assert.*;

/** Validates the actual Guice binding graph without starting or controlling the game client. */
public class ClueHubInjectionTest
{
    private static final class EntryPoint { @javax.inject.Inject ClueHub hub; }

    @Test public void overlaysCannotCreateAnEagerTwoWayInjectionCycle()
    {
        for(Class<?> overlay:new Class<?>[]{ClueGroundOverlay.class,ClueWidgetOverlay.class})
            assertFalse("Eager hub/overlay cycle prevents RuneLite loading OneMan: "+overlay.getSimpleName(),
                eagerlyDependsOn(ClueHub.class,overlay) && eagerlyDependsOn(overlay,ClueHub.class));
    }
    private static boolean eagerlyDependsOn(Class<?> owner,Class<?> dependency)
    {
        for(com.google.inject.spi.InjectionPoint point:com.google.inject.spi.InjectionPoint.forInstanceMethodsAndFields(owner))
            for(com.google.inject.spi.Dependency<?> field:point.getDependencies())
                if(field.getKey().getTypeLiteral().getRawType()==dependency) return true;
        return false;
    }

    @Test public void hubAndBothOverlaysBuildWithoutRecursiveConstructorLoad()
    {
        // Binding validation does not provision the live-client providers.
        Injector injector=Guice.createInjector(Stage.TOOL,new AbstractModule()
        {
            @Override protected void configure()
            {
                bind(Client.class).toProvider(Providers.of(null));
                bind(ClientThread.class).toProvider(Providers.of(null));
                bind(ConfigManager.class).toProvider(Providers.of(null));
                bind(ItemManager.class).toProvider(Providers.of(null));
                bind(OverlayManager.class).toProvider(Providers.of(null));
                bind(OneManSyncConfig.class).toInstance(new OneManSyncConfig(){});
            }
        });
        Injector child=injector.createChildInjector(new AbstractModule()
        {
            @Override protected void configure() { bind(EntryPoint.class).toInstance(new EntryPoint()); }
        });
        assertNotNull(child.getBinding(ClueHub.class));
        assertNotNull(child.getBinding(ClueGroundOverlay.class));
        assertNotNull(child.getBinding(ClueWidgetOverlay.class));
    }
}
