package lt.oneman.sync;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Displays existing passive text as wrapped cards, preserving the original source and actions. */
final class OneManTextCards extends JScrollPane
{
    private final OneManCards cards=new OneManCards();
    private final JTextArea source;
    private int generation;
    private String rendered="";
    OneManTextCards(JTextArea source){
        super();this.source=source;setViewportView(cards);setBorder(BorderFactory.createEmptyBorder());setMinimumSize(new Dimension(0,0));
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);getVerticalScrollBar().setUnitIncrement(18);getViewport().setBackground(OneManTheme.CANVAS);
        source.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){queue();}public void removeUpdate(DocumentEvent e){queue();}public void changedUpdate(DocumentEvent e){queue();}
        });render();
    }
    private void queue(){int request=++generation;SwingUtilities.invokeLater(()->{if(request==generation)render();});}
    private void render(){
        String value=source.getText();if(value.equals(rendered)&&cards.getComponentCount()>0)return;rendered=value;
        Point position=getViewport().getViewPosition();cards.removeAll();
        for(String block:value.split("\\n\\s*\\n")){
            if(block.trim().isEmpty())continue;JPanel card=OneManCards.card();String text=block.trim();int line=text.indexOf('\n');
            if(line>0&&line<85){JTextArea title=OneManCards.text(text.substring(0,line));title.setFont(OneManCards.HEADING);title.setForeground(OneManCards.ACCENT);card.add(title,BorderLayout.NORTH);text=text.substring(line+1);}
            card.add(OneManCards.text(text));cards.card(card);
        }
        cards.finish();int request=++generation;
        SwingUtilities.invokeLater(()->{if(request==generation){validate();int max=Math.max(0,getViewport().getViewSize().height-getViewport().getExtentSize().height);getViewport().setViewPosition(new Point(0,Math.min(position.y,max)));}});
    }
}
