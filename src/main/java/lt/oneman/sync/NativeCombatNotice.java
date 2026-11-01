package lt.oneman.sync;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

/** Suppresses only the native task popup that our queued CA banner replaces. */
@Singleton
final class NativeCombatNotice {
    @Inject private Client client;
    private Widget hidden;
    void update(boolean replacingTask){
        Widget container=client.getWidget(InterfaceID.NotificationDisplay.CONTAINER);
        Widget title=client.getWidget(InterfaceID.NotificationDisplay.TITLE_TEXT);
        String text=title==null||title.getText()==null?"":Text.removeTags(title.getText()).trim();
        boolean matches="Combat Task Completed!".equalsIgnoreCase(text)
            ||"Combat Task Completed".equalsIgnoreCase(text);
        if(!replacingTask||!matches||container==null){restore();return;}
        if(hidden!=container){restore();if(container.isSelfHidden())return;hidden=container;}
        container.setHidden(true);
    }
    void restore(){
        if(hidden!=null){hidden.setHidden(false);hidden=null;}
    }
}
