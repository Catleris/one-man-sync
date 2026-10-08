package lt.oneman.sync;
import java.awt.Point;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
/** Keep passive updates from pulling readers back to the caret. Call on the EDT. */
final class CompanionText {
 private CompanionText() {}
 static void update(JTextArea area,String text) {
  if(area.getText().equals(text))return;
  JViewport viewport=(JViewport)SwingUtilities.getAncestorOfClass(JViewport.class,area);
  Point position=viewport==null?null:viewport.getViewPosition();
  ((DefaultCaret)area.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
  area.setText(text);
  if(viewport!=null) {
   viewport.setViewPosition(position);
   SwingUtilities.invokeLater(()->{if(area.getText().equals(text))viewport.setViewPosition(position);});
  }
 }
}
