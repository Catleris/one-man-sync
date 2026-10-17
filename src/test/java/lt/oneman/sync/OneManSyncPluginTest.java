package lt.oneman.sync;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OneManSyncPluginTest
{
    public static void main(String[] args) throws Exception
    {
        System.out.println("OneMan Sync development build " + OneManCompanion.VERSION);
        ExternalPluginManager.loadBuiltin(OneManSyncPlugin.class);
        RuneLite.main(args);
    }
}
