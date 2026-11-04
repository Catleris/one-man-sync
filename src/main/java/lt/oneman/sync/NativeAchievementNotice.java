package lt.oneman.sync;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

/** Suppresses only native popups replaced by real queued OneMan notices. */
@Singleton
final class NativeAchievementNotice {
    @Inject private Client client;
    private Widget hidden;
    void update(boolean replacingTask, boolean replacingCollection){
        Widget container=client.getWidget(InterfaceID.NotificationDisplay.CONTAINER);
        Widget title=client.getWidget(InterfaceID.NotificationDisplay.TITLE_TEXT);
        String text=title==null||title.getText()==null?"":Text.removeTags(title.getText()).trim();
        boolean matches=(replacingTask && ("Combat Task Completed!".equalsIgnoreCase(text)
            ||"Combat Task Completed".equalsIgnoreCase(text)))
            ||(replacingCollection && "Collection log".equalsIgnoreCase(text));
        if(!matches||container==null){restore();return;}
        if(hidden!=container){restore();if(container.isSelfHidden())return;hidden=container;}
        container.setHidden(true);
    }
    void restore(){
        if(hidden!=null){hidden.setHidden(false);hidden=null;}
    }
}
