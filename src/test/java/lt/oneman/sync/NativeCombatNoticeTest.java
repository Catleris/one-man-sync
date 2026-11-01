package lt.oneman.sync;

import com.google.inject.*;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class NativeCombatNoticeTest {
    @Test public void hidesOnlyReplacedCombatTaskAndRestoresOnDisableOrDifferentNotice(){
        Client client=mock(Client.class);Widget container=mock(Widget.class),title=mock(Widget.class);
        when(client.getWidget(InterfaceID.NotificationDisplay.CONTAINER)).thenReturn(container);
        when(client.getWidget(InterfaceID.NotificationDisplay.TITLE_TEXT)).thenReturn(title);
        when(title.getText()).thenReturn("<col=ffff00>Combat Task Completed!</col>");
        NativeCombatNotice notice=Guice.createInjector(new AbstractModule(){protected void configure(){bind(Client.class).toInstance(client);}}).getInstance(NativeCombatNotice.class);
        notice.update(false);verify(container,never()).setHidden(anyBoolean());
        notice.update(true);verify(container).setHidden(true);
        when(title.getText()).thenReturn("Collection log");
        notice.update(true);verify(container).setHidden(false);
        clearInvocations(container);notice.update(true);verify(container,never()).setHidden(anyBoolean());
        when(title.getText()).thenReturn("Combat Task Completed!");
        notice.update(true);notice.restore();verify(container).setHidden(false);
    }
    @Test public void leavesAnAlreadyHiddenNativeWidgetUntouched(){
        Client client=mock(Client.class);Widget container=mock(Widget.class),title=mock(Widget.class);
        when(client.getWidget(InterfaceID.NotificationDisplay.CONTAINER)).thenReturn(container);
        when(client.getWidget(InterfaceID.NotificationDisplay.TITLE_TEXT)).thenReturn(title);
        when(container.isSelfHidden()).thenReturn(true);when(title.getText()).thenReturn("Combat Task Completed!");
        NativeCombatNotice notice=Guice.createInjector(new AbstractModule(){protected void configure(){bind(Client.class).toInstance(client);}}).getInstance(NativeCombatNotice.class);
        notice.update(true);notice.restore();verify(container,never()).setHidden(anyBoolean());
    }
}
