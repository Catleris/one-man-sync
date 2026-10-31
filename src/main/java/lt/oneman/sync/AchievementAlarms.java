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
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.gameval.VarbitID;
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
    @Inject private QuestWindowTheme questWindow;
    @Inject private ConfigManager configs;
    @Inject private OneManSyncConfig config;
    @Inject private OverlayManager overlays;
    @Inject private AchievementAlarmOverlay overlay;
    private final AchievementAlarmQueue queue=new AchievementAlarmQueue();
    private final Map<String,Integer> itemIds=new HashMap<>();
    private final AchievementTierTracker progress=new AchievementTierTracker();
    private boolean progressDirty=true;
    private boolean tiersEnabled,diariesEnabled;
    private int warmup=10;
    private int nextItem;
    private boolean searched;
    private boolean active;
    private volatile AchievementAlarmQueue.Notice displayed;
    private volatile BufferedImage icon;
    private JLabel questStatus;
    private String lastQuestStatus="";
    private JCheckBox collectionBox,combatBox,tiersBox,diariesBox,questsBox;
    void startUp(){active=true;clear();resetProgress();overlays.add(overlay);questWindow.startUp();}
    void shutDown(){questWindow.shutDown();active=false;overlays.remove(overlay);clear();resetProgress();itemIds.clear();nextItem=0;}
    void clear(){queue.clear();displayed=null;icon=null;}
    private void resetProgress(){progress.reset();warmup=10;progressDirty=true;tiersEnabled=config.alarmCombatTiers();diariesEnabled=config.alarmDiaries();}
    void progressChanged(){progressDirty=true;}
    void onGameStateChanged(GameStateChanged event){
        GameState state=event.getGameState();questWindow.gameStateChanged(state);
        if(state!=GameState.LOGGED_IN)clear();
        if(state==GameState.LOGIN_SCREEN||state==GameState.HOPPING||state==GameState.LOGGING_IN||state==GameState.CONNECTION_LOST)resetProgress();
    }
    boolean enabled(AchievementAlarmQueue.Notice notice){
        switch(notice.kind){
        case COLLECTION:return config.alarmCollection();
        case COMBAT_TASK:return config.alarmCombat();
        case COMBAT_TIER:return config.alarmCombatTiers();
        case DIARY:return config.alarmDiaries();
        default:return false;
        }
    }
    void onWidgetLoaded(net.runelite.api.events.WidgetLoaded event){
        if(event.getGroupId()==net.runelite.api.gameval.InterfaceID.QUESTSCROLL)clientThread.invokeLater(questWindow::opened);
    }
    void onWidgetClosed(net.runelite.api.events.WidgetClosed event){
        if(event.getGroupId()==net.runelite.api.gameval.InterfaceID.QUESTSCROLL)questWindow.closed();
    }
    private void pollProgress(){
        if(warmup>0){warmup--;return;}
        if(!progressDirty)return;
        int[] thresholds=readThresholds();boolean[] diaries=readDiaries();
        for(AchievementAlarmQueue.Notice notice:progress.observe(client.getVarbitValue(VarbitID.CA_POINTS),thresholds,diaries,config.alarmCombatTiers(),config.alarmDiaries()))
            queue.add(notice,System.currentTimeMillis());
        progressDirty=false;
    }
    private int[] readThresholds(){
        int[] result=new int[CombatAchievementsHub.THRESHOLDS.length];
        for(int i=0;i<result.length;i++)result[i]=client.getVarbitValue(CombatAchievementsHub.THRESHOLDS[i]);
        return result;
    }
    private boolean[] readDiaries(){
        boolean[] result=new boolean[DiaryUnlockCatalog.ENTRIES.size()];
        for(int i=0;i<result.length;i++){
            DiaryUnlockCatalog.Entry entry=DiaryUnlockCatalog.ENTRIES.get(i);
            result[i]=entry.complete(client.getVarbitValue(entry.varbit));
        }
        return result;
    }
    AchievementAlarmQueue.Notice displayed(){return displayed;}
    BufferedImage icon(){return icon;}
    private static String key(String name){return name.toLowerCase(Locale.ROOT).trim();}
    void onChatMessage(ChatMessage event) {
        if(!active||client.getGameState()!=GameState.LOGGED_IN)return;
        if(event.getType()!=ChatMessageType.GAMEMESSAGE&&event.getType()!=ChatMessageType.SPAM)return;
        String message=Text.removeTags(event.getMessage()).replace('\u00a0',' ').trim();
        AchievementAlarmQueue.Notice notice=AchievementAlarmQueue.parse(message);
        if(notice==null||!enabled(notice))return;
        queue.add(notice,System.currentTimeMillis());refresh();
    }
    void onGameTick() {
        if(!active||client.getGameState()!=GameState.LOGGED_IN)return;
        questWindow.tick();
        String questState=questWindow.status();
        if(!questState.equals(lastQuestStatus)){
            lastQuestStatus=questState;
            SwingUtilities.invokeLater(()->{if(questStatus!=null)questStatus.setText(questState);});
        }
        pollProgress();
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
        AchievementAlarmQueue.Notice next=queue.advance(System.currentTimeMillis(),config.alarmSeconds(),this::enabled);
        if(next!=displayed){displayed=next;icon=null;searched=false;
            if(next!=null&&config.alarmSound())client.playSoundEffect(SoundEffectID.UI_BOOP);
        }
        if(next!=null&&next.kind==AchievementAlarmQueue.Kind.COLLECTION&&icon==null) {
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
    void onConfigChanged(){
        // Enabling between a varbit event and the next game tick must not backfill it.
        if(client.getGameState()==GameState.LOGGED_IN&&warmup==0){
            if(config.alarmCombatTiers()&&!tiersEnabled)progress.baselineCombat(client.getVarbitValue(VarbitID.CA_POINTS),readThresholds());
            if(config.alarmDiaries()&&!diariesEnabled)progress.baselineDiaries(readDiaries());
        }
        questWindow.configChanged();
        tiersEnabled=config.alarmCombatTiers();diariesEnabled=config.alarmDiaries();
        refresh();SwingUtilities.invokeLater(()->{if(collectionBox!=null){
            collectionBox.setSelected(config.alarmCollection());combatBox.setSelected(config.alarmCombat());
            tiersBox.setSelected(config.alarmCombatTiers());diariesBox.setSelected(config.alarmDiaries());questsBox.setSelected(config.alarmQuests());
        }});
    }
    private void preview(AchievementAlarmQueue.Kind kind,int tier,String area) {
        clientThread.invokeLater(()->{
            if(!active||client.getGameState()!=GameState.LOGGED_IN)return;
            AchievementAlarmQueue.Notice n;
            switch(kind){
            case COLLECTION:n=new AchievementAlarmQueue.Notice(kind,"Dragon warhammer","NEW ITEM",-1,true);break;
            case COMBAT_TASK:n=new AchievementAlarmQueue.Notice(kind,"Example combat task","",tier,true);break;
            case COMBAT_TIER:n=new AchievementAlarmQueue.Notice(kind,AchievementAlarmQueue.TIERS[tier]+" tier unlocked","TIER UNLOCKED",tier,true);break;
            case DIARY:n=new AchievementAlarmQueue.Notice(kind,area+" Diary","DIARY COMPLETED",tier,true);break;
            default:return;
            }
            if(!enabled(n))return;
            queue.add(n,System.currentTimeMillis());refresh();
        });
    }
    JPanel companionPanel() {
        OneManCards cards=new OneManCards();cards.card(OneManCards.banner("Achievement alarms","Legendary • local game notifications"));
        JPanel options=OneManCards.card();JPanel rows=new JPanel(new GridLayout(0,1,0,8));rows.setOpaque(false);
        collectionBox=toggle("Collection Log","alarmCollectionToggle","alarmCollection",config.alarmCollection());
        combatBox=toggle("Combat tasks","alarmCombatToggle","alarmCombat",config.alarmCombat());
        tiersBox=toggle("Combat tier unlocks","alarmCombatTiersToggle","alarmCombatTiers",config.alarmCombatTiers());
        diariesBox=toggle("Achievement Diaries","alarmDiariesToggle","alarmDiaries",config.alarmDiaries());
        questsBox=toggle("Quest window style","alarmQuestsToggle","alarmQuests",config.alarmQuests());
        rows.add(collectionBox);rows.add(combatBox);rows.add(tiersBox);rows.add(diariesBox);rows.add(questsBox);options.add(rows,BorderLayout.CENTER);cards.card(options);
        JPanel test=OneManCards.card();JPanel buttons=new JPanel(new GridLayout(0,1,0,8));buttons.setOpaque(false);
        JButton log=new JButton("Preview Collection Log");log.setName("alarmCollectionPreview");log.addActionListener(e->preview(AchievementAlarmQueue.Kind.COLLECTION,-1,""));buttons.add(log);
        JButton quest=new JButton("Enable & Preview Quest Window");quest.setName("alarmQuestsPreview");quest.addActionListener(e->{
            configs.setConfiguration("one-man-sync","alarmQuests",true);
            questsBox.setSelected(true);
            clientThread.invokeLater(()->{questWindow.configChanged();questWindow.preview();});
        });buttons.add(quest);
        JComboBox<String> combatTier=new JComboBox<>(AchievementAlarmQueue.TIERS);combatTier.setSelectedIndex(2);combatTier.setName("alarmCombatPreviewTier");
        combatTier.setToolTipText("Tier used by both combat previews");buttons.add(combatTier);
        JButton ca=new JButton("Preview Combat Task");ca.setName("alarmCombatPreview");ca.addActionListener(e->preview(AchievementAlarmQueue.Kind.COMBAT_TASK,combatTier.getSelectedIndex(),""));buttons.add(ca);
        JButton tiers=new JButton("Preview CA Tier Unlock");tiers.setName("alarmCombatTiersPreview");tiers.addActionListener(e->preview(AchievementAlarmQueue.Kind.COMBAT_TIER,combatTier.getSelectedIndex(),""));buttons.add(tiers);
        String[] areas=DiaryUnlockCatalog.ENTRIES.stream().map(entry->entry.area).distinct().toArray(String[]::new);
        JComboBox<String> diaryArea=new JComboBox<>(areas);diaryArea.setName("alarmDiaryPreviewArea");buttons.add(diaryArea);
        JComboBox<String> diaryTier=new JComboBox<>(java.util.Arrays.copyOf(AchievementAlarmQueue.TIERS,4));diaryTier.setName("alarmDiaryPreviewTier");buttons.add(diaryTier);
        JButton diary=new JButton("Preview Achievement Diary");diary.setName("alarmDiariesPreview");diary.addActionListener(e->preview(AchievementAlarmQueue.Kind.DIARY,diaryTier.getSelectedIndex(),(String)diaryArea.getSelectedItem()));buttons.add(diary);
        test.add(buttons,BorderLayout.NORTH);test.add(OneManCards.text("Enable the matching alarm and log in to preview. Previews do not change progress."),BorderLayout.CENTER);cards.card(test);
        cards.card(OneManCards.text("New unlocks only. Collection and combat task chat notifications must be enabled in OSRS. Tier and Diary alarms use game progress; old completions are not replayed on login. The Quest option styles the original completion window; it adds no banner."));
        cards.card(OneManCards.text("BUILD: "+OneManCompanion.VERSION));
        questStatus=new JLabel(questWindow.status());questStatus.setForeground(java.awt.Color.ORANGE);
        cards.card(questStatus);
        cards.finish();JPanel result=new JPanel(new BorderLayout());result.add(OneManCards.scroll(cards));return result;
    }
    private JCheckBox toggle(String label,String name,String key,boolean selected){
        JCheckBox box=new JCheckBox(label,selected);box.setName(name);
        box.addActionListener(e->configs.setConfiguration("one-man-sync",key,box.isSelected()));return box;
    }
}
