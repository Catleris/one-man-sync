package lt.oneman.sync;
import net.runelite.api.ChatMessageType;import org.junit.Test;import static org.junit.Assert.*;
public class SuperiorAlertTest {
 @Test public void gameSignalDeduplicatesButAllowsLaterSpawns(){
  SuperiorAlert s=new SuperiorAlert();String message="A superior foe has appeared...";
  assertFalse(s.accept(ChatMessageType.PUBLICCHAT,message,10));
  assertFalse(s.accept(ChatMessageType.GAMEMESSAGE,"A regular foe has appeared...",10));
  assertTrue(s.accept(ChatMessageType.GAMEMESSAGE,"<col=ff0000>"+message+"</col>",10));
  assertFalse(s.accept(ChatMessageType.SPAM,message,10));
  assertFalse(s.accept(ChatMessageType.GAMEMESSAGE,message,11));
  assertTrue(s.accept(ChatMessageType.GAMEMESSAGE,message,20));
  s.reset();assertTrue(s.accept(ChatMessageType.GAMEMESSAGE,message,20));
 }
 @Test public void defaultsAreOptInAndFlashWhileFocused(){
  OneManSyncConfig c=new OneManSyncConfig(){};
  assertFalse(c.superiorSpawnAlert().isEnabled());
  assertTrue(c.superiorSpawnAlert().isSendWhenFocused());
  assertEquals(net.runelite.client.config.FlashNotification.FLASH_TWO_SECONDS,c.superiorSpawnAlert().getFlash());
  assertEquals(255,c.superiorSpawnAlert().getFlashColor().getRed());
 }
}
