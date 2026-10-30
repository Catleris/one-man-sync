package lt.oneman.sync;

import com.google.inject.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class QuestWindowThemeTest {
    private final Client client=mock(Client.class);
    private final OneManSyncConfig config=mock(OneManSyncConfig.class);
    private final OverlayManager overlays=mock(OverlayManager.class);
    private final Widget scroll=mock(Widget.class),reward=mock(Widget.class),model=mock(Widget.class),ribbon=mock(Widget.class),close=mock(Widget.class);
    private final Widget content=mock(Widget.class);
    private final AtomicBoolean hidden=new AtomicBoolean(false);
    private final AtomicInteger type=new AtomicInteger(net.runelite.api.widgets.WidgetType.MODEL);
    private final SpritePixels skin=mock(SpritePixels.class);
    private final Map<Integer,SpritePixels> sprites=new HashMap<>();
    private final AtomicInteger color=new AtomicInteger(0x123456);
    private final AtomicBoolean shadow=new AtomicBoolean(false);
    private QuestWindowTheme theme;
    @Before public void setup(){
        when(config.alarmQuests()).thenReturn(true);when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getWidget(InterfaceID.Questscroll.SCROLL)).thenReturn(scroll);
        when(scroll.getWidth()).thenReturn(488);when(scroll.getHeight()).thenReturn(334);
        when(client.getWidgetSpriteOverrides()).thenReturn(sprites);
        when(client.getWidget(InterfaceID.Questscroll.CONTENT)).thenReturn(content);
        when(scroll.isSelfHidden()).thenAnswer(i->hidden.get());
        when(scroll.getType()).thenAnswer(i->type.get());
        doAnswer(i->{type.set(i.getArgument(0));return null;}).when(scroll).setType(anyInt());
        when(client.createSpritePixels(any(int[].class),eq(488),eq(334))).thenReturn(skin);
        when(scroll.isHidden()).thenAnswer(i->hidden.get());
        when(scroll.getBounds()).thenReturn(new java.awt.Rectangle(100,50,488,334));
        when(scroll.setHidden(anyBoolean())).thenAnswer(i->{hidden.set(i.getArgument(0));return scroll;});

        when(client.getWidget(InterfaceID.Questscroll.QUEST_REWARD1)).thenReturn(reward);
        when(client.getWidget(InterfaceID.Questscroll.QUEST_MODEL)).thenReturn(model);
        when(client.getWidget(InterfaceID.Questscroll.ROSETTE)).thenReturn(ribbon);
        when(client.getWidget(InterfaceID.Questscroll.CLOSE_BUTTON)).thenReturn(close);
        when(reward.getText()).thenReturn("1 Quest Point");when(reward.getTextColor()).thenAnswer(i->color.get());
        when(reward.getTextShadowed()).thenAnswer(i->shadow.get());
        when(reward.setTextColor(anyInt())).thenAnswer(i->{color.set(i.getArgument(0));return reward;});
        when(reward.setTextShadowed(anyBoolean())).thenAnswer(i->{shadow.set(i.getArgument(0));return reward;});
        Injector injector=Guice.createInjector(new AbstractModule(){@Override protected void configure(){
            bind(Client.class).toInstance(client);bind(OneManSyncConfig.class).toInstance(config);bind(OverlayManager.class).toInstance(overlays);
        }});
        theme=injector.getInstance(QuestWindowTheme.class);theme.startUp();
    }
    @Test public void onlyBackgroundAndColorsChangeThenRestore(){

        assertTrue(theme.apply());assertEquals(net.runelite.api.widgets.WidgetType.GRAPHIC,type.get());assertNotNull(theme.backgroundBounds());
        assertEquals(QuestWindowTheme.CREAM,color.get());assertTrue(shadow.get());assertEquals("1 Quest Point",reward.getText());
        verify(reward,never()).setText(anyString());verify(scroll,never()).setHidden(anyBoolean());
        verify(scroll,never()).setWidth(anyInt());verify(scroll,never()).setHeight(anyInt());
        verifyNoInteractions(model,ribbon,close);
        when(config.alarmQuests()).thenReturn(false);theme.configChanged();
        assertFalse(hidden.get());assertEquals(0x123456,color.get());assertFalse(shadow.get());theme.shutDown();
    }
    @Test public void closingRemovesOnlyOwnOverrideAndRespectsAnotherPlugin(){
        assertTrue(theme.apply());theme.tick();assertNotNull(theme.backgroundBounds());assertSame(skin,sprites.get(InterfaceID.Questscroll.SCROLL));theme.closed();assertEquals(net.runelite.api.widgets.WidgetType.MODEL,type.get());assertFalse(hidden.get());assertNull(theme.backgroundBounds());
        assertEquals(0x123456,color.get());
        assertTrue(theme.apply());SpritePixels other=mock(SpritePixels.class);sprites.put(InterfaceID.Questscroll.SCROLL,other);
        theme.shutDown();assertFalse(hidden.get());assertSame(other,sprites.get(InterfaceID.Questscroll.SCROLL));
    }
    @Test public void hiddenContentSiblingDoesNotBlockVisibleCompletion(){
        when(content.isHidden()).thenReturn(true);
        assertTrue(theme.apply());assertSame(skin,sprites.get(InterfaceID.Questscroll.SCROLL));
        for(int i=0;i<50;i++)theme.tick();
        assertSame(skin,sprites.get(InterfaceID.Questscroll.SCROLL));
        assertEquals(net.runelite.api.widgets.WidgetType.GRAPHIC,type.get());
        theme.shutDown();assertEquals(net.runelite.api.widgets.WidgetType.MODEL,type.get());
        assertFalse(sprites.containsKey(InterfaceID.Questscroll.SCROLL));
    }
    @Test public void lateWindowIsDetectedWithoutLoadEvent(){
        when(client.getWidget(InterfaceID.Questscroll.SCROLL)).thenReturn(null);
        for(int i=0;i<20;i++)theme.tick();
        assertFalse(hidden.get());
        when(client.getWidget(InterfaceID.Questscroll.SCROLL)).thenReturn(scroll);
        theme.tick();assertEquals(net.runelite.api.widgets.WidgetType.GRAPHIC,type.get());assertNotNull(theme.backgroundBounds());
        when(client.getWidget(InterfaceID.Questscroll.SCROLL)).thenReturn(null);theme.tick();assertFalse(hidden.get());assertNull(theme.backgroundBounds());
    }
    @Test public void disabledOrAbsentScrollNeverGetsReplaced(){
        when(config.alarmQuests()).thenReturn(false);assertFalse(theme.apply());assertTrue(sprites.isEmpty());
        when(config.alarmQuests()).thenReturn(true);when(scroll.isHidden()).thenReturn(true);assertFalse(theme.apply());
        theme.shutDown();verifyNoInteractions(reward,model,ribbon,close);
    }
}
