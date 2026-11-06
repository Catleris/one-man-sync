package lt.oneman.sync;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Point;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class ElementalWeaknessOverlayTest
{
    @Test public void menuClickMarksExactNpcAndReattachesToItsRespawn()
    {
        Client client = mock(Client.class);
        ItemManager items = mock(ItemManager.class);
        OneManSyncConfig config = mock(OneManSyncConfig.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(config.monstersTargetInfo()).thenReturn(true);
        when(config.monstersWeaknessChat()).thenReturn(true);
        when(config.monstersWeaknessRune()).thenReturn(true);
        ElementalWeaknessOverlay overlay = Guice.createInjector(new AbstractModule()
        {
            @Override protected void configure()
            {
                bind(Client.class).toInstance(client);
                bind(ItemManager.class).toInstance(items);
                bind(OneManSyncConfig.class).toInstance(config);
            }
        }).getInstance(ElementalWeaknessOverlay.class);

        NPC npc = mock(NPC.class);
        NPCComposition composition = mock(NPCComposition.class);
        when(npc.getName()).thenReturn("Gargoyle");
        when(npc.getTransformedComposition()).thenReturn(composition);
        when(composition.getId()).thenReturn(412);
        when(npc.getCanvasTextLocation(any(), eq("40% Earth"), anyInt())).thenReturn(new Point(10, 60));

        Menu menu = mock(Menu.class);
        MenuEntry examine = mock(MenuEntry.class);
        MenuEntry custom = mock(MenuEntry.class, RETURNS_SELF);
        MenuOpened event = mock(MenuOpened.class);
        when(examine.getNpc()).thenReturn(npc);
        when(examine.getType()).thenReturn(MenuAction.EXAMINE_NPC);
        when(examine.getTarget()).thenReturn("Gargoyle");
        when(event.getMenuEntries()).thenReturn(new MenuEntry[]{examine});
        when(client.getMenu()).thenReturn(menu);
        when(menu.createMenuEntry(0)).thenReturn(custom);
        doAnswer(call -> {
            call.<Consumer<MenuEntry>>getArgument(0).accept(custom);
            return custom;
        }).when(custom).onClick(any());

        overlay.onMenuOpened(event);
        verify(custom).setOption("Check weakness");
        verify(client).addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Gargoyle: 40% Earth weakness.", null);

        Graphics2D graphics = new BufferedImage(120, 120, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try
        {
            overlay.render(graphics);
            overlay.despawned(npc);
            overlay.render(graphics);
            verify(items).getImage(ItemID.EARTHRUNE);

            NPC respawn = mock(NPC.class);
            when(respawn.getName()).thenReturn("Gargoyle");
            when(respawn.getTransformedComposition()).thenReturn(composition);
            when(respawn.getCanvasTextLocation(any(), eq("40% Earth"), anyInt())).thenReturn(new Point(10, 60));
            overlay.appeared(respawn);
            overlay.render(graphics);
            verify(items, times(2)).getImage(ItemID.EARTHRUNE);
        }
        finally
        {
            graphics.dispose();
        }
    }
}
