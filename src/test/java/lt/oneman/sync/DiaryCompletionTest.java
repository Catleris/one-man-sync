package lt.oneman.sync;
import org.junit.Test;import static org.junit.Assert.*;
public class DiaryCompletionTest {
 @Test public void legacyKaramjaStateOneIsNotFinished() {
  for(String tier:new String[]{"Easy","Medium","Hard"}) {
   assertFalse(DiaryCompletion.complete("Karamja",tier,0));
   assertFalse(DiaryCompletion.complete("Karamja",tier,1));
   assertTrue(DiaryCompletion.complete("Karamja",tier,2));
  }
  assertTrue(DiaryCompletion.complete("Karamja","Elite",1));
  assertFalse(DiaryCompletion.complete("Karamja","Elite",2));
  assertTrue(DiaryCompletion.complete("Ardougne","Easy",1));
 }
}
