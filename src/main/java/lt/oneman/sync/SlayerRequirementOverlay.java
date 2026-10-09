package lt.oneman.sync;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.*;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** Passive preparation hints, calculated on the client thread. */
final class SlayerRequirementOverlay extends OverlayPanel
{
    static final class Row {
        final String label, status; final Color color;
        Row(String label, String status, Color color) { this.label=label;this.status=status;this.color=color; }
    }
    @Inject private OneManSyncConfig config;
    volatile List<Row> rows=Collections.emptyList();
    SlayerRequirementOverlay() { setPosition(OverlayPosition.TOP_LEFT); }
    static Row row(SlayerLabCatalog.Requirement requirement, Set<String> inventory, Set<String> equipment,
                   Map<String,Integer> bank, boolean bankKnown) {
        if(equipment.stream().anyMatch(requirement::matches))return new Row(requirement.label,"Equipped",Color.GREEN);
        if(inventory.stream().anyMatch(requirement::matches))return new Row(requirement.label,requirement.equip?"Equip":"Carried",Color.GREEN);
        if(bankKnown && bank.entrySet().stream().anyMatch(e->e.getValue()>0&&requirement.matches(e.getKey())))
            return new Row(requirement.label,"In bank",new Color(190,130,255));
        return new Row(requirement.label,bankKnown?"Missing":"Bank unknown",bankKnown?new Color(255,90,90):Color.GRAY);
    }
    @Override public Dimension render(Graphics2D graphics) {
        if(!config.slayerRequirementsOverlay()||rows.isEmpty())return null;
        panelComponent.getChildren().add(TitleComponent.builder().text("Slayer items").build());
        for(Row row:rows) {
            panelComponent.getChildren().add(LineComponent.builder().left(row.label).leftColor(row.color).build());
            panelComponent.getChildren().add(LineComponent.builder().left("  "+row.status).leftColor(row.color).build());
        }
        return super.render(graphics);
    }
}
