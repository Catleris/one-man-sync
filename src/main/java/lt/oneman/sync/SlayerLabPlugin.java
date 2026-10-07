package lt.oneman.sync;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.inject.Inject;
import javax.swing.*;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.slayer.SlayerConfig;
import net.runelite.client.plugins.slayer.SlayerPlugin;
import net.runelite.client.ui.*;
import net.runelite.client.util.LinkBrowser;

@PluginDescriptor(name = "OneMan Slayer Lab", description = "Local Slayer preparation and locations — development copy", tags = {"slayer", "oneman", "lab"}, internalName = "one-man-slayer-lab", enabledByDefault = true)
@PluginDependency(SlayerPlugin.class)
public class SlayerLabPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ConfigManager configManager;
    @Inject private ItemManager itemManager;
    @Inject private ClientToolbar toolbar;
    @Inject private ClientThread clientThread;
    @Inject private OverlayManager overlayManager;
    @Inject private WorldMapPointManager mapManager;
    @Inject private SlayerLabMinimapOverlay minimapOverlay;
    private final java.util.List<SlayerLabMapPoint> mapPoints = new ArrayList<>();
    private java.util.List<SlayerLabLocations.Destination> destinations = Collections.emptyList();
    private volatile SlayerLabLocations.Destination selectedDestination;
    private String taskSignature = "";
    private NavigationButton navigation;
    private LabPanel panel;
    private String lastView = "";
    private int nextRefresh;
    private volatile boolean active;

    @Override protected void startUp()
    {
        active = true;
        overlayManager.add(minimapOverlay);
        nextRefresh = 0;
        lastView = "";
        SwingUtilities.invokeLater(() -> {
            if (!active) return;
            panel = new LabPanel();
            BufferedImage icon = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = icon.createGraphics();
            g.setColor(new Color(218, 176, 85));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g.drawString("S", 6, 19);
            g.dispose();
            navigation = NavigationButton.builder().tooltip("OneMan Slayer Lab").icon(icon).priority(6).panel(panel).build();
            toolbar.addNavigation(navigation);
            panel.showView("Log in to detect your Slayer task.", "");
        });
    }
    @Override protected void shutDown()
    {
        active = false;
        overlayManager.remove(minimapOverlay);
        clearLocations();
        SwingUtilities.invokeLater(() -> {
            if (navigation != null) toolbar.removeNavigation(navigation);
            navigation = null;
            panel = null;
        });
        lastView = "";
    }
    @Subscribe public void onGameStateChanged(GameStateChanged event)
    {
        nextRefresh = 0;
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            clearLocations();
            lastView = "";
            publish("Log in to detect your Slayer task.", "");
        }
    }
    @Subscribe public void onGameTick(GameTick event)
    {
        if (!active || client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) return;
        for (int i = 0; i < mapPoints.size(); i++)
            mapPoints.get(i).highlight(destinations.get(i) == selectedDestination, (client.getTickCount() / 2) % 2 == 0);
        if (client.getTickCount() < nextRefresh) return;
        nextRefresh = client.getTickCount() + 5;
        // Built-in Slayer handles assignment DB decoding, messages and profile changes.
        String name = configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME, SlayerConfig.TASK_NAME_KEY);
        String location = configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME, SlayerConfig.TASK_LOC_KEY);
        int remaining = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
        if (remaining <= 0 || name == null || name.trim().isEmpty())
        {
            clearLocations();
            publish("No active task detected. Ask a Slayer master or check your enchanted gem. Keep RuneLite's Slayer plugin enabled.", "");
            return;
        }
        // Do not display an old profile name while the built-in plugin catches up.
        String profileAmount = configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME, SlayerConfig.AMOUNT_KEY);
        if (!Integer.toString(remaining).equals(profileAmount)) return;
        String signature = SlayerLabCatalog.normalize(name) + "|" + SlayerLabCatalog.normalize(location);
        if (!signature.equals(taskSignature))
        {
            clearLocations();
            taskSignature = signature;
            destinations = SlayerLabLocations.forTask(name, location);
            for (SlayerLabLocations.Destination destination : destinations)
            {
                SlayerLabMapPoint point = new SlayerLabMapPoint(destination);
                mapPoints.add(point);
                mapManager.add(point);
            }
        }
        Set<String> inventory = itemNames(InventoryID.INV);
        Set<String> equipment = itemNames(InventoryID.WORN);
        SlayerLabCatalog.Guide guide = SlayerLabCatalog.find(name);
        StringBuilder out = new StringBuilder(name + "\nRemaining: " + remaining + "\n");
        boolean restricted = location != null && !location.trim().isEmpty();
        if (restricted) out.append("\nASSIGNED LOCATION\n").append(location).append("\nKills must count in this assigned area. Generic locations below are reference only.\n");
        if (guide == null)
        {
            out.append("\nNo curated guide for this task yet. Use Task Wiki to check its equipment and locations; no requirements are guessed.");
        }
        else
        {
            out.append("\nREQUIRED EQUIPMENT\n");
            if (guide.requirements.isEmpty()) out.append("No automatic equipment checklist for this task. Check the preparation notes below.\n");
            for (SlayerLabCatalog.Requirement req : guide.requirements)
                out.append(req.label).append("\n").append(req.status(inventory, equipment)).append("\n\n");
            out.append("\nPREPARATION\n").append(guide.advice);
            out.append("\n\n").append(restricted ? "OTHER LOCATIONS (REFERENCE ONLY)" : "WHERE TO FIND THEM").append("\n").append(guide.locations);
        }
        if (destinations.isEmpty())
            out.append("\n\nMAP\nNo verified entrance marker for this task / assigned area yet. Use the Wiki.");
        else
            out.append("\n\nMAP\nChoose an entrance below, open the world map and press Centre map. Blue pins show alternatives; orange pins indicate Wilderness. Track entrance adds a pulsing marker and minimap direction arrow. Access conditions are not automatically verified.");
        SlayerLabLocations.Destination selected = selectedDestination;
        if (selected != null)
        {
            out.append("\n\nTRACKING: ").append(selected).append("\n").append(selected.access);
            WorldPoint player = client.getLocalPlayer().getWorldLocation();
            if (client.getTopLevelWorldView().isInstance() || (player.getY() >= 6400) != (selected.entrance.getY() >= 6400))
                out.append("\nDifferent map area: follow the dungeon notes. No cross-floor arrow.");
            else
                out.append("\nDirection: ").append(SlayerLabLocations.direction(player, selected.entrance))
                    .append(" | direct distance: ").append(player.distanceTo(selected.entrance)).append(" tiles");
            out.append("\nDestination/direction only: not a calculated walking path. At the entrance, follow the chamber/floor notes above.");
        }
        out.append("\n\nItem checks use carried, unnoted items only. Bank ownership, quest access, charges and consumable quantities are not verified.\n\nLocal helper • Wiki opens only when clicked.");
        publish(out.toString(), SlayerLabCatalog.wiki(name));
    }
    WorldPoint navigationTarget()
    {
        SlayerLabLocations.Destination selected = selectedDestination;
        return active && selected != null ? selected.entrance : null;
    }
    private void clearLocations()
    {
        for (SlayerLabMapPoint point : mapPoints) mapManager.remove(point);
        mapPoints.clear();
        destinations = Collections.emptyList();
        selectedDestination = null;
        taskSignature = "";
    }
    private void track(SlayerLabLocations.Destination destination, boolean centre)
    {
        clientThread.invokeLater(() -> {
            if (!active || !destinations.contains(destination)) return;
            if (centre)
                client.getWorldMap().setWorldMapPositionTarget(destination.entrance);
            else
                selectedDestination = destination;
            nextRefresh = 0;
        });
    }
    private Set<String> itemNames(int containerId)
    {
        Set<String> result = new HashSet<>();
        ItemContainer container = client.getItemContainer(containerId);
        if (container == null) return result;
        for (Item item : container.getItems())
        {
            if (item.getId() < 0 || item.getQuantity() <= 0) continue;
            ItemComposition definition = itemManager.getItemComposition(item.getId());
            if (definition.getNote() != -1) continue;
            result.add(SlayerLabCatalog.normalize(definition.getName()));
        }
        return result;
    }
    private void publish(String view, String url)
    {
        if (view.equals(lastView)) return;
        lastView = view;
        java.util.List<SlayerLabLocations.Destination> snapshot = destinations;
        SwingUtilities.invokeLater(() -> {
            if (active && panel != null)
            {
                panel.showView(view, url);
                panel.showLocations(snapshot);
            }
        });
    }
    private final class LabPanel extends PluginPanel
    {
        private final JTextArea text = new JTextArea();
        private final JButton wiki = new JButton("Task Wiki");
        private String url = "";
        private final JComboBox<SlayerLabLocations.Destination> choices = new JComboBox<>();
        private final JTextArea access = new JTextArea();
        private final JButton centre = new JButton("Centre map");
        private final JButton track = new JButton("Track entrance");
        private final JButton clear = new JButton("Stop tracking");
        private java.util.List<SlayerLabLocations.Destination> shown = Collections.emptyList();
        LabPanel()
        {
            setLayout(new BorderLayout(0, 10));
            setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
            JLabel title = new JLabel("ONEMAN • SLAYER LAB 0.2");
            title.setForeground(new Color(218, 176, 85));

            text.setColumns(20);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            text.setForeground(new Color(225, 225, 225));
            text.setBackground(new Color(30, 30, 30));
            text.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
            add(text, BorderLayout.CENTER);
            wiki.addActionListener(e -> { if (!url.isEmpty()) LinkBrowser.browse(url); });
            JPanel controls = new JPanel();
            controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
            controls.add(new JLabel("DESTINATION / ENTRANCE"));
            choices.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            controls.add(choices);
            access.setColumns(20);
            access.setEditable(false);
            access.setLineWrap(true);
            access.setWrapStyleWord(true);
            access.setOpaque(false);
            controls.add(access);
            for (JButton button : new JButton[]{centre, track, clear, wiki})
            {
                button.setAlignmentX(Component.LEFT_ALIGNMENT);
                button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                controls.add(button);
            }
            choices.addActionListener(e -> {
                SlayerLabLocations.Destination choice = (SlayerLabLocations.Destination) choices.getSelectedItem();
                access.setText(choice == null ? "No mapped entrance available." : choice.access);
            });
            centre.addActionListener(e -> {
                SlayerLabLocations.Destination choice = (SlayerLabLocations.Destination) choices.getSelectedItem();
                if (choice != null) SlayerLabPlugin.this.track(choice, true);
            });
            track.addActionListener(e -> {
                SlayerLabLocations.Destination choice = (SlayerLabLocations.Destination) choices.getSelectedItem();
                if (choice != null) SlayerLabPlugin.this.track(choice, false);
            });
            clear.addActionListener(e -> clientThread.invokeLater(() -> { selectedDestination = null; nextRefresh = 0; }));
            JPanel header = new JPanel(new BorderLayout(0, 10));
            header.add(title, BorderLayout.NORTH);
            header.add(controls, BorderLayout.CENTER);
            add(header, BorderLayout.NORTH);
        }
        void showLocations(java.util.List<SlayerLabLocations.Destination> locations)
        {
            if (shown.equals(locations)) return;
            shown = locations;
            choices.removeAllItems();
            for (SlayerLabLocations.Destination destination : locations) choices.addItem(destination);
            boolean hasLocations = !locations.isEmpty();
            choices.setEnabled(hasLocations);
            centre.setEnabled(hasLocations);
            track.setEnabled(hasLocations);
            clear.setEnabled(hasLocations);
            if (!hasLocations) access.setText("No mapped entrance available.");
        }
        void showView(String view, String link)
        {
            int caret = text.getCaretPosition();
            text.setText(view);
            text.setCaretPosition(Math.min(caret, view.length()));
            url = link;
            wiki.setEnabled(!link.isEmpty());
        }
    }
}
