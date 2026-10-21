package lt.oneman.sync;

import java.awt.*;
import javax.swing.*;

/** Consistent cards and a width-tracking stack for the narrow RuneLite sidebar. */
final class OneManCards extends JPanel implements Scrollable
{
    static final Color ACCENT=new Color(218,176,85);
    static final Color MUTED=new Color(211,216,213);
    static final Font BODY=new Font(Font.SANS_SERIF,Font.PLAIN,13);
    static final Font HEADING=BODY.deriveFont(Font.BOLD);
    /** Style only plugin-owned sidebar components, never the global RuneLite UI. */
    static void readable(Component component)
    {
        Font current=component.getFont();
        component.setFont(current!=null&&current.isBold()?HEADING:BODY);
        if(component instanceof Container)for(Component child:((Container)component).getComponents())readable(child);
    }
    OneManCards() { super(new GridBagLayout());setOpaque(false); }
    void card(JComponent card)
    {
        readable(card);
        GridBagConstraints row=new GridBagConstraints();row.gridx=0;row.gridy=getComponentCount();row.weightx=1;
        row.fill=GridBagConstraints.HORIZONTAL;row.anchor=GridBagConstraints.NORTHWEST;row.insets=new Insets(0,0,6,0);add(card,row);
    }
    void finish()
    {
        GridBagConstraints filler=new GridBagConstraints();filler.gridx=0;filler.gridy=getComponentCount();filler.weighty=1;add(Box.createGlue(),filler);
        revalidate();repaint();
    }
    static JPanel card()
    {
        JPanel card=new JPanel(new BorderLayout(0,6));card.setBackground(new Color(35,38,37));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(66,72,68)),BorderFactory.createEmptyBorder(8,8,8,8)));
        return card;
    }
    static JTextArea text(String value)
    {
        JTextArea text=new JTextArea(value);text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);text.setOpaque(false);text.setForeground(MUTED);text.setFont(BODY);return text;
    }
    static JPanel banner(String title,String subtitle)
    {
        JPanel banner=new JPanel(new BorderLayout(0,4));banner.setBackground(new Color(36,53,44));banner.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        JLabel heading=new JLabel(title);heading.setFont(HEADING);banner.add(heading,BorderLayout.NORTH);
        banner.add(text(subtitle),BorderLayout.CENTER);return banner;
    }
    static JScrollPane scroll(JComponent view)
    {
        JScrollPane scroll=new JScrollPane(view,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());scroll.setMinimumSize(new Dimension(0,0));scroll.getVerticalScrollBar().setUnitIncrement(18);return scroll;
    }
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle visible,int orientation,int direction) { return 18; }
    @Override public int getScrollableBlockIncrement(Rectangle visible,int orientation,int direction) { return Math.max(18,visible.height-18); }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
