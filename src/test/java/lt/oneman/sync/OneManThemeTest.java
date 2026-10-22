package lt.oneman.sync;

import java.awt.*;
import javax.swing.*;
import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

public class OneManThemeTest
{
    @BeforeClass public static void runeLiteLook(){net.runelite.client.ui.laf.RuneLiteLAF.setup();}
    @Test public void stylingKeepsActionsEditableNotesAndGlobalClientDefaults()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            Object globalFont=UIManager.get("Button.font"),globalBackground=UIManager.get("Panel.background");
            JPanel panel=new JPanel();JButton button=new JButton("Existing action");JCheckBox check=new JCheckBox("Existing setting",true);JTextArea note=new JTextArea("Keep editing");
            final int[] clicked={0};button.addActionListener(e->clicked[0]++);panel.add(button);panel.add(check);panel.add(note);
            OneManTheme.apply(panel);OneManTheme.apply(panel);button.doClick();assertEquals(1,clicked[0]);check.doClick();assertFalse(check.isSelected());
            assertTrue(note.isEditable());assertTrue(note.isFocusable());assertEquals("Keep editing",note.getText());
            assertSame(globalFont,UIManager.get("Button.font"));assertSame(globalBackground,UIManager.get("Panel.background"));
            JPanel details=new JPanel();JPanel group=OneManTheme.collapsible("Details",details);assertFalse(details.isVisible());((JToggleButton)group.getComponent(0)).doClick();assertTrue(details.isVisible());
        });
    }
    @Test public void scrollingArrowsNeverBecomeEmptyTabs()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            JTabbedPane tabs=new JTabbedPane(JTabbedPane.TOP,JTabbedPane.SCROLL_TAB_LAYOUT);
            tabs.addTab("Next",new JPanel());tabs.addTab("Prep",new JPanel());
            OneManTheme.apply(tabs);OneManTheme.apply(tabs);
            tabs.setSize(242,400);layout(tabs);
            assertEquals(2,tabs.getTabCount());
            for(Component child:tabs.getComponents())if(child instanceof javax.swing.plaf.basic.BasicArrowButton)assertFalse(child.isVisible());
            for(int i=0;i<6;i++)tabs.addTab("Long section "+i,new JPanel());
            OneManTheme.apply(tabs);layout(tabs);assertEquals(8,tabs.getTabCount());
            for(int i=0;i<8;i++)assertFalse(tabs.getTitleAt(i).isEmpty());
        });
    }
    @Test public void textCardsRetainSourceContentAndPositionAfterDeferredRefresh()throws Exception{
        final JTextArea[] source={null};final OneManTextCards[] view={null};
        SwingUtilities.invokeAndWait(()->{
            source[0]=OneManCards.text(String.join("\n\n",java.util.Collections.nCopies(60,"Heading\nA wrapped detail line preserved exactly.")));
            view[0]=new OneManTextCards(source[0]);view[0].setSize(242,250);layout(view[0]);view[0].getViewport().setViewPosition(new Point(0,500));
            CompanionText.update(source[0],source[0].getText()+"\n\nNew observation\nSame task logic.");
        });
        for(int i=0;i<4;i++)SwingUtilities.invokeAndWait(()->layout(view[0]));
        SwingUtilities.invokeAndWait(()->{
            assertTrue(source[0].getText().endsWith("New observation\nSame task logic."));assertEquals(500,view[0].getViewport().getViewPosition().y);
            fits(view[0]);
        });
    }
    private static void layout(Container c){for(int i=0;i<5;i++)once(c);}private static void once(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)once((Container)child);}
    private static void fits(Container c){for(Component child:c.getComponents())if(child.isVisible()){
        assertTrue(child.getClass()+" overflows",child.getX()>=0&&child.getX()+child.getWidth()<=c.getWidth());if(child instanceof Container)fits((Container)child);
    }}
}
