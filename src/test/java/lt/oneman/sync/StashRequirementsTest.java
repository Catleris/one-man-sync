package lt.oneman.sync;

import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.cluescrolls.clues.item.ItemRequirement;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StashRequirementsTest
{
    @Test public void prioritisesCarriedThenBankAndDoesNotGuessAnUnseenBank()
    {
        ItemRequirement requirement=mock(ItemRequirement.class);
        Item[] carried={new Item(1,1)},owned={new Item(1,1),new Item(2,1)};
        assertEquals(StashRequirements.State.UNKNOWN,StashRequirements.state(requirement,carried,owned,false));
        assertEquals(StashRequirements.State.MISSING,StashRequirements.state(requirement,carried,owned,true));
        when(requirement.fulfilledBy(owned)).thenReturn(true);
        assertEquals(StashRequirements.State.BANK,StashRequirements.state(requirement,carried,owned,true));
        when(requirement.fulfilledBy(carried)).thenReturn(true);
        assertEquals(StashRequirements.State.CARRIED,StashRequirements.state(requirement,carried,owned,true));
    }
    @Test public void coversEveryStashAndScopesBankToTheAccount()
    {
        Client client=mock(Client.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getItemDefinition(anyInt())).thenAnswer(call->{
            ItemComposition item=mock(ItemComposition.class);
            when(item.getName()).thenReturn("Item "+call.getArgument(0));
            when(item.getNote()).thenReturn(-1);when(item.getPlaceholderTemplateId()).thenReturn(-1);return item;
        });
        StashRequirements tracker=new StashRequirements();
        Map<String,List<StashRequirements.Row>> initial=tracker.snapshot(client,"account-a",false);
        for(OneManMapCatalog.Site site:OneManMapCatalog.stashes()) {
            assertNotNull(site.id,initial.get(site.id));assertFalse(site.id,initial.get(site.id).isEmpty());
        }
        assertTrue(initial.values().stream().flatMap(List::stream).allMatch(row->row.state==StashRequirements.State.UNKNOWN));
        ItemContainer bank=mock(ItemContainer.class);when(bank.getItems()).thenReturn(new Item[0]);
        when(client.getItemContainer(InventoryID.BANK)).thenReturn(bank);
        assertTrue(tracker.snapshot(client,"account-a",true).values().stream().flatMap(List::stream).allMatch(row->row.state==StashRequirements.State.MISSING));
        assertTrue(tracker.snapshot(client,"account-a",false).values().stream().flatMap(List::stream).allMatch(row->row.state==StashRequirements.State.MISSING));
        assertTrue(tracker.snapshot(client,"account-b",false).values().stream().flatMap(List::stream).allMatch(row->row.state==StashRequirements.State.UNKNOWN));
        ItemContainer inventory=mock(ItemContainer.class),equipment=mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
        when(client.getItemContainer(InventoryID.WORN)).thenReturn(equipment);
        when(inventory.getItems()).thenReturn(new Item[]{new Item(ItemID.STEEL_AXE,1)});
        when(equipment.getItems()).thenReturn(new Item[]{new Item(ItemID.DESERT_SHIRT,1)});
        when(bank.getItems()).thenReturn(new Item[]{new Item(ItemID.GNOME_ROBEBOTTOMS_GREEN,1)});
        List<StashRequirements.Row> beehive=tracker.snapshot(client,"account-b",true).get("CATHERBY_BEEHIVE_FIELD");
        assertEquals(3,beehive.size());
        assertEquals(2,beehive.stream().filter(row->row.state==StashRequirements.State.CARRIED).count());
        assertEquals(1,beehive.stream().filter(row->row.state==StashRequirements.State.BANK).count());
        when(client.getItemContainer(InventoryID.BANK)).thenReturn(null);
        assertEquals(1,tracker.snapshot(client,"account-b",false).get("CATHERBY_BEEHIVE_FIELD").stream().filter(row->row.state==StashRequirements.State.BANK).count());
        ItemComposition placeholder=mock(ItemComposition.class);when(placeholder.getPlaceholderTemplateId()).thenReturn(1);
        when(client.getItemDefinition(ItemID.GNOME_ROBEBOTTOMS_GREEN)).thenReturn(placeholder);
        when(client.getItemContainer(InventoryID.BANK)).thenReturn(bank);
        assertEquals(1,tracker.snapshot(client,"account-b",true).get("CATHERBY_BEEHIVE_FIELD").stream().filter(row->row.state==StashRequirements.State.MISSING).count());
        when(inventory.getItems()).thenReturn(new Item[0]);when(equipment.getItems()).thenReturn(new Item[0]);
        tracker.clear();
        assertTrue(tracker.snapshot(client,"account-b",false).values().stream().flatMap(List::stream).allMatch(row->row.state==StashRequirements.State.UNKNOWN));
    }
}
