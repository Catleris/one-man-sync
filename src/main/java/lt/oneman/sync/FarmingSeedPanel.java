package lt.oneman.sync;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class FarmingSeedPanel extends JPanel
{
    private final FarmingSeedCatalog catalog=new FarmingSeedCatalog();
    private final OneManMapLocations maps;
    private final Consumer<String> browse;
    private final JTextField query=new JTextField();
    private final OneManCards results=new OneManCards();
    FarmingSeedPanel(OneManMapLocations maps,Consumer<String> browse)
    {
        super(new BorderLayout(0,6));this.maps=maps;this.browse=browse;
        JPanel header=new JPanel(new BorderLayout(0,6));
        header.add(OneManCards.banner("Seed finder", "Find where a seed, sapling, spore or tuber belongs."),BorderLayout.NORTH);
        query.setName("farmingSeedSearch");query.putClientProperty("JTextField.placeholderText","e.g. ranarr seed");query.setToolTipText("Try ranarr seed, acorn, jute seed or maple sapling");
        JPanel row=new JPanel(new BorderLayout(6,0));JLabel label=new JLabel("Seed");label.setForeground(OneManCards.ACCENT);row.add(label,BorderLayout.WEST);row.add(query,BorderLayout.CENTER);
        header.add(row,BorderLayout.SOUTH);add(header,BorderLayout.NORTH);add(OneManCards.scroll(results),BorderLayout.CENTER);
        query.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {search();}
            public void removeUpdate(DocumentEvent event) {search();}
            public void changedUpdate(DocumentEvent event) {search();}
        });search();OneManTheme.apply(this);
    }
    private void search()
    {
        results.removeAll();List<FarmingSeedCatalog.Seed> matches=catalog.search(query.getText());
        if(matches.isEmpty()) results.card(OneManCards.text(query.getText().trim().isEmpty()?"Type a name, for example ranarr seed or acorn.":"No known planting match. Check the exact name; this may be a non-Farming item."));
        for(FarmingSeedCatalog.Seed seed:matches) {
            JPanel card=OneManCards.card();card.setName("seedResult:"+seed.produce.name());
            JPanel title=new JPanel(new BorderLayout(6,0));title.setOpaque(false);
            JLabel icon=new JLabel();icon.setPreferredSize(new Dimension(32,34));maps.cropIcon(icon,seed.produce);title.add(icon,BorderLayout.WEST);
            JTextArea name=OneManCards.text(seed.name);name.setForeground(Color.WHITE);name.setFont(new Font(Font.SANS_SERIF,Font.BOLD,13));title.add(name,BorderLayout.CENTER);card.add(title,BorderLayout.NORTH);
            JTextArea note=OneManCards.text("Patch: "+seed.category+"\n"+seed.note);card.add(note,BorderLayout.CENTER);
            JButton locations=new JButton("Show "+seed.category+" patches");locations.setName("seedBrowse:"+seed.produce.name());locations.addActionListener(event->browse.accept(seed.category));card.add(locations,BorderLayout.SOUTH);results.card(card);
        }
        results.finish();
    }
}
