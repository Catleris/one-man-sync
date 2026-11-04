package lt.oneman.sync;

import com.google.inject.*;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class NativeAchievementNoticeTest {
    @Test public void hidesOnlyReplacedCombatTaskAndRestoresOnDisableOrDifferentNotice(){
        Client client=mock(Client.class);Widget container=mock(Widget.class),title=mock(Widget.class);
        when(client.getWidget(InterfaceID.NotificationDisplay.CONTAINER)).thenReturn(container);
        when(client.getWidget(InterfaceID.NotificationDisplay.TITLE_TEXT)).thenReturn(title);
        when(title.getText()).thenReturn("<col=ffff00>Combat Task Completed!</col>");
        NativeAchievementNotice notice=Guice.createInjector(new AbstractModule(){protected void configure(){bind(Client.class).toInstance(client);}}).getInstance(NativeAchievementNotice.class);
        notice.update(false,false);verify(container,never()).setHidden(anyBoolean());
        notice.update(true,false);verify(container).setHidden(true);
        when(title.getText()).thenReturn("Collection log");
        notice.update(true,false);verify(container).setHidden(false);
        clearInvocations(container);notice.update(true,false);verify(container,never()).setHidden(anyBoolean());
        when(title.getText()).thenReturn("Combat Task Completed!");
        notice.update(true,false);notice.restore();verify(container).setHidden(false);
    }
    @Test public void leavesAnAlreadyHiddenNativeWidgetUntouched(){
        Client client=mock(Client.class);Widget container=mock(Widget.class),title=mock(Widget.class);
        when(client.getWidget(InterfaceID.NotificationDisplay.CONTAINER)).thenReturn(container);
        when(client.getWidget(InterfaceID.NotificationDisplay.TITLE_TEXT)).thenReturn(title);
        when(container.isSelfHidden()).thenReturn(true);when(title.getText()).thenReturn("Combat Task Completed!");
        NativeAchievementNotice notice=Guice.createInjector(new AbstractModule(){protected void configure(){bind(Client.class).toInstance(client);}}).getInstance(NativeAchievementNotice.class);
        notice.update(true,false);notice.restore();verify(container,never()).setHidden(anyBoolean());
    }
    @Test public void collectionReplacementRestoresOnDisableAndLeavesOtherNoticesVisible(){
        Client client=mock(Client.class);Widget container=mock(Widget.class),title=mock(Widget.class);
        when(client.getWidget(InterfaceID.NotificationDisplay.CONTAINER)).thenReturn(container);
        when(client.getWidget(InterfaceID.NotificationDisplay.TITLE_TEXT)).thenReturn(title);
        when(title.getText()).thenReturn("<col=ffff00>Collection log</col>");
        NativeAchievementNotice notice=Guice.createInjector(new AbstractModule(){protected void configure(){bind(Client.class).toInstance(client);}}).getInstance(NativeAchievementNotice.class);
        notice.update(true,false);verify(container,never()).setHidden(anyBoolean());
        notice.update(false,true);verify(container).setHidden(true);
        notice.update(false,false);verify(container).setHidden(false);
        clearInvocations(container);
        notice.update(false,true);verify(container).setHidden(true);
        when(title.getText()).thenReturn("A different notification");
        notice.update(true,true);verify(container).setHidden(false);
        clearInvocations(container);notice.update(true,true);verify(container,never()).setHidden(anyBoolean());
    }
    @Test public void collectionReplacementDoesNotUnhideAnAlreadyHiddenWidget(){
        Client client=mock(Client.class);Widget container=mock(Widget.class),title=mock(Widget.class);
        when(client.getWidget(InterfaceID.NotificationDisplay.CONTAINER)).thenReturn(container);
        when(client.getWidget(InterfaceID.NotificationDisplay.TITLE_TEXT)).thenReturn(title);
        when(title.getText()).thenReturn("Collection log");when(container.isSelfHidden()).thenReturn(true);
        NativeAchievementNotice notice=Guice.createInjector(new AbstractModule(){protected void configure(){bind(Client.class).toInstance(client);}}).getInstance(NativeAchievementNotice.class);
        notice.update(false,true);notice.restore();verify(container,never()).setHidden(anyBoolean());
    }
}
