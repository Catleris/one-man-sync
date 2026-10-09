package lt.oneman.sync;
import net.runelite.api.ChatMessageType;
import net.runelite.client.util.Text;
/** Game's own Superior message covers all Superior types, including newly added NPCs. */
final class SuperiorAlert {
 private int lastTick=Integer.MIN_VALUE;
 boolean accept(ChatMessageType type,String message,int tick) {
  if(type!=ChatMessageType.GAMEMESSAGE&&type!=ChatMessageType.SPAM)return false;
  if(message==null||!Text.removeTags(message).trim().equals("A superior foe has appeared..."))return false;
  if(lastTick!=Integer.MIN_VALUE&&tick>=lastTick&&tick-lastTick<=2)return false;
  lastTick=tick;return true;
 }
 void reset(){lastTick=Integer.MIN_VALUE;}
}
