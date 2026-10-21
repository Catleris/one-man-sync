package lt.oneman.sync;

import com.google.gson.Gson;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.*;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.LinkBrowser;

/** Read-only game evidence plus explicitly separate account-scoped notes. No web dependency. */
@Singleton
final class UnlocksHub
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ConfigManager configs;
    @Inject private OneManSyncConfig config;
    @Inject private ItemManager items;
    @Inject private Gson gson;
    private final UnlockCatalog catalog=new UnlockCatalog();
    static final class Saved
    {
        Set<String> manual=new HashSet<>(), favourites=new HashSet<>();
        Map<String,Long> observed=new HashMap<>();
    }
    private Saved saved=new Saved();
    private String profile="";
    private boolean active,dirty;
    private int waitTicks,lastTick;
    private volatile List<UnlockCatalog.Result> snapshot=Collections.emptyList();
    private volatile Set<String> favourites=Collections.emptySet();
    private volatile boolean accountReady;
    private volatile String accountName="Log in to inspect your unlocks";
    private String lastSignature="";
    private Panel panel;

    void startUp()
    {
        active=true;dirty=true;waitTicks=8;lastTick=Integer.MIN_VALUE;profile="";
        clear();publish(new UnlockCatalog.Facts());
    }
    void shutDown()
    {
        active=false;clear();snapshot=Collections.emptyList();
        SwingUtilities.invokeLater(()->panel=null);
    }
    private void clear()
    {
        profile="";saved=new Saved();accountReady=false;favourites=Collections.emptySet();
        accountName="Log in to inspect your unlocks";lastSignature="";
    }
    void onGameStateChanged(GameStateChanged event)
    {
        if(!active)return;
        clear();waitTicks=8;dirty=true;lastTick=Integer.MIN_VALUE;publish(new UnlockCatalog.Facts());
    }
    void changed() {dirty=true;}
    void onConfigChanged()
    {
        if(!active)return;
        SwingUtilities.invokeLater(()->{if(panel!=null)panel.rebuild();});
    }
    private boolean bind()
    {
        String next=configs.getRSProfileKey();
        if(next==null||next.isEmpty())return false;
        if(!profile.equals(next)) {
            clear();publish(new UnlockCatalog.Facts());lastTick=Integer.MIN_VALUE;profile=next;
            try {
                String data=configs.getConfiguration("one-man-sync",profile,"unlocksHubProgress");
                if(data!=null) {
                    Saved loaded=gson.fromJson(data,Saved.class);
                    if(loaded!=null&&loaded.manual!=null&&loaded.favourites!=null&&loaded.observed!=null)saved=loaded;
                }
            } catch(RuntimeException ignored) {saved=new Saved();}
            // Ignore stale/unrecognised IDs and malformed timestamps rather than breaking the Hub.
            saved.manual.removeIf(id->!catalog.byId.containsKey(id)||!catalog.byId.get(id).manualAllowed());
            saved.favourites.retainAll(catalog.byId.keySet());
            saved.observed.entrySet().removeIf(e->!catalog.byId.containsKey(e.getKey())||!"ITEM".equals(catalog.byId.get(e.getKey()).mode)||e.getValue()==null||e.getValue()<=0);
            observe(client.getItemContainer(InventoryID.INV));
            observe(client.getItemContainer(InventoryID.WORN));
            dirty=true;
        }
        accountReady=true;
        accountName=client.getLocalPlayer()==null?"Current account":client.getLocalPlayer().getName();
        return true;
    }
    void onGameTick()
    {
        if(!active||client.getGameState()!=GameState.LOGGED_IN||client.getLocalPlayer()==null)return;
        if(waitTicks>0){waitTicks--;return;}
        if(!bind())return;
        int tick=client.getTickCount();
        long elapsed=(long)tick-lastTick;
        if(elapsed>=0&&elapsed<10)return;
        if(!dirty&&elapsed>=0&&elapsed<100)return;
        dirty=false;lastTick=tick;
        UnlockCatalog.Facts facts=new UnlockCatalog.Facts();facts.live=true;
        for(Quest quest:Quest.values())try {facts.quests.put(quest,quest.getState(client));}catch(RuntimeException ignored) { }
        for(Skill skill:Skill.values())if(skill!=Skill.OVERALL)facts.skills.put(skill,client.getRealSkillLevel(skill));
        Set<Integer> bits=new HashSet<>();
        for(UnlockCatalog.Entry entry:catalog.entries)if(entry.varbit>=0)bits.add(entry.varbit);
        for(int bit:bits)try {facts.bits.put(bit,client.getVarbitValue(bit));}catch(RuntimeException ignored) { }
        publish(facts);
    }
    void onItemContainerChanged(ItemContainerChanged event)
    {
        if(!active||!accountReady||client.getGameState()!=GameState.LOGGED_IN||!profile.equals(configs.getRSProfileKey()))return;
        int id=event.getContainerId();
        if(id==InventoryID.INV||id==InventoryID.WORN||id==InventoryID.BANK)observe(event.getItemContainer());
    }
    private void observe(ItemContainer container)
    {
        if(container==null)return;
        boolean changed=false;
        for(Item item:container.getItems()) {
            if(item==null||item.getId()<0||item.getQuantity()<=0)continue;
            ItemComposition def=items.getItemComposition(item.getId());
            if(def==null)continue;
            if(def.getNote()!=-1)def=items.getItemComposition(def.getLinkedNoteId());
            if(def==null||def.getPlaceholderTemplateId()!=-1||def.getName()==null)continue;
            for(UnlockCatalog.Entry entry:catalog.matchingItems(def.getName()))if(!saved.observed.containsKey(entry.id)) {
                saved.observed.put(entry.id,System.currentTimeMillis());changed=true;
            }
        }
        if(changed){save();dirty=true;}
    }
    private void save()
    {
        if(!profile.isEmpty())configs.setConfiguration("one-man-sync",profile,"unlocksHubProgress",gson.toJson(saved));
    }
    private void publish(UnlockCatalog.Facts facts)
    {
        List<UnlockCatalog.Result> rows=new ArrayList<>();
        StringBuilder signature=new StringBuilder(profile).append(accountName).append(saved.favourites);
        for(UnlockCatalog.Entry entry:catalog.entries) {
            UnlockCatalog.Result row=catalog.evaluate(entry,facts,saved.manual,saved.observed);rows.add(row);
            signature.append(row.state).append(row.detail);
        }
        String key=signature.toString();
        if(key.equals(lastSignature))return;
        lastSignature=key;snapshot=Collections.unmodifiableList(rows);
        favourites=Collections.unmodifiableSet(new HashSet<>(saved.favourites));
        SwingUtilities.invokeLater(()->{if(active&&panel!=null)panel.rebuild();});
    }
    private void change(String account,String id,boolean favourite)
    {
        clientThread.invokeLater(()->{
            if(!active||!accountReady||!profile.equals(account)||!profile.equals(configs.getRSProfileKey()))return;
            UnlockCatalog.Entry entry=catalog.byId.get(id);if(entry==null)return;
            Set<String> set=favourite?saved.favourites:saved.manual;
            if(!favourite&&!entry.manualAllowed())return;
            if(!set.remove(id))set.add(id);
            save();dirty=true;lastTick=Integer.MIN_VALUE;
        });
    }
    List<UnlockCatalog.Result> results() {return snapshot;}
    JPanel companionPanel(){if(panel==null)panel=new Panel();return panel;}

    private final class Panel extends JPanel
    {
        private final JTextField search=new JTextField();
        private final JComboBox<String> filter=new JComboBox<>(new String[]{"All states","Not game-confirmed","Game confirmed","Check / claim","Gate pending","Item observed","Confirmed by you","Unknown","In progress"});
        private final JCheckBox journal=new JCheckBox("Include quest journal");
        private final JCheckBox favouritesOnly=new JCheckBox("Favourites only");
        private final Map<String,Boolean> expanded=new HashMap<>();
        private final Map<String,Integer> limits=new HashMap<>();
        private final OneManCards list=new OneManCards();
        private final JTextArea summary=OneManCards.text("");
        Panel()
        {
            super(new BorderLayout(0,6));
            OneManCards header=new OneManCards();
            header.card(OneManCards.banner("Unlocks Hub • Test","Account access, quest gates and important rewards"));
            JPanel tools=OneManCards.card();
            OneManCards fields=new OneManCards();search.setName("unlocksSearch");
            search.setToolTipText("Search unlock, category, quest, skill or description");
            fields.card(search);filter.setName("unlocksStatusFilter");fields.card(filter);
            favouritesOnly.setOpaque(false);fields.card(favouritesOnly);journal.setOpaque(false);journal.setSelected(config.unlocksQuestJournal());
            journal.setName("unlocksQuestJournal");fields.card(journal);fields.card(summary);
            tools.add(fields);header.card(tools);add(header,BorderLayout.NORTH);
            add(OneManCards.scroll(list),BorderLayout.CENTER);
            search.getDocument().addDocumentListener(new DocumentListener(){
                public void insertUpdate(DocumentEvent e){rebuild();}public void removeUpdate(DocumentEvent e){rebuild();}public void changedUpdate(DocumentEvent e){rebuild();}
            });
            filter.addActionListener(e->rebuild());favouritesOnly.addActionListener(e->rebuild());
            journal.addActionListener(e->configs.setConfiguration("one-man-sync","unlocksQuestJournal",journal.isSelected()));rebuild();
        }
        private boolean matches(UnlockCatalog.Result row,String query)
        {
            if(!config.unlocksQuestJournal()&&"JOURNAL".equals(row.entry.mode))return false;
            if(favouritesOnly.isSelected()&&!favourites.contains(row.entry.id))return false;
            String state=(String)filter.getSelectedItem();
            if("Not game-confirmed".equals(state)&&row.state==UnlockCatalog.State.UNLOCKED)return false;
            if(!"All states".equals(state)&&!"Not game-confirmed".equals(state)&&!row.state.label.equals(state))return false;
            String text=row.entry.title+" "+row.entry.category+" "+row.detail;
            return text.toLowerCase(Locale.ROOT).contains(query);
        }
        void rebuild()
        {
            journal.setSelected(config.unlocksQuestJournal());
            String query=search.getText().trim().toLowerCase(Locale.ROOT);
            Map<String,List<UnlockCatalog.Result>> groups=new LinkedHashMap<>();
            int confirmed=0,observed=0,manual=0,total=0;
            for(UnlockCatalog.Result row:results()) {
                if(!config.unlocksQuestJournal()&&"JOURNAL".equals(row.entry.mode))continue;
                total++;if(row.state==UnlockCatalog.State.UNLOCKED)confirmed++;
                if(row.state==UnlockCatalog.State.OBSERVED)observed++;
                if(row.state==UnlockCatalog.State.MANUAL)manual++;
                if(matches(row,query))groups.computeIfAbsent(row.entry.category,k->new ArrayList<>()).add(row);
            }
            summary.setText(accountName+"\n"+confirmed+" / "+total+" entries game-confirmed\n"+observed+" items observed • "+manual+" manual");
            list.removeAll();
            for(Map.Entry<String,List<UnlockCatalog.Result>> group:groups.entrySet()) {
                String category=group.getKey();List<UnlockCatalog.Result> rows=group.getValue();
                JPanel card=OneManCards.card();
                boolean open=expanded.getOrDefault(category,!query.isEmpty()||favouritesOnly.isSelected());
                JToggleButton heading=new JToggleButton(category+" ("+rows.size()+")",open);
                heading.setFont(heading.getFont().deriveFont(11f));
                heading.setName("unlockCategory:"+category);
                heading.setToolTipText("Expand or collapse "+category);
                heading.addActionListener(e->{expanded.put(category,heading.isSelected());rebuild();});
                card.add(heading,BorderLayout.NORTH);
                if(open) {
                    OneManCards content=new OneManCards();int count=Math.min(rows.size(),limits.getOrDefault(category,15));
                    for(int i=0;i<count;i++)content.card(row(rows.get(i)));
                    if(count<rows.size()) {
                        JButton more=new JButton("Show 15 more ("+(rows.size()-count)+" left)");
                        more.addActionListener(e->{limits.put(category,count+15);rebuild();});content.card(more);
                    }
                    card.add(content,BorderLayout.CENTER);
                }
                list.card(card);
            }
            if(groups.isEmpty())list.card(OneManCards.text("No matching unlocks. Clear the search or change the filters."));
            list.finish();
        }
        private JPanel row(UnlockCatalog.Result result)
        {
            UnlockCatalog.Entry entry=result.entry;String account=profile;
            JPanel card=OneManCards.card();
            JPanel title=new JPanel(new BorderLayout(3,3));title.setOpaque(false);
            JTextArea name=OneManCards.text(entry.title);name.setForeground(Color.WHITE);title.add(name,BorderLayout.CENTER);
            JButton star=new JButton(favourites.contains(entry.id)?"★":"☆");star.setMargin(new java.awt.Insets(0,3,0,3));
            star.setToolTipText("Toggle favourite for this account");star.setEnabled(accountReady);
            star.addActionListener(e->change(account,entry.id,true));title.add(star,BorderLayout.EAST);
            card.add(title,BorderLayout.NORTH);
            OneManCards body=new OneManCards();
            JTextArea state=OneManCards.text(result.state.label);
            state.setForeground(result.state==UnlockCatalog.State.UNLOCKED?new Color(100,210,140):result.state==UnlockCatalog.State.PENDING?new Color(225,145,100):OneManCards.ACCENT);
            body.card(state);
            JToggleButton details=new JToggleButton("Details / requirements");
            details.setName("unlockDetails:"+entry.id);
            OneManCards expandedDetails=new OneManCards();
            expandedDetails.card(OneManCards.text(result.detail));
            JButton guide=new JButton("Open Wiki guide");guide.addActionListener(e->LinkBrowser.browse(entry.wiki));expandedDetails.card(guide);
            if(entry.manualAllowed()&&result.state!=UnlockCatalog.State.OBSERVED) {
                JButton confirm=new JButton(result.state==UnlockCatalog.State.MANUAL?"Undo confirmation":"Confirm manually");
                confirm.setToolTipText("Your own note; never shown as game-confirmed");confirm.setEnabled(accountReady);
                confirm.addActionListener(e->change(account,entry.id,false));expandedDetails.card(confirm);
            }
            expandedDetails.setVisible(false);details.addActionListener(e->{expandedDetails.setVisible(details.isSelected());revalidate();repaint();});
            body.card(details);body.card(expandedDetails);card.add(body,BorderLayout.CENTER);return card;
        }
    }
}
