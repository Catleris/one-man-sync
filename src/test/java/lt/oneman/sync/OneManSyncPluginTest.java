package lt.oneman.sync;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OneManSyncPluginTest
{
    public static void main(String[] args) throws Exception
    {
        if (!Boolean.getBoolean("oneman.devHomeConfigured"))
        {
            System.setProperty("user.home", java.nio.file.Paths.get(System.getProperty("user.home"), ".oneman-companion-dev").toString());
        }
        ExternalPluginManager.loadBuiltin(OneManSyncPlugin.class);
        RuneLite.main(args);
    }
}
