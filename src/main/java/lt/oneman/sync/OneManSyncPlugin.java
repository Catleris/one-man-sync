package lt.oneman.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.*;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.slayer.SlayerConfig;
import net.runelite.client.util.Text;

@PluginDescriptor(
    name = "OneMan Hub",
    description = "Local Slayer, Farming, Clue, monster and progression tools",
    tags = {"slayer", "farming", "birdhouse", "clue", "monsters", "weakness", "achievements", "collection"},
    internalName = "one-man-sync"
)
@net.runelite.client.plugins.PluginDependency(net.runelite.client.plugins.slayer.SlayerPlugin.class)
public class OneManSyncPlugin extends Plugin
{
    private static final Logger log = Logger.getLogger(OneManSyncPlugin.class.getName());
    private static final Pattern SLAYER_PROGRESS_MESSAGE =
        Pattern.compile("^(?:You're assigned to kill|You have received a new Slayer assignment from .*:) (?:[Tt]he )?(?<name>.+?)(?: (?:in|on|south of) (?:the )?(?<location>[^;]+))?(?:; only | \\()(?<amount>\\d+)(?: more to go\\.|\\))$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SLAYER_CURRENT_DIALOG =
        Pattern.compile("^You're still (?:hunting|bringing balance to) (?<name>.+?)(?: (?:in|on|south of) (?:the )?(?<location>.+?))?(?:, with|; you have) (?<amount>\\d+) to go\\..*$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SLAYER_ASSIGN_DIALOG =
        Pattern.compile(".*(?:Your new task is to kill|You are to bring balance to)\\s*(?<amount>\\d+) (?<name>.+?)(?: (?:in|on|south of) (?:the )?(?<location>.+))?\\.$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SLAYER_FIRST_ASSIGN_DIALOG =
        Pattern.compile("^We'll start you off (?:hunting|bringing balance to) (?<name>.+?), you'll need to kill (?<amount>\\d+) of them\\.$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SLAYER_BRACELET_PROGRESS =
        Pattern.compile("^You still need to kill (?<amount>\\d+) monsters to complete your current Slayer assignment.*$", Pattern.CASE_INSENSITIVE);

    private static final String SLAYER_NO_TASK_MESSAGE = "You need something new to hunt.";
    private static final String SLAYER_CANCEL_MESSAGE = "Your task has been cancelled.";
    private static final String SLAYER_CANCEL_JAD_MESSAGE = "You no longer have a slayer task as you left the fight cave.";

    @Inject private OneManSlayerHelper slayerHelper;
    @Inject private OneManCompanion companion;
    @Inject private ClueHub clueHub;
    @Inject private MonstersHub monstersHub;
    @Inject private UnlocksHub unlocksHub;
    @Inject private CombatAchievementsHub combatHub;
    @Inject private OneManMapLocations mapLocations;
    @Inject private AchievementAlarms achievementAlarms;
    @Inject private StashMinimapOverlay stashMinimapOverlay;
    @Inject private net.runelite.client.ui.overlay.OverlayManager mapOverlays;
    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    @Inject private ConfigManager configManager;
    @Inject private ClientThread clientThread;

    private volatile JsonObject slayerStateCache;
    private int lastSlayerRefreshTick = -100000;
    @Inject private net.runelite.client.eventbus.EventBus eventBus;

    @Provides
    OneManSyncConfig provideConfig(ConfigManager configManager)
    {
        OneManConfigMigration.migrate(configManager);
        return configManager.getConfig(OneManSyncConfig.class);
    }

    @Override
    protected void startUp()
    {
        achievementAlarms.startUp();
        mapLocations.startUp();
        mapOverlays.add(stashMinimapOverlay);
        clueHub.startUp();
        monstersHub.startUp(this::localSlayerState);
        unlocksHub.startUp();
        combatHub.startUp();
        slayerHelper.startUp(this::localSlayerState);
        companion.startUp(this::getPluginDirectory, () -> eventBus.post(OneManSettings.request(this)));
        if (client.getGameState() == GameState.LOGGED_IN)
            clientThread.invokeLater(this::refreshSlayerStateCache);
    }

    @Override
    protected void shutDown()
    {
        achievementAlarms.shutDown();
        mapOverlays.remove(stashMinimapOverlay);
        mapLocations.shutDown();
        unlocksHub.shutDown();
        combatHub.shutDown();
        monstersHub.shutDown();
        clueHub.shutDown();
        companion.shutDown();
        slayerHelper.shutDown();
        slayerStateCache = null;
        lastSlayerRefreshTick = -100000;
    }

    @Subscribe
    public void onNpcSpawned(net.runelite.api.events.NpcSpawned event) { monstersHub.onNpcSpawned(event); }
    @Subscribe
    public void onNpcDespawned(net.runelite.api.events.NpcDespawned event) { monstersHub.onNpcDespawned(event); }
    @Subscribe
    public void onNpcChanged(net.runelite.api.events.NpcChanged event) { monstersHub.onNpcChanged(event); }
    @Subscribe
    public void onItemSpawned(ItemSpawned event) { clueHub.onItemSpawned(event); }
    @Subscribe
    public void onItemDespawned(ItemDespawned event) { clueHub.onItemDespawned(event); }
    @Subscribe
    public void onMenuOptionClicked(net.runelite.api.events.MenuOptionClicked event) { clueHub.onMenuOptionClicked(event); }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) { clueHub.onMenuEntryAdded(event); }
    @Subscribe
    public void onMenuOpened(net.runelite.api.events.MenuOpened event) { companion.onMenuOpened(event); monstersHub.onMenuOpened(event); }
    @Subscribe
    public void onPostMenuSort(net.runelite.api.events.PostMenuSort event) { monstersHub.onPostMenuSort(); }
    @Subscribe
    public void onBeforeRender(net.runelite.api.events.BeforeRender event){achievementAlarms.beforeRender();}
    @Subscribe
    public void onWidgetLoaded(net.runelite.api.events.WidgetLoaded event){achievementAlarms.onWidgetLoaded(event);}
    @Subscribe
    public void onWidgetClosed(net.runelite.api.events.WidgetClosed event){achievementAlarms.onWidgetClosed(event);}
    @Subscribe
    public void onConfigChanged(net.runelite.client.events.ConfigChanged event)
    {
        if ("one-man-sync".equals(event.getGroup()) && event.getKey().startsWith("alarm"))
            clientThread.invokeLater(achievementAlarms::onConfigChanged);
        if ("one-man-sync".equals(event.getGroup()) && "companionRoadmapVisible".equals(event.getKey()))
            companion.refreshViews();
        if ("one-man-sync".equals(event.getGroup()) && "slayerHistoryVisible".equals(event.getKey()))
            slayerHelper.refreshOptionalViews();
        if ("one-man-sync".equals(event.getGroup()) && event.getKey().startsWith("unlocks"))
            unlocksHub.onConfigChanged();
        if ("one-man-sync".equals(event.getGroup()) && event.getKey().startsWith("monsters"))
            clientThread.invokeLater(monstersHub::onConfigChanged);
        if ("one-man-sync".equals(event.getGroup()) && event.getKey().startsWith("clue"))
            clientThread.invokeLater(clueHub::onConfigChanged);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        achievementAlarms.onGameStateChanged(event);
        unlocksHub.onGameStateChanged(event);
        combatHub.onGameStateChanged(event);
        monstersHub.onGameStateChanged(event);
        clueHub.onGameStateChanged(event);
        slayerHelper.onGameStateChanged(event);
        companion.onGameStateChanged(event);
        if (event.getGameState() == GameState.LOGGED_IN)
            clientThread.invokeLater(this::refreshSlayerStateCache);
        else if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
        {
            slayerStateCache = null;
            lastSlayerRefreshTick = -100000;
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        unlocksHub.changed();
        slayerHelper.onStatChanged(event);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        achievementAlarms.progressChanged();
        combatHub.changed();
        unlocksHub.changed();
        if (client.getGameState() == GameState.LOGGED_IN && isSlayerSignal(event))
            clientThread.invokeLater(this::refreshSlayerStateCache);
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event)
    {
        clueHub.onNpcLootReceived(event);
        slayerHelper.onNpcLootReceived(event);
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        unlocksHub.onItemContainerChanged(event);
        clueHub.onItemContainerChanged(event);
        slayerHelper.onItemContainerChanged(event);
        companion.onFarmingItems(event);
        if (client.getGameState() == GameState.LOGGED_IN && event.getContainerId() == InventoryID.BANK
            && event.getItemContainer() != null)
            monstersHub.onBankChanged(event.getItemContainer().getItems());
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        achievementAlarms.onChatMessage(event);
        clueHub.onChatMessage(event);
        slayerHelper.onChatMessage(event);
        companion.onFarmingChat(event);
        if (client.getGameState() != GameState.LOGGED_IN) return;
        ChatMessageType type = event.getType();
        if (type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.SPAM || type == ChatMessageType.DIALOG
            || type == ChatMessageType.MESBOX || type == ChatMessageType.NPC_SAY)
            captureSlayerMessage(cleanMessage(event.getMessage()));
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        mapLocations.tick();
        achievementAlarms.onGameTick();
        clueHub.onGameTick();
        monstersHub.onGameTick();
        unlocksHub.onGameTick();
        combatHub.onGameTick();
        slayerHelper.onGameTick(event);
        companion.onGameTick(event);
        if (client.getGameState() == GameState.LOGGED_IN && client.getTickCount() - lastSlayerRefreshTick >= 50)
            refreshSlayerStateCache();
    }

    private boolean isSlayerSignal(VarbitChanged event)
    {
        int varpId=event.getVarpId();
        int varbitId=event.getVarbitId();
        return varpId==VarPlayerID.SLAYER_COUNT
            || varpId==VarPlayerID.SLAYER_AREA
            || varpId==VarPlayerID.SLAYER_TARGET
            || varpId==VarPlayerID.SLAYER_COUNT_ORIGINAL
            || varpId==VarPlayerID.SLAYER_MORTIMER_TASKS_COMPLETED
            || varbitId==VarbitID.SLAYER_TARGET_BOSSID
            || varbitId==VarbitID.SLAYER_MODIFIER_ID
            || varbitId==VarbitID.SLAYER_MODIFIER_VALUE
            || varbitId==VarbitID.SLAYER_MODIFIER_NEGATIVE
            || varbitId==VarbitID.SLAYER_POINTS
            || varbitId==VarbitID.SLAYER_TASKS_COMPLETED
            || varbitId==VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED
            || varpId==VarPlayerID.SLAYER_REWARDS_UNLOCKS
            || varpId==VarPlayerID.SLAYER_REWARDS_UNLOCKS1
            || varpId==VarPlayerID.SLAYER_REWARDS_UNLOCKS2
            || varbitId==VarbitID.SLAYER_UNLOCK_SUPERIORMOBS
            || varbitId==VarbitID.SLAYER_TOGGLEOFF_SUPERIORMOBS
            || varbitId==VarbitID.SLAYER_AUTOKILL_ROCKSLUGS
            || varbitId==VarbitID.SLAYER_AUTOKILL_DESERTLIZARDS;
    }

    private JsonObject localSlayerState()
    {
        JsonObject cached=slayerStateCache;
        if(cached==null || client.getTickCount()-lastSlayerRefreshTick>=5
            || jsonInt(cached,"rawTaskCount")!=client.getVarpValue(VarPlayerID.SLAYER_COUNT)
            || jsonInt(cached,"rawTaskTarget")!=client.getVarpValue(VarPlayerID.SLAYER_TARGET)
            || jsonInt(cached,"rawTaskArea")!=client.getVarpValue(VarPlayerID.SLAYER_AREA)) refreshSlayerStateCache();
        return slayerStateCache;
    }

    private void refreshSlayerStateCache()
    {
        if(client.getGameState()!=GameState.LOGGED_IN || client.getLocalPlayer()==null) return;
        try
        {
            JsonObject direct=readSlayerStateDirect();
            JsonObject old=slayerStateCache;

            direct=OneManSlayerState.reconcile(direct,old,System.currentTimeMillis());

            slayerStateCache=direct;
            lastSlayerRefreshTick=client.getTickCount();
        }
        catch(Exception ex)
        {
            log.fine("Slayer state refresh failed: "+ex.getMessage());
        }
    }

    private static int jsonInt(JsonObject o,String key)
    {
        try{return o!=null && o.has(key)?o.get(key).getAsInt():0;}
        catch(Exception ex){return 0;}
    }

    private boolean captureSlayerMessage(String message)
    {
        if(message==null || message.isEmpty()) return false;

        if(message.equalsIgnoreCase(SLAYER_NO_TASK_MESSAGE)
            || message.equalsIgnoreCase(SLAYER_CANCEL_MESSAGE)
            || message.equalsIgnoreCase(SLAYER_CANCEL_JAD_MESSAGE))
        {
            JsonObject state=readSlayerStateDirect();
            state.addProperty("taskName","");
            state.addProperty("amount",0);
            state.addProperty("initialAmount",0);
            state.addProperty("location","");
            state.addProperty("taskSource","chat-clear");
            state.addProperty("taskStatus","none");
            state.addProperty("taskSignalValid",true);
            state.addProperty("lastTaskSeenEpochMs",System.currentTimeMillis());
            slayerStateCache=state;
            return true;
        }

        Matcher m=SLAYER_PROGRESS_MESSAGE.matcher(message);
        if(m.matches())
        {
            cacheSlayerTaskFromMessage(m.group("name"),m.group("amount"),m.group("location"),false);
            return true;
        }

        m=SLAYER_CURRENT_DIALOG.matcher(message);
        if(m.matches())
        {
            cacheSlayerTaskFromMessage(m.group("name"),m.group("amount"),m.group("location"),false);
            return true;
        }

        m=SLAYER_ASSIGN_DIALOG.matcher(message);
        if(m.matches())
        {
            cacheSlayerTaskFromMessage(m.group("name"),m.group("amount"),m.group("location"),true);
            return true;
        }

        m=SLAYER_FIRST_ASSIGN_DIALOG.matcher(message);
        if(m.matches())
        {
            cacheSlayerTaskFromMessage(m.group("name"),m.group("amount"),"",true);
            return true;
        }

        m=SLAYER_BRACELET_PROGRESS.matcher(message);
        if(m.matches())
        {
            JsonObject old=slayerStateCache;
            String name=old!=null && old.has("taskName")?old.get("taskName").getAsString():"";
            if(!name.isEmpty())
            {
                cacheSlayerTaskFromMessage(name,m.group("amount"),
                    old.has("location")?old.get("location").getAsString():"",false);
                return true;
            }
        }

        return false;
    }

    private void cacheSlayerTaskFromMessage(String name,String amountText,String location,boolean newAssignment)
    {
        if(name==null || name.trim().isEmpty()) return;
        int amount=Math.max(0,safeInt(amountText,0));
        JsonObject state=readSlayerStateDirect();
        JsonObject old=slayerStateCache;

        state.addProperty("taskName",name.trim());
        state.addProperty("amount",amount);
        int original=jsonInt(state,"initialAmount");
        if(original<=0 && old!=null && old.has("taskName") && name.trim().equalsIgnoreCase(old.get("taskName").getAsString()))
            original=jsonInt(old,"initialAmount");
        state.addProperty("initialAmount",newAssignment?amount:Math.max(amount,original));
        state.addProperty("location",location==null?"":location.trim());
        state.addProperty("taskSource","chat");
        state.addProperty("taskSignalValid",true);
        state.addProperty("taskStatus",amount>0?"active":"none");
        state.addProperty("lastTaskSeenEpochMs",System.currentTimeMillis());

        slayerStateCache=state;
        lastSlayerRefreshTick=client.getTickCount();
    }

    private JsonObject readSlayerStateDirect()
    {
        JsonObject o=new JsonObject();

        int level=Math.max(1,client.getRealSkillLevel(Skill.SLAYER));
        int points=Math.max(0,client.getVarbitValue(VarbitID.SLAYER_POINTS));
        int streak=Math.max(0,client.getVarbitValue(VarbitID.SLAYER_TASKS_COMPLETED));
        int amount=Math.max(0,client.getVarpValue(VarPlayerID.SLAYER_COUNT));
        int initialAmount=Math.max(0,client.getVarpValue(VarPlayerID.SLAYER_COUNT_ORIGINAL));
        int taskId=Math.max(0,client.getVarpValue(VarPlayerID.SLAYER_TARGET));
        int areaId=Math.max(0,client.getVarpValue(VarPlayerID.SLAYER_AREA));

        String taskName="";
        String taskLocation="";
        String taskSource="none";

        try
        {
            if(amount>0 && taskId>0)
            {
                int taskDBRow=-1;
                if(taskId==98)
                {
                    var bossRows=client.getDBRowsByValue(
                        DBTableID.SlayerTaskSublist.ID,
                        DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
                        0,
                        client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
                    if(!bossRows.isEmpty())
                    {
                        taskDBRow=(Integer)client.getDBTableField(
                            bossRows.get(0),
                            DBTableID.SlayerTaskSublist.COL_TASK,
                            0)[0];
                    }
                }
                else
                {
                    var taskRows=client.getDBRowsByValue(
                        DBTableID.SlayerTask.ID,
                        DBTableID.SlayerTask.COL_ID,
                        0,
                        taskId);
                    if(!taskRows.isEmpty()) taskDBRow=taskRows.get(0);
                }

                if(taskDBRow>=0)
                {
                    Object[] nameField=client.getDBTableField(
                        taskDBRow,
                        DBTableID.SlayerTask.COL_NAME_UPPERCASE,
                        0);
                    if(nameField!=null && nameField.length>0 && nameField[0] instanceof String)
                    {
                        taskName=(String)nameField[0];
                        taskSource="game-db";
                    }
                }

                if(areaId>0)
                {
                    var areaRows=client.getDBRowsByValue(
                        DBTableID.SlayerArea.ID,
                        DBTableID.SlayerArea.COL_AREA_ID,
                        0,
                        areaId);
                    if(!areaRows.isEmpty())
                    {
                        Object[] locationField=client.getDBTableField(
                            areaRows.get(0),
                            DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER,
                            0);
                        if(locationField!=null && locationField.length>0 && locationField[0] instanceof String)
                        {
                            taskLocation=(String)locationField[0];
                        }
                    }
                }

                if(client.getVarbitValue(VarbitID.SLAYER_MODIFIER_ID)==2)
                {
                    boolean negative=client.getVarbitValue(VarbitID.SLAYER_MODIFIER_NEGATIVE)==1;
                    int modifier=client.getVarbitValue(VarbitID.SLAYER_MODIFIER_VALUE);
                    initialAmount+=negative?-modifier:modifier;
                    initialAmount=Math.max(0,initialAmount);
                }
            }
        }
        catch(Exception ex)
        {
            log.fine("Direct Slayer task decode failed: "+ex.getMessage());
        }

        // RuneLite Slayer profile is only a fallback. OneMan no longer depends on
        // the Slayer plugin being enabled/initialized to know the active assignment.
        String cachedTask=configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME,SlayerConfig.TASK_NAME_KEY);
        String cachedAmount=configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME,SlayerConfig.AMOUNT_KEY);
        String cachedInitial=configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME,SlayerConfig.INIT_AMOUNT_KEY);
        String cachedLocation=configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME,SlayerConfig.TASK_LOC_KEY);

        // The live count is authoritative. A zero must not resurrect a profile task.
        // Only a profile count and initial amount matching both live values may
        // supply a missing label, and that label remains unverified until chat/DB confirmation.
        boolean profileMatches=amount>0 && taskId>0 && safeInt(cachedAmount,-1)==amount
            && initialAmount>0 && safeInt(cachedInitial,-1)==initialAmount;
        if(taskName.isEmpty() && profileMatches && cachedTask!=null && !cachedTask.trim().isEmpty())
        {
            taskName=cachedTask.trim(); taskSource="runelite-profile-unverified";
            if(taskLocation.isEmpty() && cachedLocation!=null) taskLocation=cachedLocation;
        }
        if(amount==0) { taskName=""; taskLocation=""; initialAmount=0; taskSource="game-no-task"; }

        o.addProperty("level",level);
        o.addProperty("points",points);
        o.addProperty("streak",streak);
        o.addProperty("taskCountVarp",Math.max(0,client.getVarpValue(VarPlayerID.SLAYER_COUNT)));
        o.addProperty("taskTargetVarp",taskId);
        o.addProperty("taskName",taskName);
        o.addProperty("amount",amount);
        o.addProperty("initialAmount",initialAmount);
        o.addProperty("location",taskLocation);
        o.addProperty("taskSource",taskSource);
        boolean confirmed=taskSource.equals("game-db") || taskSource.equals("game-no-task");
        o.addProperty("schemaVersion",2);
        o.addProperty("observedEpochMs",System.currentTimeMillis());
        o.addProperty("taskSignalValid",confirmed);
        o.addProperty("taskStatus",amount==0?"none":confirmed?"active":"unknown");
        o.addProperty("lastTaskSeenEpochMs",confirmed?System.currentTimeMillis():0L);
        o.addProperty("rawTaskCount",Math.max(0,client.getVarpValue(VarPlayerID.SLAYER_COUNT)));
        o.addProperty("rawTaskTarget",taskId);
        o.addProperty("rawTaskArea",areaId);

        JsonArray unlocks=new JsonArray();
        unlocks.add(client.getVarpValue(VarPlayerID.SLAYER_REWARDS_UNLOCKS));
        unlocks.add(client.getVarpValue(VarPlayerID.SLAYER_REWARDS_UNLOCKS1));
        unlocks.add(client.getVarpValue(VarPlayerID.SLAYER_REWARDS_UNLOCKS2));
        o.add("unlockBits",unlocks);
        JsonObject rewardUnlocks=new JsonObject();
        rewardUnlocks.addProperty("biggerAndBadder",client.getVarbitValue(VarbitID.SLAYER_UNLOCK_SUPERIORMOBS)>0);
        rewardUnlocks.addProperty("biggerAndBadderEnabled",client.getVarbitValue(VarbitID.SLAYER_TOGGLEOFF_SUPERIORMOBS)==0);
        rewardUnlocks.addProperty("slugSalter",client.getVarbitValue(VarbitID.SLAYER_AUTOKILL_ROCKSLUGS)>0);
        rewardUnlocks.addProperty("reptileFreezer",client.getVarbitValue(VarbitID.SLAYER_AUTOKILL_DESERTLIZARDS)>0);
        o.add("rewardUnlocks",rewardUnlocks);

        JsonArray blocked=new JsonArray();
        int[] blockedVarps={
            VarPlayerID.SLAYER_REWARDS_BLOCKED,VarPlayerID.SLAYER_REWARDS_BLOCKED_2,VarPlayerID.SLAYER_REWARDS_BLOCKED_3,
            VarPlayerID.SLAYER_REWARDS_BLOCKED_4,VarPlayerID.SLAYER_REWARDS_BLOCKED_5,VarPlayerID.SLAYER_REWARDS_BLOCKED_6,
            VarPlayerID.SLAYER_REWARDS_BLOCKED_7,VarPlayerID.SLAYER_REWARDS_BLOCKED_8,VarPlayerID.SLAYER_REWARDS_BLOCKED_9,
            VarPlayerID.SLAYER_REWARDS_BLOCKED_10,VarPlayerID.SLAYER_REWARDS_BLOCKED_11,VarPlayerID.SLAYER_REWARDS_BLOCKED_12,
            VarPlayerID.SLAYER_REWARDS_BLOCKED_13,VarPlayerID.SLAYER_REWARDS_BLOCKED_14
        };
        for(int v:blockedVarps) blocked.add(client.getVarpValue(v));
        o.add("blockedRaw",blocked);
        return o;
    }

    private static int safeInt(String value,int fallback)
    {
        try{return value==null?fallback:Integer.parseInt(value.trim());}
        catch(Exception ex){return fallback;}
    }

    private static String cleanMessage(String s)
    {
        if(s==null)return"";
        String clean=Text.removeTags(s);
        clean=clean.replaceAll("@[^@]+@","");
        return clean.trim();
    }

}
