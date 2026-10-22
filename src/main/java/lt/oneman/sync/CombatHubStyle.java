package lt.oneman.sync;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

/** Explicit painting keeps OneMan colours intact under RuneLite's look and feel. */
final class CombatHubStyle
{
    static final Color BACK=new Color(30,35,33), GREEN=new Color(126,205,143);
    private CombatHubStyle(){}
    static JTextArea text(String value){
        JTextArea text=OneManCards.text(value);
        ((DefaultCaret)text.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        text.setCaretPosition(0);text.setFocusable(false);return text;
    }
    static JPanel card(Color accent,boolean selected){
        JPanel card=new JPanel(new BorderLayout(0,7)){
            @Override protected void paintComponent(Graphics graphics){
                Graphics2D g=(Graphics2D)graphics.create();smooth(g);
                g.setColor(selected?blend(BACK,accent,.10f):BACK);g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g.setColor(selected?accent:new Color(64,75,68));g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);g.dispose();
            }
        };
        card.setOpaque(false);card.setBorder(BorderFactory.createEmptyBorder(10,9,10,9));return card;
    }
    static JToggleButton button(String label,Color accent,boolean selected){
        JToggleButton button=new JToggleButton(label,selected){
            @Override protected void paintComponent(Graphics graphics){
                Graphics2D g=(Graphics2D)graphics.create();smooth(g);
                boolean on=isSelected(),hover=getModel().isRollover();
                g.setColor(blend(BACK,accent,on?.23f:hover?.12f:.04f));g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,9,9);
                g.setColor(on?accent:blend(BACK,accent,.48f));g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,9,9);
                Font font=getFont().deriveFont(Font.BOLD);if(g.getFontMetrics(font).stringWidth(getText())>getWidth()-8)font=font.deriveFont(12f);
                g.setFont(font);g.setColor(on?accent:blend(accent,Color.WHITE,.28f));FontMetrics metrics=g.getFontMetrics();
                g.drawString(getText(),(getWidth()-metrics.stringWidth(getText()))/2,(getHeight()-metrics.getHeight())/2+metrics.getAscent());
                if(hasFocus()){g.setColor(Color.WHITE);g.drawRoundRect(3,3,getWidth()-7,getHeight()-7,6,6);}g.dispose();
            }
        };
        button.setContentAreaFilled(false);button.setBorderPainted(false);button.setOpaque(false);button.setRolloverEnabled(true);
        button.setFont(OneManCards.HEADING);button.setForeground(accent);button.setMargin(new Insets(5,2,5,2));
        button.setPreferredSize(new Dimension(1,34));button.setMinimumSize(new Dimension(0,30));return button;
    }
    static JProgressBar bar(int total,int value,Color color){
        JProgressBar bar=new JProgressBar(0,Math.max(1,total)){
            @Override protected void paintComponent(Graphics graphics){
                Graphics2D g=(Graphics2D)graphics.create();smooth(g);int h=Math.min(7,getHeight()),y=(getHeight()-h)/2;
                g.setColor(new Color(58,65,61));g.fillRoundRect(0,y,getWidth(),h,h,h);
                int width=(int)Math.round(getWidth()*getPercentComplete());
                if(width>0){g.setColor(getForeground());g.fillRoundRect(0,y,width,h,h,h);}g.dispose();
            }
        };
        bar.setValue(value);bar.setForeground(color);bar.setOpaque(false);bar.setBorderPainted(false);bar.setPreferredSize(new Dimension(1,8));return bar;
    }
    static JLabel badge(String label,Color color){
        JLabel badge=new JLabel(label);badge.setForeground(color);badge.setOpaque(true);badge.setBackground(blend(BACK,color,.16f));
        badge.setBorder(BorderFactory.createEmptyBorder(3,5,3,5));badge.setFont(OneManCards.HEADING);return badge;
    }
    static Icon fallback(){return new Icon(){
        public int getIconWidth(){return 36;}public int getIconHeight(){return 36;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){
            Graphics2D g=(Graphics2D)graphics.create();smooth(g);g.translate(x,y);g.setColor(new Color(43,54,45));g.fillRoundRect(0,0,36,36,9,9);
            g.setColor(OneManCards.ACCENT);g.setStroke(new BasicStroke(1.8f));
            g.drawPolyline(new int[]{10,26,25,18,11,10},new int[]{8,8,21,28,21,8},6);g.drawLine(14,14,22,22);g.drawLine(22,14,14,22);g.dispose();
        }
    };}
    private static Color blend(Color a,Color b,float ratio){return new Color((int)(a.getRed()*(1-ratio)+b.getRed()*ratio),(int)(a.getGreen()*(1-ratio)+b.getGreen()*ratio),(int)(a.getBlue()*(1-ratio)+b.getBlue()*ratio));}
    private static void smooth(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);}
}
