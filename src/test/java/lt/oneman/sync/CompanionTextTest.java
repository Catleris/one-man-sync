package lt.oneman.sync;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class CompanionTextTest {
 @Test public void changingTextPreservesScrolledPosition() throws Exception {
  final JTextArea[] area=new JTextArea[1]; final JScrollPane[] pane=new JScrollPane[1];
  SwingUtilities.invokeAndWait(()->{
   area[0]=new JTextArea(String.join("\n",java.util.Collections.nCopies(200,"Read this line")));
   pane[0]=new JScrollPane(area[0]);pane[0].setSize(240,180);pane[0].doLayout();
   area[0].setSize(220,4000);pane[0].getViewport().setViewPosition(new Point(0,800));
   CompanionText.update(area[0],area[0].getText()+"\nUpdated");
  });
  SwingUtilities.invokeAndWait(()->assertEquals(800,pane[0].getViewport().getViewPosition().y));
 }
 @Test public void unchangedTextDoesNotReplaceDocumentContents() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   JTextArea area=new JTextArea("Same"); final int[] changes={0};
   area.getDocument().addDocumentListener(new DocumentListener(){
    public void insertUpdate(DocumentEvent e){changes[0]++;}
    public void removeUpdate(DocumentEvent e){changes[0]++;}
    public void changedUpdate(DocumentEvent e){changes[0]++;}
   });
   CompanionText.update(area,"Same");assertEquals(0,changes[0]);
  });
 }
}
