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
    @Test public void reportedCategoriesResolveUsingTheirActualCacheNames(){
        String[] groups={"Sarachnis","Shellbane Gryphon","General","Kurask","Moons of Peril","Others",
            "The Mad Angel","Maggot King","Demonic Gorilla","Doom of Mokhaiotl","Fragment of Seren",
            "Galvek","Glough","Tormented Demon","TzHaar - Ket - Rak's Challenges"};
        for(String group:groups)assertTrue("Missing portrait: "+group,
            CombatAchievementIcons.item(group)>=0||CombatAchievementIcons.sprite(group)>=0);
        assertEquals(SpriteID.HISCORE_SARACHNIS,CombatAchievementIcons.sprite("Sarachnis"));
        assertEquals(SpriteID.HISCORE_LUNAR_CHESTS,CombatAchievementIcons.sprite("Moons of Peril"));
        assertEquals(ItemID.SLAYERGUIDE_KURASK,CombatAchievementIcons.item("Kurask"));
        assertEquals(ItemID.CA_BOOK,CombatAchievementIcons.item("General"));
        assertEquals(ItemID.BOOK_OF_THE_DEAD,CombatAchievementIcons.item("Others"));
        assertEquals(net.runelite.api.gameval.SpriteID.IconBoss25x25.SHELLBANE_GRYPHON,CombatAchievementIcons.sprite("Shellbane Gryphon"));
        assertEquals(net.runelite.api.gameval.SpriteID.IconBoss25x25.MAD_ANGEL,CombatAchievementIcons.sprite("The Mad Angel"));
        assertEquals(net.runelite.api.gameval.SpriteID.IconBoss25x25.MAGGOT_KING,CombatAchievementIcons.sprite("Maggot King"));
        assertEquals(net.runelite.api.gameval.SpriteID.IconBoss25x25.DOOM_OF_MOKHAIOTL,CombatAchievementIcons.sprite("Doom of Mokhaiotl"));
    }
    @Test public void unknownCategoriesRequestAnAchievementBookInsteadOfLeavingThePlaceholder() throws Exception {
        SpriteManager sprites=mock(SpriteManager.class);ItemManager items=mock(ItemManager.class);
        CombatAchievementIcons icons=Guice.createInjector(new AbstractModule(){protected void configure(){
            bind(SpriteManager.class).toInstance(sprites);bind(ItemManager.class).toInstance(items);
        }}).getInstance(CombatAchievementIcons.class);
        icons.startUp();SwingUtilities.invokeAndWait(()->icons.addTo("Future category",new JLabel()));
        verify(items).getImage(ItemID.CA_BOOK);verifyNoInteractions(sprites);
        icons.shutDown();SwingUtilities.invokeAndWait(()->{});
    }
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
        assertEquals(SpriteID.HISCORE_CHAMBERS_OF_XERIC_CHALLENGE_MODE,CombatAchievementIcons.sprite("chambersofxericcm"));
        assertEquals(SpriteID.HISCORE_THE_GAUNTLET,CombatAchievementIcons.sprite("crystallinehunllef"));
        assertEquals(SpriteID.HISCORE_THE_CORRUPTED_GAUNTLET,CombatAchievementIcons.sprite("corruptedhunllef"));
        assertNotEquals(CombatAchievementIcons.sprite("crystallinehunllef"),CombatAchievementIcons.sprite("corruptedhunllef"));
        icons.shutDown();SwingUtilities.invokeAndWait(()->{});clearInvocations(sprites);
        callback.getValue().accept(new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB));SwingUtilities.invokeAndWait(()->icons.addTo("Barrows",c));
        verifyNoInteractions(sprites);assertNotNull(c.getIcon());
    }
}
