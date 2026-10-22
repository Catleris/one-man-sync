package lt.oneman.sync;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.DefaultCaret;

/** Applies only to OneMan-owned components. No UIManager defaults or native client panels. */
final class OneManTheme
{
    static final Color CANVAS=new Color(24,29,26), INPUT=new Color(27,33,29);
    private OneManTheme(){}
    static void apply(Component component){
        Font font=component.getFont();component.setFont(font!=null&&font.isBold()?OneManCards.HEADING:OneManCards.BODY);
        if(component instanceof JLabel){if(component.getForeground() instanceof UIResource)component.setForeground(OneManCards.MUTED);return;}
        if(component instanceof JCheckBox){
            JCheckBox box=(JCheckBox)component;box.setOpaque(false);box.setForeground(OneManCards.MUTED);box.setIcon(CHECK);box.setSelectedIcon(CHECK);return;
        }
        if(component instanceof AbstractButton){
            AbstractButton button=(AbstractButton)component;
            if(!(button.getUI() instanceof ButtonUI))button.setUI(new ButtonUI());
            button.setContentAreaFilled(false);button.setBorderPainted(false);button.setOpaque(false);button.setRolloverEnabled(true);
            button.setMargin(new Insets(6,6,6,6));
            if(button.getForeground() instanceof UIResource)button.setForeground(OneManCards.ACCENT);
            if(button.getToolTipText()==null&&button.getText()!=null&&!button.getText().startsWith("<html>"))button.setToolTipText(button.getText());return;
        }
        if(component instanceof JComboBox){
            JComboBox<?> box=(JComboBox<?>)component;box.setBackground(INPUT);box.setForeground(OneManCards.MUTED);return;
        }
        if(component instanceof JTextField){component.setBackground(INPUT);component.setForeground(Color.WHITE);((JTextField)component).setCaretColor(OneManCards.ACCENT);return;}
        if(component instanceof JTextArea){
            JTextArea area=(JTextArea)component;
            if(!area.isEditable()){((DefaultCaret)area.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);area.setFocusable(false);area.setOpaque(false);}
            else{area.setBackground(INPUT);area.setForeground(Color.WHITE);area.setCaretColor(OneManCards.ACCENT);}
            if(area.getForeground() instanceof UIResource)area.setForeground(OneManCards.MUTED);return;
        }
        if(component instanceof JTabbedPane){JTabbedPane tabs=(JTabbedPane)component;if(!(tabs.getUI() instanceof TabsUI))tabs.setUI(new TabsUI());tabs.setForeground(OneManCards.MUTED);tabs.setBackground(CANVAS);}
        if(component instanceof JScrollPane){JScrollPane pane=(JScrollPane)component;pane.setBorder(BorderFactory.createEmptyBorder());pane.getViewport().setBackground(CANVAS);pane.getVerticalScrollBar().setUnitIncrement(18);}
        if(component instanceof JPanel&&component.getBackground() instanceof UIResource)((JPanel)component).setOpaque(false);
        if(component instanceof Container)for(Component child:((Container)component).getComponents())apply(child);
    }
    static JPanel collapsible(String title,JComponent content){
        JPanel panel=OneManCards.card();JToggleButton toggle=new JToggleButton(title);toggle.setName("oneManExpand:"+title);content.setVisible(false);
        toggle.addActionListener(e->{content.setVisible(toggle.isSelected());panel.revalidate();panel.repaint();});
        panel.add(toggle,BorderLayout.NORTH);panel.add(content);apply(panel);return panel;
    }
    private static void smooth(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);}
    private static final class ButtonUI extends BasicButtonUI {
        @Override public void paint(Graphics graphics,JComponent component){
            AbstractButton button=(AbstractButton)component;Graphics2D g=(Graphics2D)graphics.create();smooth(g);
            Color accent=OneManCards.ACCENT;boolean selected=button.getModel().isSelected(),hover=button.getModel().isRollover();
            g.setColor(!button.isEnabled()?new Color(31,36,32):selected?new Color(74,65,39):hover?new Color(49,53,39):new Color(32,39,33));
            g.fillRoundRect(0,0,component.getWidth()-1,component.getHeight()-1,8,8);
            g.setColor(!button.isEnabled()?new Color(58,65,59):selected||hover?accent:new Color(91,85,57));
            g.drawRoundRect(0,0,component.getWidth()-1,component.getHeight()-1,8,8);g.dispose();super.paint(graphics,component);
        }
        @Override protected void paintButtonPressed(Graphics g,AbstractButton b){}
        @Override protected void paintFocus(Graphics g,AbstractButton b,Rectangle view,Rectangle text,Rectangle icon){g.setColor(OneManCards.ACCENT);g.drawRoundRect(3,3,b.getWidth()-7,b.getHeight()-7,5,5);}
    }
    private static final Icon CHECK=new Icon(){
        public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){
            Graphics2D g=(Graphics2D)graphics.create();smooth(g);g.translate(x,y);AbstractButton b=(AbstractButton)c;boolean on=b.isSelected();
            g.setColor(on?new Color(77,67,37):INPUT);g.fillRoundRect(0,0,15,15,4,4);g.setColor(on?OneManCards.ACCENT:new Color(94,105,96));g.drawRoundRect(0,0,15,15,4,4);
            if(on){g.setStroke(new BasicStroke(2));g.drawPolyline(new int[]{3,6,12},new int[]{8,11,4},3);}g.dispose();
        }
    };
    private static final class TabsUI extends BasicTabbedPaneUI {
        @Override protected JButton createScrollButton(int direction){return new BasicArrowButton(direction,CANVAS,new Color(65,76,66),OneManCards.ACCENT,CANVAS);}
        @Override protected void installDefaults(){super.installDefaults();tabInsets=new Insets(7,8,7,8);tabAreaInsets=new Insets(2,0,4,0);contentBorderInsets=new Insets(0,0,0,0);}
        @Override protected void paintTabBackground(Graphics graphics,int placement,int index,int x,int y,int w,int h,boolean selected){
            Graphics2D g=(Graphics2D)graphics.create();smooth(g);g.setColor(selected?new Color(74,65,39):new Color(31,38,33));g.fillRoundRect(x,y,w-2,h-2,8,8);g.dispose();
        }
        @Override protected void paintTabBorder(Graphics graphics,int placement,int index,int x,int y,int w,int h,boolean selected){
            Graphics2D g=(Graphics2D)graphics.create();smooth(g);g.setColor(selected?OneManCards.ACCENT:new Color(65,76,66));g.drawRoundRect(x,y,w-2,h-2,8,8);g.dispose();
        }
        @Override protected void paintText(Graphics g,int placement,Font font,FontMetrics metrics,int index,String title,Rectangle bounds,boolean selected){
            g.setFont(font);g.setColor(selected?OneManCards.ACCENT:OneManCards.MUTED);g.drawString(title,bounds.x,bounds.y+metrics.getAscent());
        }
        @Override protected void paintContentBorder(Graphics g,int placement,int selectedIndex){}
        @Override protected void paintFocusIndicator(Graphics g,int placement,Rectangle[] bounds,int index,Rectangle icon,Rectangle text,boolean selected){}
    }
}
