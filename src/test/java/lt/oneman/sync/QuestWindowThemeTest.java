package lt.oneman.sync;

import com.google.inject.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.*;
import net.runelite.client.ui.overlay.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class QuestWindowThemeTest {
    private final Client client=mock(Client.class);
    private final OneManSyncConfig config=mock(OneManSyncConfig.class);
    private final OverlayManager overlays=mock(OverlayManager.class);
    private final Widget scroll=mock(Widget.class),reward=mock(Widget.class),model=mock(Widget.class),
        ribbon=mock(Widget.class),close=mock(Widget.class);
    private final AtomicInteger type=new AtomicInteger(WidgetType.MODEL),color=new AtomicInteger(0x123456);
    private QuestWindowTheme theme;
    private QuestWindowBackgroundOverlay background;
    @Before public void setup(){
        when(config.alarmQuests()).thenReturn(true);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getWidget(InterfaceID.Questscroll.SCROLL)).thenReturn(scroll);
        when(scroll.getWidth()).thenReturn(488);when(scroll.getHeight()).thenReturn(334);
        when(scroll.getBounds()).thenReturn(new Rectangle(30,20,488,334));
        when(scroll.getType()).thenAnswer(i->type.get());
        doAnswer(i->{type.set(i.getArgument(0));return null;}).when(scroll).setType(anyInt());
        // Real legacy widgets cannot be assumed to support the dynamic-child path.
        when(scroll.createChild(anyInt(),anyInt())).thenThrow(new IllegalStateException("legacy widget"));
        when(client.getWidget(InterfaceID.Questscroll.QUEST_REWARD1)).thenReturn(reward);
        when(client.getWidget(InterfaceID.Questscroll.QUEST_MODEL)).thenReturn(model);
        when(client.getWidget(InterfaceID.Questscroll.ROSETTE)).thenReturn(ribbon);
        when(client.getWidget(InterfaceID.Questscroll.CLOSE_BUTTON)).thenReturn(close);
        when(reward.getTextColor()).thenAnswer(i->color.get());
        when(reward.setTextColor(anyInt())).thenAnswer(i->{color.set(i.getArgument(0));return reward;});
        Injector injector=Guice.createInjector(new AbstractModule(){@Override protected void configure(){
            bind(Client.class).toInstance(client);bind(OneManSyncConfig.class).toInstance(config);
            bind(OverlayManager.class).toInstance(overlays);
        }});
        theme=injector.getInstance(QuestWindowTheme.class);
        background=injector.getInstance(QuestWindowBackgroundOverlay.class);
        theme.startUp();
    }
    @Test public void nativeLayerProducesOpaquePixelsAndPersistsBeyondPreviewDuration(){
        assertEquals(OverlayLayer.MANUAL,background.getLayer());
        assertTrue(background.getDrawHooks().contains(InterfaceID.Questscroll.SCROLL));
        assertTrue(theme.apply());
        BufferedImage canvas=new BufferedImage(600,400,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=canvas.createGraphics();
        try {
            for(int i=0;i<60;i++){background.render(graphics);theme.tick();}
        } finally {graphics.dispose();}
        assertEquals(255,canvas.getRGB(270,180)>>>24);
        assertEquals(0,canvas.getRGB(0,0));
        assertEquals(WidgetType.LAYER,type.get());
        verify(reward,never()).setText(anyString());verifyNoInteractions(model,ribbon,close);
        verify(scroll,never()).setHidden(anyBoolean());
        verify(scroll,never()).createChild(anyInt(),anyInt());
        theme.shutDown();assertEquals(WidgetType.MODEL,type.get());assertEquals(0x123456,color.get());
    }
    @Test public void tinyLegacyModelViewportUsesNativeTextAnchorAndStillPaintsOpaqueBackground(){
        when(scroll.getWidth()).thenReturn(1);when(scroll.getHeight()).thenReturn(1);
        when(scroll.getBounds()).thenReturn(new Rectangle(274,187,1,1));
        Widget heading=mock(Widget.class);
        when(client.getWidget(InterfaceID.Questscroll.CONGRATULATIONS)).thenReturn(heading);
        when(heading.getBounds()).thenReturn(new Rectangle(150,75,248,20));
        assertTrue(theme.apply());
        assertEquals(new Rectangle(30,20,488,334),theme.backgroundBounds());
        BufferedImage canvas=new BufferedImage(600,400,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=canvas.createGraphics();
        try{background.render(g);}finally{g.dispose();}
        assertEquals(255,canvas.getRGB(270,180)>>>24);
        theme.tick();assertTrue(theme.status().contains("model 1x1 paint 488x334"));
    }
    @Test public void missingDrawCallbackRestoresOriginalInsteadOfLeavingTransparency(){
        assertTrue(theme.apply());
        theme.tick();theme.tick();theme.tick();
        assertEquals(WidgetType.MODEL,type.get());assertNull(theme.backgroundBounds());
        assertTrue(theme.status().contains("original restored"));
        for(int i=0;i<20;i++)theme.tick();
        assertEquals(WidgetType.MODEL,type.get());
        theme.configChanged();assertEquals(WidgetType.LAYER,type.get());
    }
    @Test public void closeDisableAndLateOpenUseRealWidgetLifetime(){
        when(client.getWidget(InterfaceID.Questscroll.SCROLL)).thenReturn(null);
        for(int i=0;i<20;i++)theme.tick();
        when(client.getWidget(InterfaceID.Questscroll.SCROLL)).thenReturn(scroll);
        theme.tick();assertNotNull(theme.backgroundBounds());
        when(config.alarmQuests()).thenReturn(false);theme.configChanged();
        assertEquals(WidgetType.MODEL,type.get());assertNull(theme.backgroundBounds());
        when(config.alarmQuests()).thenReturn(true);theme.configChanged();
        theme.closed();assertEquals(WidgetType.MODEL,type.get());
    }
}
