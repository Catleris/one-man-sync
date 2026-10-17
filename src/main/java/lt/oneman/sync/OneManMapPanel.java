package lt.oneman.sync;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Narrow-sidebar browser shared by STASH tiers and farming types. */
final class OneManMapPanel extends JPanel
{
    private final OneManMapLocations maps;
    private final OneManMapCatalog.Group group;
    private final DefaultListModel<OneManMapCatalog.Site> model=new DefaultListModel<>();
    private final JList<OneManMapCatalog.Site> list=new JList<OneManMapCatalog.Site>(model) {
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
    };
    private final JComboBox<String> filter;
    private final JCheckBox show;
    private final JLabel count=new JLabel();
    private final JTextArea details=new JTextArea(5,0);
    private Map<String,String> statuses=Collections.emptyMap();

    OneManMapPanel(OneManMapLocations maps, OneManMapCatalog.Group group)
    {
        super(new BorderLayout(0,6));this.maps=maps;this.group=group;
        setBorder(BorderFactory.createEmptyBorder(6,4,6,4));
        filter=new JComboBox<>(maps.categories(group).toArray(new String[0]));
        filter.setName(group==OneManMapCatalog.Group.STASH?"stashTierFilter":"farmingTypeFilter");
        filter.setSelectedItem(maps.category(group));
        show=new JCheckBox(group==OneManMapCatalog.Group.STASH?"Show STASH markers":"Show farming map pins",maps.visible(group));
        JPanel controls=new JPanel(new GridLayout(0,1,0,3));
        controls.add(new JLabel(group==OneManMapCatalog.Group.STASH?"STASH • clue tier":"Farming • patch type"));
        controls.add(filter);controls.add(show);controls.add(count);add(controls,BorderLayout.NORTH);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setName(group==OneManMapCatalog.Group.STASH?"stashLocations":"farmingLocations");
        list.setCellRenderer((view,site,index,selected,focus)-> {
            JLabel row=new JLabel("<html><body style='width:154px'><b>"+html(site.name)+"</b><br>"+html(site.category)+"</body></html>");
            row.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));row.setOpaque(true);
            row.setBackground(selected?view.getSelectionBackground():view.getBackground());
            row.setForeground(selected?view.getSelectionForeground():site.color);
            row.setToolTipText(site.name+" • "+site.category);return row;
        });
        JScrollPane scroll=new JScrollPane(list,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setMinimumSize(new Dimension(0,0));add(scroll,BorderLayout.CENTER);
        details.setEditable(false);details.setLineWrap(true);details.setWrapStyleWord(true);details.setOpaque(false);
        JButton focus=new JButton("Show selected on map");focus.setEnabled(false);
        list.addListSelectionListener(event->{
            if(event.getValueIsAdjusting()) return;
            OneManMapCatalog.Site site=list.getSelectedValue();focus.setEnabled(site!=null);describe();
            if(site!=null) maps.focus(group,site);
        });
        focus.addActionListener(event->{OneManMapCatalog.Site site=list.getSelectedValue();if(site!=null) maps.focus(group,site);});
        JPanel footer=new JPanel(new BorderLayout(0,4));JScrollPane notes=new JScrollPane(details,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);notes.setPreferredSize(new Dimension(0,100));notes.setMinimumSize(new Dimension(0,0));footer.add(notes,BorderLayout.CENTER);footer.add(focus,BorderLayout.SOUTH);add(footer,BorderLayout.SOUTH);
        filter.addActionListener(event->{ reload(); maps.configure(group,(String)filter.getSelectedItem(),show.isSelected()); });
        show.addActionListener(event->maps.configure(group,(String)filter.getSelectedItem(),show.isSelected()));
        reload();
    }
    void setStatuses(Map<String,String> statuses)
    {
        this.statuses=Collections.unmodifiableMap(new HashMap<>(statuses));describe();
    }
    private void reload()
    {
        List<OneManMapCatalog.Site> sites=maps.sites(group,(String)filter.getSelectedItem());
        model.clear();for(OneManMapCatalog.Site site:sites) model.addElement(site);
        count.setText(sites.size()+(group==OneManMapCatalog.Group.STASH?" STASH units":" patch locations"));describe();
    }
    private void describe()
    {
        OneManMapCatalog.Site site=list.getSelectedValue();
        if(site==null) {
            details.setText("Open the world map, then select a location to centre it. "+(group==OneManMapCatalog.Group.STASH?"Nearby STASH markers also appear on the minimap.":"Nearby plots are grouped by site. Locations do not confirm account access."));return;
        }
        String status=statuses.get(site.id);
        details.setText(site.category+" • "+site.name+"\n"+(status==null?"":status+"\n")+
            "Open world map to centre. "+(site.points.get(0).getY()>6400?"Underground: switch to the appropriate map area.":"Floor "+site.points.get(0).getPlane()+". Locations do not confirm access."));
        details.setCaretPosition(0);
    }
    private static String html(String text) { return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
}
