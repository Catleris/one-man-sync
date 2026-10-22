package lt.oneman.sync;

import com.google.inject.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.*;
import net.runelite.api.SpriteID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CombatAchievementIconsTest
{
    @Test public void gamePortraitsAreCachedAndStaleCallbacksCannotReviveShutdownIcons() throws Exception {
        SpriteManager sprites=mock(SpriteManager.class);ItemManager items=mock(ItemManager.class);
        CombatAchievementIcons icons=Guice.createInjector(new AbstractModule(){protected void configure(){
            bind(SpriteManager.class).toInstance(sprites);bind(ItemManager.class).toInstance(items);
        }}).getInstance(CombatAchievementIcons.class);
        JLabel a=new JLabel(),b=new JLabel(),c=new JLabel();
        icons.startUp();SwingUtilities.invokeAndWait(()->{icons.addTo("Abyssal Sire",a);icons.addTo("Abyssal Sire",b);});
        ArgumentCaptor<Consumer<BufferedImage>> callback=ArgumentCaptor.forClass(Consumer.class);
        verify(sprites,times(1)).getSpriteAsync(eq(SpriteID.HISCORE_ABYSSAL_SIRE),eq(0),callback.capture());
        Icon placeholder=a.getIcon();assertNotNull(placeholder);
        callback.getValue().accept(new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB));SwingUtilities.invokeAndWait(()->{});
        assertNotSame(placeholder,a.getIcon());assertSame(a.getIcon(),b.getIcon());
        SwingUtilities.invokeAndWait(()->icons.addTo("Abyssal Sire",c));assertSame(a.getIcon(),c.getIcon());
        assertEquals(ItemID.SLAYERGUIDE_ABERRANTSPECTER,CombatAchievementIcons.item("aberrantspectre"));
        assertEquals(SpriteID.HISCORE_BARROWS_CHESTS,CombatAchievementIcons.sprite("barrows"));
        icons.shutDown();SwingUtilities.invokeAndWait(()->{});clearInvocations(sprites);
        callback.getValue().accept(new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB));SwingUtilities.invokeAndWait(()->icons.addTo("Barrows",c));
        verifyNoInteractions(sprites);assertNotNull(c.getIcon());
    }
}
