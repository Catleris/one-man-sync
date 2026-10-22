package lt.oneman.sync;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import static lt.oneman.sync.CombatAchievementCatalog.*;

/** Exercise deferred EDT caret/layout work, rather than checking only immediate button state. */
public class CombatHubVisualTest
{
    @BeforeClass public static void runeLiteTheme(){net.runelite.client.ui.laf.RuneLiteLAF.setup();}
    @Test public void filtersReturnToTopAfterQueuedCaretAndLayoutWork() throws Exception {
        CombatAchievementsPanel[] panel={null};JScrollPane[] scroll={null};
        SwingUtilities.invokeAndWait(()->{
            panel[0]=new CombatAchievementsPanel((id,on,profile)->{});panel[0].setSize(242,700);panel[0].update(data());
            find(panel[0],"combatFiltersToggle",JToggleButton.class).doClick();layout(panel[0]);
            scroll[0]=find(panel[0],"combatResultsScroll",JScrollPane.class);
        });drain();
        for(Tier tier:Tier.values()){
            SwingUtilities.invokeAndWait(()->{
                layout(panel[0]);scroll[0].getVerticalScrollBar().setValue(scroll[0].getVerticalScrollBar().getMaximum());
                find(panel[0],"combatTier:"+tier.name(),JToggleButton.class).doClick();layout(panel[0]);
            });drain();
            SwingUtilities.invokeAndWait(()->{layout(panel[0]);assertEquals("Filter must stay at top after delayed caret work: "+tier,0,scroll[0].getViewport().getViewPosition().y);});
        }
        SwingUtilities.invokeAndWait(()->{
            for(Tier tier:Tier.values())find(panel[0],"combatTier:"+tier.name(),JToggleButton.class).doClick();
            find(panel[0],"combatView:Tasks",JToggleButton.class).doClick();find(panel[0],"combatSearch",JTextField.class).setText("Task");layout(panel[0]);
        });drain();
        SwingUtilities.invokeAndWait(()->{
            assertEquals(0,scroll[0].getViewport().getViewPosition().y);
            int position=100;scroll[0].getVerticalScrollBar().setValue(position);panel[0].update(data());layout(panel[0]);
        });drain();
        SwingUtilities.invokeAndWait(()->assertEquals("Passive updates preserve the reader's position",100,scroll[0].getViewport().getViewPosition().y));
    }
    @Test public void tierPaintRemainsColouredAndDescriptionsCannotScrollTheViewport() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            for(Tier tier:Tier.values()){
                JToggleButton button=CombatHubStyle.button(tier.label,tier.color,true);button.setSize(108,34);
                BufferedImage image=new BufferedImage(108,34,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();button.paint(g);g.dispose();
                Color pixel=new Color(image.getRGB(8,8));assertTrue("Tint must be visible for "+tier,pixel.getRGB()!=CombatHubStyle.BACK.getRGB());
                if(tier==Tier.ELITE)assertTrue(pixel.getBlue()>pixel.getRed());
                if(tier==Tier.MASTER)assertTrue(pixel.getRed()>pixel.getBlue());
                if(tier==Tier.GRANDMASTER)assertTrue(pixel.getRed()>pixel.getBlue()&&pixel.getGreen()>pixel.getBlue());
                button.doClick();BufferedImage off=new BufferedImage(108,34,BufferedImage.TYPE_INT_ARGB);g=off.createGraphics();button.paint(g);g.dispose();assertNotEquals(image.getRGB(8,8),off.getRGB(8,8));
            }
            JTextArea description=CombatHubStyle.text("A long task description");assertFalse(description.isFocusable());
            assertEquals(DefaultCaret.NEVER_UPDATE,((DefaultCaret)description.getCaret()).getUpdatePolicy());
            CombatAchievementsPanel panel=new CombatAchievementsPanel((id,on,profile)->{});panel.update(data());panel.setSize(242,700);
            find(panel,"combatFiltersToggle",JToggleButton.class).doClick();find(panel,"combatBoss:Boss 00",JToggleButton.class).doClick();layout(panel);fits(panel);
            assertNotNull(find(panel,"combatPortrait:Boss 00",JLabel.class).getIcon());
        });
    }
    private static CombatAchievementsHub.Snapshot data(){
        List<Task> tasks=new ArrayList<>();for(int i=0;i<180;i++)tasks.add(new Task(i,"Task "+i,"A long description that wraps over several lines and must not move the scrollbar.",String.format("Boss %02d",i/6),"Mechanical",Tier.values()[i%6]));
        return new CombatAchievementsHub.Snapshot(tasks,Collections.singleton(0),Collections.singleton(1),new int[]{33,115,304,820,1465,2300},1,true,"profile");
    }
    private static void drain() throws Exception {for(int i=0;i<4;i++)SwingUtilities.invokeAndWait(()->{});}
    private static <T extends Component>T find(Container c,String name,Class<T> type){for(Component child:c.getComponents()){
        if(name.equals(child.getName())&&type.isInstance(child))return type.cast(child);if(child instanceof Container){T found=find((Container)child,name,type);if(found!=null)return found;}}
        return null;
    }
    private static void layout(Container c){for(int pass=0;pass<4;pass++)layoutOnce(c);}
    private static void layoutOnce(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layoutOnce((Container)child);}
    private static void fits(Container c){for(Component child:c.getComponents())if(child.isVisible()){
        assertTrue(child.getClass()+" overflows",child.getX()>=0&&child.getX()+child.getWidth()<=c.getWidth());if(child instanceof Container)fits((Container)child);
    }}
}
