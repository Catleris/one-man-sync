package lt.oneman.sync;

import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.api.MenuAction;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;

/** Route a user click through RuneLite's public overlay configuration action.
 * ConfigPlugin owns the actual configuration panel; no duplicate settings form.
 */
final class OneManSettings
{
    private OneManSettings() {}

    static OverlayMenuClicked request(Plugin plugin)
    {
        // This is only the action's owning-plugin context, never registered or rendered.
        Overlay context = new Overlay(plugin) {
            @Override public Dimension render(Graphics2D graphics) { return null; }
        };
        return new OverlayMenuClicked(
            new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, "Configure", plugin.getName()), context);
    }
}
