package lt.oneman.sync;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BirdhouseRunsPanelTest
{
    @Test public void observationStatesKeepTheirExistingTimingAndUnknownSemantics(){
        long seen=1700000000L;
        assertEquals(BirdhouseRun.Status.EMPTY,BirdhouseRun.read(0,"0:"+seen,seen).status);
        assertEquals(BirdhouseRun.Status.NEEDS_SEEDS,BirdhouseRun.read(0,"22:"+seen,seen).status);
        BirdhouseRun growing=BirdhouseRun.read(0,"24:"+seen,seen+1500);
        assertEquals(BirdhouseRun.Status.GROWING,growing.status);assertEquals("Magic Bird House",growing.house);assertEquals(50,growing.progress());assertEquals("25m 0s left",growing.time());
        assertEquals(BirdhouseRun.Status.READY,BirdhouseRun.read(0,"24:"+seen,seen+3000).status);
        assertEquals("Estimated ready",BirdhouseRun.read(0,"24:"+seen,seen+3000).label());
        for(String raw:new String[]{null,"","bad","3:0","28:"+seen,"-1:"+seen,"3:bad","3:1:extra"})assertEquals(BirdhouseRun.Status.UNKNOWN,BirdhouseRun.read(0,raw,seen).status);
    }
    @Test public void fourStableCardsFitNarrowSidebarAndClearOnLogout()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            BirdhouseRunsPanel panel=new BirdhouseRunsPanel(null,()->{});panel.setSize(242,600);
            List<BirdhouseRun> runs=Arrays.asList(BirdhouseRun.read(0,"24:1700000000",1700003000),BirdhouseRun.read(1,"24:1700000000",1700001500),BirdhouseRun.read(2,"22:1700000000",1700001500),BirdhouseRun.unknown(3));
            panel.update(runs,"In progress");layout(panel);fits(panel);
            JPanel first=find(panel,"birdhouseCard:0",JPanel.class);assertNotNull(first);
            assertEquals("Estimated ready",find(panel,"birdhouseStatus:0",JLabel.class).getText());
            assertTrue(find(panel,"birdhouseDetail:1",JTextArea.class).getText().contains("25m"));
            panel.update(runs,"Complete");assertSame("Refreshes must not detach the user's cards",first,find(panel,"birdhouseCard:0",JPanel.class));
            assertEquals("Complete",find(panel,"farmingContractStatus",JLabel.class).getText());
            panel.loggedOut();assertEquals("Log in",find(panel,"birdhouseStatus:0",JLabel.class).getText());assertEquals("Not checked",find(panel,"farmingContractStatus",JLabel.class).getText());
            for(int i=0;i<4;i++)assertEquals("",find(panel,"birdhouseDetail:"+i,JTextArea.class).getText());layout(panel);fits(panel);
        });
    }
    private static <T extends Component>T find(Container c,String name,Class<T> type){for(Component child:c.getComponents()){
        if(name.equals(child.getName())&&type.isInstance(child))return type.cast(child);if(child instanceof Container){T found=find((Container)child,name,type);if(found!=null)return found;}}
        return null;
    }
    private static void layout(Container c){for(int i=0;i<6;i++)once(c);}private static void once(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)once((Container)child);}
    private static void fits(Container c){for(Component child:c.getComponents())if(child.isVisible()){
        assertTrue(child.getClass()+" overflows",child.getX()>=0&&child.getX()+child.getWidth()<=c.getWidth());if(child instanceof Container)fits((Container)child);
    }}
}
