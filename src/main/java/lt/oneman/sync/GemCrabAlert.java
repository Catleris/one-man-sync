package lt.oneman.sync;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.Notifier;
import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
final class GemCrabAlert extends Overlay
{
    private static final Color FLASH_COLOR = new Color(67, 224, 195, 64);
    private static final Notification ALERT = Notification.ON.withInitialized(true).withOverride(true)
        .withTray(true).withSendWhenFocused(true).withTimeout(5).withVolume(100)
        .withSound(NotificationSound.NATIVE);

    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    @Inject private Notifier notifier;
    private NPC crab;
    private WorldPoint nest;
    private int despawnTick;
    private boolean flashing;
    private long lastPress;
    private long flashStart;

    GemCrabAlert()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    void tick()
    {
        if (!config.monstersGemCrabAlert()) return;
        Player player = client.getLocalPlayer();
        if (player != null && player.getInteracting() instanceof NPC)
        {
            NPC npc = (NPC) player.getInteracting();
            if (npc.getId() == NpcID.GEMSTONE_CRAB && (npc != crab || despawnTick < 0))
            {
                crab = npc;
                nest = npc.getWorldLocation();
                despawnTick = -1;
                return;
            }
        }

        if (nest != null && (player == null || nest.distanceTo(player.getWorldLocation()) > 12
            || (despawnTick >= 0 && client.getTickCount() - despawnTick > 3)))
        {
            crab = null;
            nest = null;
        }
    }

    void despawned(NPC npc)
    {
        if (npc == crab) despawnTick = client.getTickCount();
    }

    void appeared(NPC npc)
    {
        if (nest == null || !config.monstersGemCrabAlert()
            || npc.getId() != NpcID.GEMSTONE_CRAB_REMAINS
            || (npc != crab && !nest.equals(npc.getWorldLocation())))
        {
            return;
        }

        crab = null;
        nest = null;
        lastPress = client.getMouseLastPressedMillis();
        flashStart = System.nanoTime();
        flashing = true;
        notifier.notify(ALERT, "OneMan: Gemstone Crab burrowed!");
    }

    void reset()
    {
        crab = null;
        nest = null;
        flashing = false;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!flashing || client.getGameState() != GameState.LOGGED_IN) return null;
        if (client.getMouseLastPressedMillis() != lastPress)
        {
            flashing = false;
            return null;
        }
        if ((System.nanoTime() - flashStart) / 650_000_000L % 2 == 0)
        {
            graphics.setColor(FLASH_COLOR);
            graphics.fillRect(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
        }
        return null;
    }
}
