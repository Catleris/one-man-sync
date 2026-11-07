package lt.oneman.sync;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Singleton
final class GuardiansFightGuide extends OverlayPanel
{
    enum Phase
    {
        DAWN("Dawn", "Ranged / Magic / Halberd", "Watch for Dawn's stun projectile.", ""),
        DUSK("Dusk", "Melee", "Watch for rockfall and the wing attack.", "Dusk → step one tile away"),
        SPHERES("Dawn", "Ranged / Magic / Halberd", "HEALING SPHERES: collect them if Dawn will survive. Finish Dawn with a hammer.", ""),
        PRISON("Dusk", "Melee", "Leave the flame prison through its gap. Finish Dusk with a hammer.", "Inside → gap in flames → outside");

        final String target, style, reminder, diagram;
        Phase(String target, String style, String reminder, String diagram)
        {
            this.target = target;
            this.style = style;
            this.reminder = reminder;
            this.diagram = diagram;
        }

        String title() { return "Phase " + (ordinal() + 1) + " / 4  ·  " + target; }
    }

    private static final Phase[] PHASES = Phase.values();
    private static final int[] HAMMERS = {ItemID.SLAYER_ROCK_HAMMER, ItemID.GRANITE_HAMMER, ItemID.SLAYER_ROCK_THROWNHAMMER};
    private static final String[] NAMES = {"Rock hammer", "Granite hammer", "Rock thrownhammer"};
    private static final Color GOLD = new Color(247, 207, 115);
    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    @Inject private CombatInfoPage combatInfo;
    private volatile Phase phase = Phase.DAWN;
    private volatile String hammer = "Rock hammer · check supplies";

    GuardiansFightGuide()
    {
        setPosition(OverlayPosition.TOP_RIGHT);
        panelComponent.setPreferredSize(new Dimension(211, 0));
    }

    boolean selected() { return config.monstersProfile() == MonstersProfile.GROTESQUE_GUARDIANS; }
    Phase phase() { return phase; }
    String hammer() { return hammer; }
    void reset() { phase = Phase.DAWN; }
    void move(int step) { phase = PHASES[Math.floorMod(phase.ordinal() + step, PHASES.length)]; }

    void updateSupplies()
    {
        if (!selected()) return;
        ItemContainer worn = client.getItemContainer(InventoryID.WORN);
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        for (int i = 0; i < HAMMERS.length; i++)
            if (has(worn, HAMMERS[i]))
            {
                hammer = NAMES[i] + " · Equipped";
                return;
            }
        for (int i = 0; i < HAMMERS.length; i++)
            if (has(inventory, HAMMERS[i]))
            {
                hammer = NAMES[i] + " · Carried";
                return;
            }
        for (int i = 0; i < HAMMERS.length; i++)
            if (combatInfo.bankHas(HAMMERS[i]))
            {
                hammer = NAMES[i] + " · In bank";
                return;
            }
        hammer = "Bring a Rock hammer or equivalent";
    }

    private static boolean has(ItemContainer container, int id)
    {
        if (container == null) return false;
        for (Item item : container.getItems())
            if (item.getId() == id && item.getQuantity() > 0) return true;
        return false;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!selected() || !config.monstersGuardiansOverlay() || client.getGameState() != GameState.LOGGED_IN)
            return null;
        Phase current = phase;
        panelComponent.getChildren().add(TitleComponent.builder().text(current.title()).color(GOLD).build());
        panelComponent.getChildren().add(LineComponent.builder().left(current.style).leftColor(GOLD).build());
        panelComponent.getChildren().add(LineComponent.builder().left(current.reminder)
            .leftColor(current == Phase.SPHERES ? GOLD : Color.WHITE).build());
        if (!current.diagram.isEmpty())
            panelComponent.getChildren().add(LineComponent.builder().left(current.diagram).build());
        panelComponent.getChildren().add(LineComponent.builder().left(hammer).build());
        return super.render(graphics);
    }
}
