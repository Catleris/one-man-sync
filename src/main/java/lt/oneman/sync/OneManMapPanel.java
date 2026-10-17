package lt.oneman.sync;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Shared category cards: an icon, location count and explicit location selector. */
final class OneManMapPanel extends JPanel
{
    private final OneManMapLocations maps;
    private final OneManMapCatalog.Group group;
    private final JComboBox<String> filter;
    private final JCheckBox show;
    private final JTextField search=new JTextField();
    private final OneManCards cards=new OneManCards();
    private final JLabel count=new JLabel();
    private final Map<String,String> selected=new HashMap<>();
    private final Map<String,JComboBox<OneManMapCatalog.Site>> selectors=new HashMap<>();
    private final Map<String,JTextArea> descriptions=new HashMap<>();
    private Map<String,String> statuses=Collections.emptyMap();

    OneManMapPanel(OneManMapLocations maps,OneManMapCatalog.Group group)
    {
        super(new BorderLayout(0,6));this.maps=maps;this.group=group;
        boolean farming=group==OneManMapCatalog.Group.FARMING;
        filter=new JComboBox<>(maps.categories(group).toArray(new String[0]));
        filter.setName(farming?"farmingTypeFilter":"stashTierFilter");filter.setSelectedItem(maps.category(group));
        filter.setMinimumSize(new Dimension(0,24));
        show=new JCheckBox(farming?"Show farming map pins":"Show STASH markers",maps.visible(group));
        search.setName(farming?"farmingLocationSearch":"stashLocationSearch");search.setToolTipText("Filter cards by type or location name");search.putClientProperty("JTextField.placeholderText","Patch or location");
        JPanel controls=new JPanel(new BorderLayout(0,5));
        JPanel fields=new JPanel(new GridLayout(0,1,0,3));
        JPanel searchRow=new JPanel(new BorderLayout(6,0));JLabel label=new JLabel("Search");label.setForeground(OneManCards.ACCENT);searchRow.add(label,BorderLayout.WEST);searchRow.add(search,BorderLayout.CENTER);
        fields.add(filter);fields.add(searchRow);fields.add(show);fields.add(count);
        controls.add(OneManCards.banner(farming?"FARMING PATCH LOCATIONS":"STASH LOCATIONS", "Choose a location to focus the world map."),BorderLayout.NORTH);
        controls.add(fields,BorderLayout.CENTER);add(controls,BorderLayout.NORTH);add(OneManCards.scroll(cards),BorderLayout.CENTER);
        filter.addActionListener(event->{reload();maps.configure(group,(String)filter.getSelectedItem(),show.isSelected());});
        show.addActionListener(event->maps.configure(group,(String)filter.getSelectedItem(),show.isSelected()));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {reload();}
            public void removeUpdate(DocumentEvent event) {reload();}
            public void changedUpdate(DocumentEvent event) {reload();}
        });
        reload();
    }
    void chooseCategory(String category)
    {
        show.setSelected(true);search.setText("");filter.setSelectedItem(category);
    }
    void setStatuses(Map<String,String> statuses)
    {
        this.statuses=Collections.unmodifiableMap(new HashMap<>(statuses));
        selectors.forEach((category,selector)->describe(category,(OneManMapCatalog.Site)selector.getSelectedItem()));
    }
    private void reload()
    {
        Map<String,List<OneManMapCatalog.Site>> grouped=new LinkedHashMap<>();
        String query=search.getText().trim().toLowerCase(Locale.ROOT);
        for(String category:maps.categories(group)) if(!category.equals("All")) grouped.put(category,new ArrayList<>());
        int locations=0;
        for(OneManMapCatalog.Site site:maps.sites(group,(String)filter.getSelectedItem())) {
            if(!query.isEmpty() && !(site.category+" "+site.name).toLowerCase(Locale.ROOT).contains(query)) continue;
            grouped.get(site.category).add(site);locations++;
        }
        cards.removeAll();selectors.clear();descriptions.clear();int types=0;
        for(Map.Entry<String,List<OneManMapCatalog.Site>> row:grouped.entrySet()) if(!row.getValue().isEmpty()) {addCard(row.getKey(),row.getValue());types++;}
        if(types==0) cards.card(OneManCards.text("No matching locations. Try another type or place name."));
        count.setText(locations+" locations • "+types+" "+(types==1?"type":"types"));count.setForeground(OneManCards.MUTED);cards.finish();
    }
    private void addCard(String category,List<OneManMapCatalog.Site> sites)
    {
        JPanel card=OneManCards.card();card.setName((group==OneManMapCatalog.Group.FARMING?"farmingCard:":"stashCard:")+category);
        JPanel header=new JPanel(new BorderLayout(6,0));header.setOpaque(false);
        JLabel icon=new JLabel();icon.setPreferredSize(new Dimension(32,34));maps.icon(icon,category);header.add(icon,BorderLayout.WEST);
        JPanel title=new JPanel(new GridLayout(0,1));title.setOpaque(false);
        JLabel name=new JLabel(category);name.setFont(name.getFont().deriveFont(Font.BOLD));name.setToolTipText(category);title.add(name);
        JLabel size=new JLabel(sites.size()+" "+(sites.size()==1?"location":"locations"));size.setForeground(OneManCards.MUTED);size.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,11));title.add(size);header.add(title,BorderLayout.CENTER);
        JButton map=new JButton("Map");map.setMargin(new Insets(2,5,2,5));map.setToolTipText("Centre the world map on the selected location");header.add(map,BorderLayout.EAST);card.add(header,BorderLayout.NORTH);
        JComboBox<OneManMapCatalog.Site> selector=new JComboBox<>(sites.toArray(new OneManMapCatalog.Site[0]));
        selector.setName((group==OneManMapCatalog.Group.FARMING?"farmingLocation:":"stashLocation:")+category);
        selector.setPreferredSize(new Dimension(1,28));selector.setMinimumSize(new Dimension(0,28));
        for(OneManMapCatalog.Site site:sites) if(site.id.equals(selected.get(category))) selector.setSelectedItem(site);
        JTextArea status=OneManCards.text("");
        selectors.put(category,selector);descriptions.put(category,status);
        JPanel body=new JPanel(new BorderLayout(0,5));body.setOpaque(false);body.add(selector,BorderLayout.NORTH);body.add(status,BorderLayout.CENTER);card.add(body,BorderLayout.CENTER);
        Runnable focus=()-> {
            OneManMapCatalog.Site site=(OneManMapCatalog.Site)selector.getSelectedItem();if(site==null) return;
            selected.put(category,site.id);describe(category,site);maps.focus(group,site);
        };
        selector.addActionListener(event->focus.run());map.addActionListener(event->focus.run());
        describe(category,(OneManMapCatalog.Site)selector.getSelectedItem());cards.card(card);
    }
    private void describe(String category,OneManMapCatalog.Site site)
    {
        if(site==null) return;
        selectors.get(category).setToolTipText(site.name);
        String status=statuses.get(site.id);
        String hint=site.points.get(0).getY()>6400?"Underground map area":site.points.get(0).getPlane()>0?"Floor "+site.points.get(0).getPlane():"Surface location";
        descriptions.get(category).setText(status==null?(group==OneManMapCatalog.Group.STASH?site.name+"\n":"")+hint:status);
    }
}
