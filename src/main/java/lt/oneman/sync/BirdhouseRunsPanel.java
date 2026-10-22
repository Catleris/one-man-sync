package lt.oneman.sync;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

/** Four stable cards: updating observations never replaces the user's scrollable content. */
final class BirdhouseRunsPanel extends JPanel
{
    private final ItemManager items;
    private final java.util.List<HouseCard> houses=new ArrayList<>();
    private final JTextArea summary=CombatHubStyle.text("Log in to view your runs.");
    private final JTextArea hint=CombatHubStyle.text("Visit Fossil Island with Time Tracking enabled to detect birdhouses.");
    private final JLabel contract=CombatHubStyle.badge("Not checked",OneManCards.MUTED);
    BirdhouseRunsPanel(ItemManager items,Runnable refresh){
        super(new BorderLayout(0,6));this.items=items;setName("birdhouseRunsPanel");setOpaque(false);
        OneManCards content=new OneManCards();content.card(OneManCards.banner("Birdhouse runs","Fossil Island"));
        JPanel overview=CombatHubStyle.card(OneManCards.ACCENT,false);summary.setName("birdhouseSummary");overview.add(summary,BorderLayout.NORTH);overview.add(hint);content.card(overview);
        for(int i=0;i<4;i++){HouseCard card=new HouseCard(i);houses.add(card);content.card(card.card);}
        JPanel contractCard=CombatHubStyle.card(CombatHubStyle.GREEN,false);JLabel heading=new JLabel("Farming contract");heading.setForeground(Color.WHITE);heading.setFont(OneManCards.HEADING);
        contractCard.add(heading,BorderLayout.NORTH);contract.setName("farmingContractStatus");contractCard.add(contract);content.card(contractCard);content.finish();
        JScrollPane scroll=OneManCards.scroll(content);scroll.setName("birdhouseRunsScroll");scroll.getViewport().setBackground(new Color(24,29,26));add(scroll);
        JButton reload=new JButton("Refresh observations");reload.addActionListener(e->refresh.run());add(reload,BorderLayout.SOUTH);OneManCards.readable(this);
        loggedOut();
    }
    void loggedOut(){
        summary.setText("Log in to view your runs.");hint.setVisible(false);contract.setText("Not checked");contract.setForeground(OneManCards.MUTED);
        for(int i=0;i<4;i++)houses.get(i).update(BirdhouseRun.unknown(i),false);
    }
    void update(List<BirdhouseRun> runs,String contractState){
        long ready=runs.stream().filter(r->r.status==BirdhouseRun.Status.READY).count(),unknown=runs.stream().filter(r->r.status==BirdhouseRun.Status.UNKNOWN).count();
        summary.setText(unknown==4?"No observations yet":ready+" ready"+(unknown>0?" · "+unknown+" not checked":" · 4 locations"));hint.setVisible(unknown>0);
        contract.setText(contractState);contract.setForeground("Complete".equals(contractState)?CombatHubStyle.GREEN:OneManCards.MUTED);
        for(int i=0;i<4;i++)houses.get(i).update(runs.get(i),true);revalidate();
    }
    private final class HouseCard {
        final JPanel card=CombatHubStyle.card(OneManCards.ACCENT,false);
        final JLabel icon=new JLabel();final JLabel state=CombatHubStyle.badge("Not checked",OneManCards.MUTED);
        final JTextArea detail=CombatHubStyle.text("");final JProgressBar bar=CombatHubStyle.bar(100,0,OneManCards.ACCENT);
        int item=-1;
        HouseCard(int index){
            card.setName("birdhouseCard:"+index);JPanel header=new JPanel(new BorderLayout(6,0));header.setOpaque(false);
            icon.setPreferredSize(new Dimension(36,36));icon.setMinimumSize(new Dimension(36,36));icon.setHorizontalAlignment(SwingConstants.CENTER);icon.setIcon(CombatHubStyle.fallback());
            JTextArea location=CombatHubStyle.text(BirdhouseRun.LOCATIONS[index]);location.setForeground(Color.WHITE);location.setFont(OneManCards.HEADING);
            header.add(icon,BorderLayout.WEST);header.add(location);card.add(header,BorderLayout.NORTH);
            OneManCards body=new OneManCards();state.setName("birdhouseStatus:"+index);detail.setName("birdhouseDetail:"+index);body.card(state);body.card(detail);body.card(bar);card.add(body);
        }
        void update(BirdhouseRun run,boolean loggedIn){
            Color color=run.status==BirdhouseRun.Status.READY?CombatHubStyle.GREEN:run.status==BirdhouseRun.Status.UNKNOWN?OneManCards.MUTED:OneManCards.ACCENT;
            state.setText(loggedIn?run.label():"Log in");state.setForeground(color);
            detail.setText(run.status==BirdhouseRun.Status.GROWING?run.house+" · "+run.time():run.house);
            detail.setVisible(!detail.getText().isEmpty());bar.setValue(run.progress());bar.setForeground(color);bar.setVisible(run.status==BirdhouseRun.Status.GROWING||run.status==BirdhouseRun.Status.READY);
            if(item!=run.item){item=run.item;AsyncBufferedImage image=items==null?null:items.getImage(item);if(image!=null)image.addTo(icon);}
        }
    }
}
