package lt.oneman.sync;

import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.util.LinkBrowser;
import static lt.oneman.sync.CombatAchievementCatalog.*;

/** One-column, width-tracking browser for the real narrow RuneLite sidebar. EDT only. */
final class CombatAchievementsPanel extends JPanel
{
    interface TrackAction {void set(int id,boolean selected,String profile);}
    private static final Color GREEN=new Color(134,207,151), BACK=new Color(35,38,37);
    private final TrackAction track;
    private CombatAchievementsHub.Snapshot state=CombatAchievementsHub.Snapshot.empty();
    private final OneManCards list=new OneManCards();
    private final JScrollPane scroll=OneManCards.scroll(list);
    private final JTextField search=new JTextField();
    private final JComboBox<String> status=new JComboBox<>(new String[]{"All statuses","Incomplete","Complete"});
    private final JComboBox<String> type=new JComboBox<>(new String[]{"All types","Stamina","Perfection","Kill Count","Mechanical","Restriction","Speed","Other"});
    private final JComboBox<String> sort=new JComboBox<>(new String[]{"Tier","Name"});
    private final EnumSet<Tier> tiers=EnumSet.allOf(Tier.class);
    private final Map<String,Boolean> expanded=new HashMap<>();
    private final JTextArea progressText=OneManCards.text("Log in to load your combat tasks.");
    private final JTextArea nextText=OneManCards.text("");
    private final JProgressBar progress=new JProgressBar();
    private final OneManCards filterFields=new OneManCards();
    private final ButtonGroup views=new ButtonGroup();
    private String view="Bosses";
    private int limit=25;
    CombatAchievementsPanel(TrackAction track){
        super(new BorderLayout(0,6));this.track=track;setName("combatAchievementsPanel");
        OneManCards header=new OneManCards();
        header.card(OneManCards.banner("Combat Achievements","Tasks, bosses and progress"));
        JPanel summary=OneManCards.card();OneManCards summaryRows=new OneManCards();
        progressText.setName("combatProgress");summaryRows.card(progressText);summaryRows.card(progress);summaryRows.card(nextText);summary.add(summaryRows);header.card(summary);
        JPanel filterCard=OneManCards.card();
        JToggleButton filters=new JToggleButton("Filters  ▾");filters.setName("combatFiltersToggle");filterCard.add(filters,BorderLayout.NORTH);
        JPanel tierButtons=new JPanel(new GridLayout(0,2,4,4));tierButtons.setOpaque(false);
        for(Tier tier:Tier.values()){
            JToggleButton button=new JToggleButton(tier.label,true);button.setMargin(new Insets(4,2,4,2));button.setForeground(tier.color);
            button.setName("combatTier:"+tier.name());button.addActionListener(e->{if(button.isSelected())tiers.add(tier);else tiers.remove(tier);resetResults();});tierButtons.add(button);
        }
        filterFields.card(tierButtons);status.setName("combatStatus");type.setName("combatType");sort.setName("combatSort");
        filterFields.card(field("Status",status));filterFields.card(field("Type",type));filterFields.card(field("Sort",sort));
        filterFields.setVisible(false);filterCard.add(filterFields,BorderLayout.CENTER);
        filters.addActionListener(e->{filterFields.setVisible(filters.isSelected());filters.setText(filters.isSelected()?"Filters  ▴":"Filters  ▾");revalidate();});
        for(JComboBox<String> box:Arrays.asList(status,type,sort))box.addActionListener(e->resetResults());header.card(filterCard);
        JPanel tabs=new JPanel(new GridLayout(1,3,3,0));tabs.setOpaque(false);
        for(String name:new String[]{"Tasks","Tracked","Bosses"}){
            JToggleButton button=new JToggleButton(name,"Bosses".equals(name));button.setMargin(new Insets(5,2,5,2));button.setName("combatView:"+name);
            button.setForeground(OneManCards.ACCENT);views.add(button);tabs.add(button);button.addActionListener(e->{view=name;resetResults();});
        }
        header.card(tabs);search.setName("combatSearch");search.setToolTipText("Search bosses, task names or descriptions");
        header.card(field("Search",search));
        search.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){resetResults();}public void removeUpdate(DocumentEvent e){resetResults();}public void changedUpdate(DocumentEvent e){resetResults();}
        });
        add(header,BorderLayout.NORTH);add(scroll,BorderLayout.CENTER);OneManCards.readable(this);rebuild();
    }
    private static JPanel field(String title,JComponent control){
        JPanel row=new JPanel(new BorderLayout(5,0));row.setOpaque(false);JLabel label=new JLabel(title);label.setForeground(OneManCards.MUTED);
        row.add(label,BorderLayout.WEST);control.setMinimumSize(new Dimension(0,26));row.add(control);return row;
    }
    void update(CombatAchievementsHub.Snapshot next){
        if(!state.profile.equals(next.profile)){expanded.clear();limit=25;}
        state=next;
        if(!state.ready){progressText.setText("Log in to load your combat tasks.");nextText.setText("");progress.setValue(0);}
        else{
            int done=state.completed.size();
            progressText.setText(done+" / "+state.tasks.size()+" complete · "+state.points+" pts");
            progress.setMaximum(Math.max(1,state.tasks.size()));progress.setValue(done);progress.setForeground(GREEN);
            nextText.setText(nextTier(state));
        }
        rebuild();
    }
    static String nextTier(CombatAchievementsHub.Snapshot state){
        for(int i=0;i<state.thresholds.length;i++){
            if(state.thresholds[i]<=0)return "";
            if(state.points<state.thresholds[i])return "Next: "+Tier.values()[i].label+" · "+(state.thresholds[i]-state.points)+" pts to go";
        }
        return "Grandmaster reached";
    }
    private void resetResults(){limit=25;rebuild();scroll.getVerticalScrollBar().setValue(0);}
    private boolean matches(Task task){
        String query=search.getText().trim().toLowerCase(Locale.ROOT);
        if(!tiers.contains(task.tier)||(!query.isEmpty()&&!(task.name+" "+task.boss+" "+task.description).toLowerCase(Locale.ROOT).contains(query)))return false;
        boolean done=state.completed.contains(task.id);
        if("Incomplete".equals(status.getSelectedItem())&&(!state.known(task)||done))return false;
        if("Complete".equals(status.getSelectedItem())&&!done)return false;
        return "All types".equals(type.getSelectedItem())||task.type.equals(type.getSelectedItem());
    }
    private void rebuild(){
        int position=scroll.getVerticalScrollBar().getValue();list.removeAll();
        if(!state.ready){list.card(OneManCards.text("Your tasks will appear here after login."));list.finish();return;}
        List<Task> visible=state.tasks.stream().filter(this::matches).filter(t->!"Tracked".equals(view)||state.tracked.contains(t.id)).collect(Collectors.toList());
        if("Bosses".equals(view)){
            Map<String,List<Task>> groups=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for(Task task:visible)groups.computeIfAbsent(task.boss,k->new ArrayList<>()).add(task);
            int count=0;
            for(Map.Entry<String,List<Task>> group:groups.entrySet()){
                if(count++>=limit)break;list.card(boss(group.getKey(),group.getValue()));
            }
            more(groups.size());
        }else{
            order(visible);for(int i=0;i<Math.min(limit,visible.size());i++)list.card(task(visible.get(i)));more(visible.size());
        }
        if(visible.isEmpty())list.card(OneManCards.text("Tracked".equals(view)?"No matching tracked tasks. Use Track on a task to add it here.":"No matching tasks. Change the filters or search."));
        list.finish();scroll.getVerticalScrollBar().setValue(position);
    }
    private void order(List<Task> tasks){
        tasks.sort("Name".equals(sort.getSelectedItem())?Comparator.comparing(t->t.name,String.CASE_INSENSITIVE_ORDER):
            Comparator.comparingInt((Task t)->t.tier.ordinal()).thenComparing(t->t.name));
    }
    private void more(int total){if(total>limit){JButton button=new JButton("Show 25 more");button.setName("combatShowMore");button.addActionListener(e->{limit+=25;rebuild();});list.card(button);}}
    private JPanel boss(String name,List<Task> filtered){
        JPanel card=OneManCards.card();boolean open=expanded.getOrDefault(name,false);
        JPanel heading=new JPanel(new BorderLayout(5,2));heading.setOpaque(false);
        JTextArea title=OneManCards.text(name);title.setFont(OneManCards.HEADING);title.setForeground(Color.WHITE);
        JToggleButton expand=new JToggleButton(open?"▴":"▾",open);expand.setMargin(new Insets(2,6,2,6));expand.setName("combatBoss:"+name);
        expand.setToolTipText("Show or hide "+name+" tasks");expand.addActionListener(e->{expanded.put(name,expand.isSelected());rebuild();});
        heading.add(title);heading.add(expand,BorderLayout.EAST);card.add(heading,BorderLayout.NORTH);
        List<Task> all=state.tasks.stream().filter(t->name.equals(t.boss)).collect(Collectors.toList());
        long done=all.stream().filter(t->state.completed.contains(t.id)).count();
        boolean known=all.stream().allMatch(state::known);
        OneManCards body=new OneManCards();JTextArea count=OneManCards.text(known?done+" / "+all.size()+(done==all.size()?" · Complete":""):"Progress unavailable");
        count.setForeground(done==all.size()?GREEN:OneManCards.MUTED);body.card(count);
        JProgressBar bar=new JProgressBar(0,all.size());bar.setValue((int)done);bar.setForeground(done==all.size()?GREEN:OneManCards.ACCENT);bar.setPreferredSize(new Dimension(1,6));body.card(bar);
        if(open){order(filtered);for(Task entry:filtered)body.card(task(entry));}
        card.add(body);return card;
    }
    private JPanel task(Task task){
        JPanel card=OneManCards.card();card.setName("combatTask:"+task.id);
        boolean done=state.completed.contains(task.id);JTextArea title=OneManCards.text(task.name);title.setFont(OneManCards.HEADING);title.setForeground(done?GREEN:Color.WHITE);card.add(title,BorderLayout.NORTH);
        OneManCards body=new OneManCards();JTextArea tier=OneManCards.text(task.tier.label+" · "+task.type+" · "+(state.known(task)?done?"Complete":"Incomplete":"Unknown"));tier.setForeground(task.tier.color);body.card(tier);
        if(!"Bosses".equals(view))body.card(OneManCards.text(task.boss));
        body.card(OneManCards.text(task.description));
        JPanel actions=new JPanel(new GridLayout(1,2,4,0));actions.setOpaque(false);
        JCheckBox tracked=new JCheckBox("Track",state.tracked.contains(task.id));tracked.setOpaque(false);tracked.setName("combatTrack:"+task.id);
        String expected=state.profile;tracked.addActionListener(e->track.set(task.id,tracked.isSelected(),expected));actions.add(tracked);
        JButton wiki=new JButton("Wiki");wiki.setMargin(new Insets(3,3,3,3));wiki.addActionListener(e->LinkBrowser.browse("https://oldschool.runescape.wiki/w/"+URLEncoder.encode(task.name.replace(' ','_'),StandardCharsets.UTF_8)));actions.add(wiki);body.card(actions);
        card.add(body);return card;
    }
}
