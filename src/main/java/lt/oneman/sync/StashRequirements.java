package lt.oneman.sync;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.plugins.cluescrolls.clues.EmoteClue;
import net.runelite.client.plugins.cluescrolls.clues.item.ItemRequirement;
import net.runelite.client.plugins.cluescrolls.clues.item.SlotLimitationRequirement;
import net.runelite.client.util.Text;

/** Client-thread snapshots only; bank contents are never guessed or uploaded. */
final class StashRequirements
{
    enum State
    {
        CARRIED("Inventory / equipped",new Color(100,220,135)),
        BANK("Bank (last seen)",new Color(240,202,80)),
        MISSING("Missing",new Color(240,105,105)),
        UNKNOWN("Bank unknown",OneManCards.MUTED);
        final String label; final Color color;
        State(String label,Color color) { this.label=label;this.color=color; }
    }
    static final class Row
    {
        final String name; final State state;
        Row(String name,State state) {this.name=name;this.state=state;}
    }
    private final Map<String,List<ItemRequirement>> requirements=new LinkedHashMap<>();
    private final Map<ItemRequirement,String> names=new IdentityHashMap<>();
    private Item[] bank=new Item[0];
    private boolean bankKnown;
    private String profile;
    StashRequirements()
    {
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(Objects.requireNonNull(
            getClass().getResourceAsStream("clue-emote-steps.tsv")),StandardCharsets.UTF_8)))
        {
            String line;
            while((line=reader.readLine())!=null) {
                if(line.isEmpty() || line.startsWith("#")) continue;
                EmoteClue clue=EmoteClue.forText(line.split("\t",2)[1]);
                if(clue==null || clue.getStashUnit()==null) continue;
                for(ItemRequirement requirement:clue.getItemRequirements())
                    if(!(requirement instanceof SlotLimitationRequirement))
                        requirements.computeIfAbsent(clue.getStashUnit().name(),key->new ArrayList<>()).add(requirement);
            }
        } catch(IOException error) {throw new IllegalStateException("Unable to read STASH requirements",error);}
    }
    void clear() {bank=new Item[0];bankKnown=false;profile=null;}
    Map<String,List<Row>> snapshot(Client client,String currentProfile,boolean bankChanged)
    {
        if(!Objects.equals(profile,currentProfile)) {clear();profile=currentProfile;}
        if(client.getGameState()!=GameState.LOGGED_IN) return Collections.emptyMap();
        if(bankChanged) {
            ItemContainer container=client.getItemContainer(InventoryID.BANK);
            if(container!=null) {bank=usable(client,container.getItems());bankKnown=true;}
        }
        List<Item> carried=new ArrayList<>();
        for(int containerId:new int[]{InventoryID.INV,InventoryID.WORN}) {
            ItemContainer container=client.getItemContainer(containerId);
            if(container!=null) Collections.addAll(carried,usable(client,container.getItems()));
        }
        Item[] held=carried.toArray(new Item[0]);
        Collections.addAll(carried,bank);
        Item[] owned=carried.toArray(new Item[0]);
        Map<String,List<Row>> result=new LinkedHashMap<>();
        requirements.forEach((id,list)-> {
            Map<String,Row> rows=new LinkedHashMap<>();
            for(ItemRequirement requirement:list) {
                String name=names.computeIfAbsent(requirement,key->Text.removeTags(key.getCollectiveName(client)));
                State state=state(requirement,held,owned,bankKnown);
                rows.putIfAbsent(name,new Row(name,state));
            }
            result.put(id,Collections.unmodifiableList(new ArrayList<>(rows.values())));
        });
        return Collections.unmodifiableMap(result);
    }
    static State state(ItemRequirement requirement,Item[] carried,Item[] owned,boolean bankKnown)
    {
        if(requirement.fulfilledBy(carried)) return State.CARRIED;
        if(requirement.fulfilledBy(owned)) return State.BANK;
        return bankKnown?State.MISSING:State.UNKNOWN;
    }
    private static Item[] usable(Client client,Item[] source)
    {
        List<Item> result=new ArrayList<>();
        for(Item item:source) {
            if(item==null || item.getId()<0 || item.getQuantity()<=0) continue;
            ItemComposition definition=client.getItemDefinition(item.getId());
            if(definition.getPlaceholderTemplateId()!=-1) continue;
            int id=definition.getNote()!=-1?definition.getLinkedNoteId():item.getId();
            result.add(new Item(id,item.getQuantity()));
        }
        return result.toArray(new Item[0]);
    }
}
