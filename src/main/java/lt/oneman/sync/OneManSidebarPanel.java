package lt.oneman.sync;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import net.runelite.client.ui.PluginPanel;

/** Fill the provided viewport without asking RuneLite to enlarge a fixed-mode frame. */
final class OneManSidebarPanel extends PluginPanel
{
    OneManSidebarPanel()
    {
        super(false);setLayout(new BorderLayout(0,6));setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
    }
    @Override public Dimension getPreferredSize() { return new Dimension(PANEL_WIDTH+SCROLLBAR_WIDTH,0); }
    @Override public Dimension getMinimumSize() { return new Dimension(PANEL_WIDTH+SCROLLBAR_WIDTH,0); }
}
