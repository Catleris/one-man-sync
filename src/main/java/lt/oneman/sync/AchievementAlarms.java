package lt.oneman.sync;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Singleton
final class AchievementAlarms
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ItemManager items;
    @Inject private ConfigManager configs;
    @Inject private OneManSyncConfig config;
    @Inject private OverlayManager overlays;
    @Inject private AchievementAlarmOverlay overlay;
    private final AchievementAlarmQueue queue=new AchievementAlarmQueue();
    private final Map<String,Integer> itemIds=new HashMap<>();
    private int nextItem;
    private boolean searched;
    private boolean active;
    private volatile AchievementAlarmQueue.Notice displayed;
    private volatile BufferedImage icon;
    private JCheckBox collectionBox,combatBox;
    void startUp(){active=true;clear();overlays.add(overlay);}
    void shutDown(){active=false;overlays.remove(overlay);clear();itemIds.clear();nextItem=0;}
    void clear(){queue.clear();displayed=null;icon=null;}
    AchievementAlarmQueue.Notice displayed(){return displayed;}
    BufferedImage icon(){return icon;}
    private static String key(String name){return name.toLowerCase(Locale.ROOT).trim();}
    void onChatMessage(ChatMessage event) {
        if(!active||client.getGameState()!=GameState.LOGGED_IN)return;
        if(event.getType()!=ChatMessageType.GAMEMESSAGE&&event.getType()!=ChatMessageType.SPAM)return;
        String message=Text.removeTags(event.getMessage()).replace('\u00a0',' ').trim();
        AchievementAlarmQueue.Notice notice=AchievementAlarmQueue.parse(message);
        if(notice==null||(notice.combat?!config.alarmCombat():!config.alarmCollection()))return;
        queue.add(notice,System.currentTimeMillis());refresh();
    }
    void onGameTick() {
        if(!active||client.getGameState()!=GameState.LOGGED_IN)return;
        // Bounded cache indexing includes untradeable items and pets, unlike the GE item list.
        if(config.alarmCollection()) {
            int end=Math.min(client.getItemCount(),nextItem+256);
            for(;nextItem<end;nextItem++) {
                ItemComposition item=items.getItemComposition(nextItem);
                if(item.getNote()!=-1||item.getPlaceholderTemplateId()!=-1)continue;
                String name=item.getName();
                if(name!=null&&!"null".equals(name))itemIds.putIfAbsent(key(name),nextItem);
            }
        }
        refresh();
    }
    void refresh() {
        AchievementAlarmQueue.Notice next=queue.advance(System.currentTimeMillis(),config.alarmSeconds(),config.alarmCollection(),config.alarmCombat());
        if(next!=displayed){displayed=next;icon=null;searched=false;
            if(next!=null&&config.alarmSound())client.playSoundEffect(SoundEffectID.UI_BOOP);
        }
        if(next!=null&&!next.combat&&icon==null) {
            Integer id=itemIds.get(key(next.name));
            if(id==null&&!searched) {
                searched=true;
                for(net.runelite.http.api.item.ItemPrice item:items.search(next.name)) {
                    if(next.name.equalsIgnoreCase(item.getName())){id=item.getId();itemIds.put(key(next.name),id);break;}
                }
            }
            if(id!=null)icon=items.getImage(id);
        }
    }
    void onConfigChanged(){refresh();SwingUtilities.invokeLater(()->{if(collectionBox!=null){collectionBox.setSelected(config.alarmCollection());combatBox.setSelected(config.alarmCombat());}});}
    private void preview(boolean combat) {
        clientThread.invokeLater(()->{
            if(!active||client.getGameState()!=GameState.LOGGED_IN)return;
            boolean enabled=combat?config.alarmCombat():config.alarmCollection();
            if(!enabled)return;
            AchievementAlarmQueue.Notice n=new AchievementAlarmQueue.Notice(combat,combat?"Example combat task":"Dragon warhammer",combat?"HARD  •  +3 points":"NEW ITEM",combat?2:-1,true);
            queue.add(n,System.currentTimeMillis());refresh();
        });
    }
    JPanel companionPanel() {
        OneManCards cards=new OneManCards();cards.card(OneManCards.banner("Collection / Combat alarms","Legendary • local game notifications"));
        JPanel options=OneManCards.card();JPanel rows=new JPanel(new GridLayout(0,1,0,8));rows.setOpaque(false);
        collectionBox=new JCheckBox("Collection Log alarm",config.alarmCollection());combatBox=new JCheckBox("Combat Achievement alarm",config.alarmCombat());
        collectionBox.addActionListener(e->configs.setConfiguration("one-man-sync","alarmCollection",collectionBox.isSelected()));
        combatBox.addActionListener(e->configs.setConfiguration("one-man-sync","alarmCombat",combatBox.isSelected()));
        rows.add(collectionBox);rows.add(combatBox);options.add(rows,BorderLayout.CENTER);cards.card(options);
        JPanel test=OneManCards.card();JPanel buttons=new JPanel(new GridLayout(0,1,0,8));buttons.setOpaque(false);
        JButton log=new JButton("Preview Collection Log");log.addActionListener(e->preview(false));buttons.add(log);
        JButton ca=new JButton("Preview Combat Achievement");ca.addActionListener(e->preview(true));buttons.add(ca);
        test.add(buttons,BorderLayout.NORTH);test.add(OneManCards.text("Enable the matching alarm and log in to preview. Preview messages do not change your progress."),BorderLayout.CENTER);cards.card(test);
        cards.card(OneManCards.text("New unlocks only. Enable Collection log notifications and Combat task completion messages in OSRS settings. Old entries and opening your log do not trigger alarms."));
        cards.finish();JPanel result=new JPanel(new BorderLayout());result.add(OneManCards.scroll(cards));return result;
    }
}
