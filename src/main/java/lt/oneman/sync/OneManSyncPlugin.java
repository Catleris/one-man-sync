package lt.oneman.sync;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.EnumID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.cluescrolls.clues.emote.STASHUnit;
import net.runelite.client.plugins.poh.PohIcons;
import net.runelite.client.plugins.slayer.SlayerConfig;
import net.runelite.client.plugins.timetracking.TimeTrackingConfig;
import net.runelite.client.util.Text;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@PluginDescriptor(
    name = "OneMan Sync",
    description = "Local Slayer preparation, maps and Prayer resources, with optional progression sync to oneman.lt",
    tags = {"progress", "quests", "ironman", "sync", "achievements", "collection", "log", "boss", "kc", "pets", "loot", "clue", "stash", "slayer", "storage", "poh"},
    internalName = "one-man-sync"
)
@net.runelite.client.plugins.PluginDependency(net.runelite.client.plugins.slayer.SlayerPlugin.class)
public class OneManSyncPlugin extends Plugin
{
    private static final Logger log = Logger.getLogger(OneManSyncPlugin.class.getName());
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int TICKS_PER_MINUTE = 100;
    private static final String SYNC_ENDPOINT = "https://oneman.lt/runelite_sync.php";

    private static final Pattern COLLECTION_LOG_ITEM_PATTERN =
        Pattern.compile(".*New item added to your collection log:\\s*(?<item>.+?)[.]?$", Pattern.CASE_INSENSITIVE);

    private static final Pattern CA_MESSAGE_PATTERN =
        Pattern.compile("CA_ID:(?<id>\\d+)\\|Congratulations, you've completed an? (?<tier>\\w+) combat task:\\s*(?<task>.+?)(?:\\s*\\(\\d+ points?\\))?[.]?$", Pattern.CASE_INSENSITIVE);

    private static final Pattern NEW_PB_PATTERN =
        Pattern.compile(".*\\(new personal best\\).*", Pattern.CASE_INSENSITIVE);

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

    private static final String[] PET_MESSAGES = {
        "You have a funny feeling like you're being followed",
        "You feel something weird sneaking into your backpack",
        "You have a funny feeling like you would have been followed"
    };

    private static final int[] CA_VARPS = {
        VarPlayerID.CA_TASK_COMPLETED_0, VarPlayerID.CA_TASK_COMPLETED_1,
        VarPlayerID.CA_TASK_COMPLETED_2, VarPlayerID.CA_TASK_COMPLETED_3,
        VarPlayerID.CA_TASK_COMPLETED_4, VarPlayerID.CA_TASK_COMPLETED_5,
        VarPlayerID.CA_TASK_COMPLETED_6, VarPlayerID.CA_TASK_COMPLETED_7,
        VarPlayerID.CA_TASK_COMPLETED_8, VarPlayerID.CA_TASK_COMPLETED_9,
        VarPlayerID.CA_TASK_COMPLETED_10, VarPlayerID.CA_TASK_COMPLETED_11,
        VarPlayerID.CA_TASK_COMPLETED_12, VarPlayerID.CA_TASK_COMPLETED_13,
        VarPlayerID.CA_TASK_COMPLETED_14, VarPlayerID.CA_TASK_COMPLETED_15,
        VarPlayerID.CA_TASK_COMPLETED_16, VarPlayerID.CA_TASK_COMPLETED_17,
        VarPlayerID.CA_TASK_COMPLETED_18, VarPlayerID.CA_TASK_COMPLETED_19
    };

    private static final Map<String, Integer> BOSS_VARPS = loadBossVarps();

    @Inject private OneManSlayerHelper slayerHelper;
    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    @Inject private OkHttpClient http;
    @Inject private Gson gson;
    @Inject private ItemManager itemManager;
    @Inject private ConfigManager configManager;
    @Inject private ClientThread clientThread;

    private int loginCountdown = -1;
    private int lastFullSyncTick = -100000;
    private int lastKnownTotalLevel = -1;
    private boolean progressDirty = false;
    private volatile boolean requestInFlight = false;

    private final Map<String, CollectionEntry> observedCollection = new ConcurrentHashMap<>();
    private final Set<String> pendingNewCollection = ConcurrentHashMap.newKeySet();
    private final Map<Integer, CaMeta> pendingCaMeta = new ConcurrentHashMap<>();
    private final Map<String, LootEventData> pendingLootEvents = new ConcurrentHashMap<>();
    private final Map<String, PetEventData> pendingPetEvents = new ConcurrentHashMap<>();
    private final Map<String, PersonalBestData> pendingPbEvents = new ConcurrentHashMap<>();
    private final AtomicLong eventSequence = new AtomicLong();

    private final Map<Integer, BankItemData> latestBankItems = new ConcurrentHashMap<>();
    private volatile boolean bankSnapshotComplete = false;
    private volatile long bankSnapshotEpochMs = 0L;

    private final Map<String,Map<Integer,StorageItemData>> latestStorageItems = new ConcurrentHashMap<>();
    private final Map<String,Long> storageSnapshotEpochMs = new ConcurrentHashMap<>();
    private final Map<String,String> storageLabels = new ConcurrentHashMap<>();
    private final Set<String> storageComplete = ConcurrentHashMap.newKeySet();
    private final Set<String> latestPohFeatures = ConcurrentHashMap.newKeySet();
    private volatile long pohSnapshotEpochMs = 0L;
    private volatile JsonObject slayerStateCache = null;
    private volatile int lastSlayerRefreshTick = -100000;

    private volatile long pendingPetSignalAt = 0L;
    private volatile int pendingPetSignalTick = -1;
    private volatile String lastLootSource = "";
    private volatile long lastLootAt = 0L;

    @Provides
    OneManSyncConfig provideConfig(ConfigManager configManager)
    {
        OneManConfigMigration.migrate(configManager);
        return configManager.getConfig(OneManSyncConfig.class);
    }

    @Override
    protected void startUp()
    {
        slayerHelper.startUp();
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            loginCountdown = 10;
            clientThread.invokeLater(this::refreshSlayerStateCache);
        }
    }

    @Override
    protected void shutDown()
    {
        slayerHelper.shutDown();
        loginCountdown=-1;
        progressDirty=false;
        requestInFlight=false;
        lastKnownTotalLevel=-1;
        observedCollection.clear();
        pendingNewCollection.clear();
        pendingCaMeta.clear();
        pendingLootEvents.clear();
        pendingPetEvents.clear();
        pendingPbEvents.clear();
        latestBankItems.clear();
        bankSnapshotComplete=false;
        bankSnapshotEpochMs=0L;
        latestStorageItems.clear();
        storageSnapshotEpochMs.clear();
        storageLabels.clear();
        storageComplete.clear();
        latestPohFeatures.clear();
        pohSnapshotEpochMs=0L;
        slayerStateCache=null;
        lastSlayerRefreshTick=-100000;
        pendingPetSignalAt=0L;
        pendingPetSignalTick=-1;
        lastLootSource="";
        lastLootAt=0L;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        slayerHelper.onGameStateChanged(event);
        if(event.getGameState()==GameState.LOGGED_IN){
            loginCountdown=10;
            progressDirty=true;
            clientThread.invokeLater(this::refreshSlayerStateCache);
        } else if(event.getGameState()==GameState.LOGIN_SCREEN){
            loginCountdown=-1;
            progressDirty=false;
            lastKnownTotalLevel=-1;
            slayerStateCache=null;
            lastSlayerRefreshTick=-100000;
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        slayerHelper.onStatChanged(event);
        if(!ready()) return;
        int total=calculateTotalLevel();
        if(lastKnownTotalLevel>0 && total>lastKnownTotalLevel) syncSoon();
        lastKnownTotalLevel=total;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if(config.enabled() && client.getGameState()==GameState.LOGGED_IN && isSlayerSignal(event))
        {
            // Match RuneLite's Slayer plugin behaviour: read the assignment after
            // the var/DB update has settled on the client thread.
            clientThread.invokeLater(() -> {
                refreshSlayerStateCache();
                syncSoon();
            });
        }
        if(ready()) progressDirty=true;
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

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event)
    {
        slayerHelper.onNpcLootReceived(event);
        if(!ready() || event.getNpc()==null) return;

        String source=event.getNpc().getName();
        if(source==null || source.trim().isEmpty()) return;
        source=source.trim();
        long now=System.currentTimeMillis();
        lastLootSource=source;
        lastLootAt=now;

        // Keep raw loot only for tracked bosses/activities so the website DB does not explode.
        if(!isTrackedBoss(source)) return;

        LootEventData loot=new LootEventData(makeEventKey("loot",source,now),source,now);
        for(ItemStack stack:event.getItems()){
            if(stack==null || stack.getQuantity()<=0) continue;
            String name;
            try{
                name=itemManager.getItemComposition(stack.getId()).getName();
            }catch(Exception ex){
                name="Item #"+stack.getId();
            }
            loot.items.put(stack.getId()+"|"+name,new LootItemData(stack.getId(),name,stack.getQuantity()));
        }
        if(!loot.items.isEmpty()){
            pendingLootEvents.put(loot.eventKey,loot);
            syncSoon();
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        slayerHelper.onItemContainerChanged(event);
        if(!config.enabled() || client.getGameState()!=GameState.LOGGED_IN) return;

        ItemContainer container=event.getItemContainer();
        if(container==null || container.getItems()==null) return;

        if(event.getContainerId()==InventoryID.BANK)
        {
            Map<Integer,BankItemData> snapshot=new LinkedHashMap<>();
            for(Item item:container.getItems())
            {
                if(item==null || item.getId()<=0 || item.getQuantity()<=0) continue;
                String name=itemName(item.getId());
                if(name.isEmpty()) continue;
                BankItemData old=snapshot.get(item.getId());
                int qty=item.getQuantity()+(old!=null?old.quantity:0);
                snapshot.put(item.getId(),new BankItemData(item.getId(),name,qty));
            }

            latestBankItems.clear();
            latestBankItems.putAll(snapshot);
            bankSnapshotComplete=true;
            bankSnapshotEpochMs=System.currentTimeMillis();
            syncSoon();
            return;
        }

        String key=storageKeyForContainer(event.getContainerId());
        if(key!=null)
        {
            captureStorage(key,storageLabel(key),container);
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        capturePohObject(event.getGameObject().getId());
    }

    @Subscribe
    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
    {
        capturePohObject(event.getDecorativeObject().getId());
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if(!config.enabled() || client.getGameState()!=GameState.LOGGED_IN) return;
        ChatMessageType type=event.getType();
        boolean normalGameMessage=type==ChatMessageType.GAMEMESSAGE || type==ChatMessageType.SPAM;
        boolean slayerDialog=type==ChatMessageType.DIALOG || type==ChatMessageType.MESBOX || type==ChatMessageType.NPC_SAY;
        if(!normalGameMessage && !slayerDialog) return;

        String message=cleanMessage(event.getMessage());
        long now=System.currentTimeMillis();

        if(captureSlayerMessage(message))
        {
            syncSoon();
        }

        if(!normalGameMessage) return;

        for(String petMessage:PET_MESSAGES){
            if(message.contains(petMessage)){
                pendingPetSignalAt=now;
                pendingPetSignalTick=client.getTickCount();
                syncSoon();
                break;
            }
        }

        Matcher clog=COLLECTION_LOG_ITEM_PATTERN.matcher(message);
        if(clog.find()){
            String item=clog.group("item").trim();
            if(!item.isEmpty()){
                String key=normalize(item);
                boolean isPet=isPetWindowActive();
                String source=recentLootSource();
                CollectionEntry old=observedCollection.get(key);
                int id=old!=null?old.itemId:0;
                int qty=old!=null?old.quantity:1;
                String section=old!=null?old.section:"";
                observedCollection.put(key,new CollectionEntry(id,item,qty,section,now,isPet,source));
                pendingNewCollection.add(key);

                if(isPet){
                    queuePet(item,source,now);
                    pendingPetSignalAt=0L;
                    pendingPetSignalTick=-1;
                }
                syncSoon();
            }
        }

        Matcher ca=CA_MESSAGE_PATTERN.matcher(message);
        if(ca.find()){
            int id=Integer.parseInt(ca.group("id"));
            if(id>=0 && id<=639){
                pendingCaMeta.put(id,new CaMeta(id,ca.group("task").trim(),titleCase(ca.group("tier")),""));
                syncSoon();
            }
        }

        if(NEW_PB_PATTERN.matcher(message).matches()){
            String source=recentLootSource();
            String key=makeEventKey("pb",source,now);
            pendingPbEvents.put(key,new PersonalBestData(key,message,source,now));
            syncSoon();
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        if(!ready() || event.getScriptId()!=ScriptID.COLLECTION_DRAW_LIST) return;

        markStorageScan("collection_log_scan","Collection Log");

        Widget header=client.getWidget(InterfaceID.Collection.HEADER_TEXT);
        Widget items=client.getWidget(InterfaceID.Collection.ITEMS_CONTENTS);
        if(header==null || items==null || header.getChildren()==null || items.getChildren()==null) return;

        Widget titleWidget=header.getChild(0);
        String section=titleWidget!=null?cleanMessage(titleWidget.getText()):"Collection Log";
        boolean changed=false;

        for(Widget child:items.getChildren()){
            if(child==null || child.getOpacity()!=0) continue;
            int itemId=child.getItemId();
            if(itemId<=0) continue;

            String name;
            try{name=itemManager.getItemComposition(itemId).getName();}
            catch(Exception ex){continue;}
            if(name==null || name.isEmpty() || "null".equalsIgnoreCase(name)) continue;

            int quantity=Math.max(1,child.getItemQuantity());
            String key=normalize(name);
            CollectionEntry old=observedCollection.get(key);

            if(old==null || old.itemId!=itemId || quantity>old.quantity || !section.equals(old.section)){
                long occurred=old!=null?old.occurredAt:0L; // old log backfill has no known historical timestamp
                boolean pet=old!=null && old.isPet;
                String source=old!=null?old.sourceName:"";
                observedCollection.put(key,new CollectionEntry(itemId,name,quantity,section,occurred,pet,source));
                changed=true;
            }
        }

        if(changed) syncSoon();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        slayerHelper.onGameTick(event);
        if(!ready()) return;

        // Pet message can precede the Collection Log message. If a follower appears,
        // record its real NPC name as a permanent pet memory.
        if(isPetWindowActive()){
            NPC follower=client.getFollower();
            if(follower!=null && follower.getName()!=null && !follower.getName().trim().isEmpty()){
                queuePet(follower.getName().trim(),recentLootSource(),pendingPetSignalAt>0?pendingPetSignalAt:System.currentTimeMillis());
                pendingPetSignalAt=0L;
                pendingPetSignalTick=-1;
            }
        } else if(pendingPetSignalTick>=0 && client.getTickCount()-pendingPetSignalTick>25){
            pendingPetSignalAt=0L;
            pendingPetSignalTick=-1;
        }

        if(loginCountdown>0){
            loginCountdown--;
            if(loginCountdown==0){
                syncSnapshot();
                return;
            }
        }

        int tick=client.getTickCount();

        // Periodic fallback: catches assignments when this plugin was enabled
        // after login or when RuneLite did not emit a fresh Slayer var event.
        if(tick-lastSlayerRefreshTick>=50)
        {
            refreshSlayerStateCache();
        }

        int intervalTicks=Math.max(2,config.intervalMinutes())*TICKS_PER_MINUTE;
        if(tick-lastFullSyncTick>=intervalTicks) syncSnapshot();
        else if(progressDirty && tick-lastFullSyncTick>=200) syncSnapshot();
    }

    private void queuePet(String petName,String source,long occurredAt)
    {
        if(petName==null || petName.trim().isEmpty()) return;
        String normalized=normalize(petName);
        int kc=0;
        Integer varp=BOSS_VARPS.get(source);
        if(varp!=null) kc=Math.max(0,client.getVarpValue(varp));
        String key="pet-"+normalized.replace(' ','_');
        pendingPetEvents.putIfAbsent(key,new PetEventData(key,petName.trim(),source,kc,occurredAt));
    }

    private boolean isPetWindowActive()
    {
        return pendingPetSignalTick>=0 && client.getTickCount()-pendingPetSignalTick<=25;
    }

    private String recentLootSource()
    {
        return System.currentTimeMillis()-lastLootAt<=30000?lastLootSource:"";
    }

    private boolean isTrackedBoss(String source)
    {
        if(BOSS_VARPS.containsKey(source)) return true;
        String noThe=source.startsWith("The ")?source.substring(4):source;
        for(String boss:BOSS_VARPS.keySet()){
            String b=boss.startsWith("The ")?boss.substring(4):boss;
            if(b.equalsIgnoreCase(noThe)) return true;
        }
        return false;
    }

    private String makeEventKey(String prefix,String source,long time)
    {
        return prefix+"-"+time+"-"+client.getTickCount()+"-"+eventSequence.incrementAndGet()+"-"+normalize(source).replace(' ','_');
    }

    private void syncSoon()
    {
        progressDirty=true;
        loginCountdown=Math.max(loginCountdown,8);
    }

    private boolean ready()
    {
        return config.enabled()
            && client.getGameState()==GameState.LOGGED_IN
            && client.getLocalPlayer()!=null
            && !config.syncKey().trim().isEmpty()
            && !requestInFlight;
    }

    private int calculateTotalLevel()
    {
        int total=0;
        for(Skill skill:Skill.values()) total+=client.getRealSkillLevel(skill);
        return total;
    }

    private void syncSnapshot()
    {
        if(!ready()) return;

        Set<String> sentNewCollection=new HashSet<>(pendingNewCollection);
        Map<Integer,CaMeta> sentCaMeta=new HashMap<>(pendingCaMeta);
        Map<String,LootEventData> sentLoot=new HashMap<>(pendingLootEvents);
        Map<String,PetEventData> sentPets=new HashMap<>(pendingPetEvents);
        Map<String,PersonalBestData> sentPb=new HashMap<>(pendingPbEvents);

        JsonObject payload=buildSnapshot(sentNewCollection,sentCaMeta,sentLoot,sentPets,sentPb);
        if(payload==null) return;

        RequestBody body=RequestBody.create(JSON,gson.toJson(payload));
        Request request=new Request.Builder()
            .url(SYNC_ENDPOINT)
            .header("Authorization","Bearer "+config.syncKey().trim())
            .header("Accept","application/json")
            .post(body)
            .build();

        requestInFlight=true;
        lastFullSyncTick=client.getTickCount();
        progressDirty=false;

        http.newCall(request).enqueue(new Callback(){
            @Override public void onFailure(Call call,IOException e){
                requestInFlight=false;
                progressDirty=true;
                log.warning("OneMan sync failed: "+e.getMessage());
            }

            @Override public void onResponse(Call call,Response response)throws IOException{
                try(Response r=response){
                    String text=r.body()!=null?r.body().string():"";
                    if(!r.isSuccessful()){
                        progressDirty=true;
                        log.warning("OneMan sync HTTP "+r.code()+": "+text);
                    }else{
                        pendingNewCollection.removeAll(sentNewCollection);
                        for(Integer id:sentCaMeta.keySet()) pendingCaMeta.remove(id,sentCaMeta.get(id));
                        for(String k:sentLoot.keySet()) pendingLootEvents.remove(k,sentLoot.get(k));
                        for(String k:sentPets.keySet()) pendingPetEvents.remove(k,sentPets.get(k));
                        for(String k:sentPb.keySet()) pendingPbEvents.remove(k,sentPb.get(k));
                        log.fine("OneMan sync OK: "+text);
                    }
                }finally{
                    requestInFlight=false;
                }
            }
        });
    }

    private JsonObject buildSnapshot(Set<String> newCollectionSnapshot,Map<Integer,CaMeta> caMetaSnapshot,
        Map<String,LootEventData> lootSnapshot,Map<String,PetEventData> petSnapshot,Map<String,PersonalBestData> pbSnapshot)
    {
        String player=client.getLocalPlayer()!=null?client.getLocalPlayer().getName():null;
        if(player==null || player.trim().isEmpty()) return null;

        JsonObject root=new JsonObject();
        root.addProperty("player",player);
        root.addProperty("clientRevision",client.getRevision());
        root.addProperty("totalLevel",calculateTotalLevel());
        root.addProperty("totalXp",client.getOverallExperience());
        root.addProperty("snapshotEpochMs",System.currentTimeMillis());

        JsonArray skills=new JsonArray();
        for(Skill skill:Skill.values()){
            JsonObject s=new JsonObject();
            s.addProperty("name",skill.getName());
            s.addProperty("level",client.getRealSkillLevel(skill));
            s.addProperty("xp",client.getSkillExperience(skill));
            skills.add(s);
        }
        root.add("skills",skills);

        JsonArray quests=new JsonArray();
        for(Quest quest:Quest.values()){
            try{
                QuestState state=quest.getState(client);
                JsonObject q=new JsonObject();
                q.addProperty("key",quest.name());
                q.addProperty("name",quest.getName());
                q.addProperty("state",state.name());
                quests.add(q);
            }catch(Exception ex){
                log.fine("Quest state failed for "+quest.name()+": "+ex.getMessage());
            }
        }
        root.add("quests",quests);

        root.add("diaries",buildDiaries());
        root.add("combatAchievements",buildCombatAchievementTiers());
        root.add("combatAchievementTasks",buildCompletedCombatAchievementTasks());
        root.add("combatAchievementMeta",buildCombatAchievementMeta(caMetaSnapshot));
        root.add("bossKillCounts",buildBossKillCounts());
        root.add("collectionLogEntries",buildCollectionLogEntries(newCollectionSnapshot));
        root.add("lootEvents",buildLootEvents(lootSnapshot,newCollectionSnapshot));
        root.add("petEvents",buildPetEvents(petSnapshot));
        root.add("personalBestEvents",buildPersonalBestEvents(pbSnapshot));
        root.add("bankItems",buildBankItems());
        root.addProperty("bankSnapshotComplete",bankSnapshotComplete);
        root.addProperty("bankSnapshotEpochMs",bankSnapshotEpochMs);
        root.add("stashUnits",buildStashUnits());
        root.addProperty("stashSnapshotEpochMs",System.currentTimeMillis());
        root.add("storageSnapshots",buildStorageSnapshots());
        root.add("slayer",buildSlayerState());
        root.add("poh",buildPohState());
        root.add("currencies",buildCurrencies());
        root.add("daily",buildDailyState());
        root.add("unlocks",buildUnlocks());
        return root;
    }

    private JsonArray buildDiaries()
    {
        JsonArray a=new JsonArray();
        addDiaryArea(a,"Ardougne",VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE,VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE,VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE);
        addDiaryArea(a,"Desert",VarbitID.DESERT_DIARY_EASY_COMPLETE,VarbitID.DESERT_DIARY_MEDIUM_COMPLETE,VarbitID.DESERT_DIARY_HARD_COMPLETE,VarbitID.DESERT_DIARY_ELITE_COMPLETE);
        addDiaryArea(a,"Falador",VarbitID.FALADOR_DIARY_EASY_COMPLETE,VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE,VarbitID.FALADOR_DIARY_HARD_COMPLETE,VarbitID.FALADOR_DIARY_ELITE_COMPLETE);
        addDiaryArea(a,"Fremennik",VarbitID.FREMENNIK_DIARY_EASY_COMPLETE,VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE,VarbitID.FREMENNIK_DIARY_HARD_COMPLETE,VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE);
        addDiaryArea(a,"Kandarin",VarbitID.KANDARIN_DIARY_EASY_COMPLETE,VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE,VarbitID.KANDARIN_DIARY_HARD_COMPLETE,VarbitID.KANDARIN_DIARY_ELITE_COMPLETE);
        addDiaryArea(a,"Karamja",VarbitID.ATJUN_EASY_DONE,VarbitID.ATJUN_MED_DONE,VarbitID.ATJUN_HARD_DONE,VarbitID.KARAMJA_DIARY_ELITE_COMPLETE);
        addDiaryArea(a,"Kourend & Kebos",VarbitID.KOUREND_DIARY_EASY_COMPLETE,VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE,VarbitID.KOUREND_DIARY_HARD_COMPLETE,VarbitID.KOUREND_DIARY_ELITE_COMPLETE);
        addDiaryArea(a,"Lumbridge & Draynor",VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE,VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE,VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE,VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE);
        addDiaryArea(a,"Morytania",VarbitID.MORYTANIA_DIARY_EASY_COMPLETE,VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE,VarbitID.MORYTANIA_DIARY_HARD_COMPLETE,VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE);
        addDiaryArea(a,"Varrock",VarbitID.VARROCK_DIARY_EASY_COMPLETE,VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,VarbitID.VARROCK_DIARY_HARD_COMPLETE,VarbitID.VARROCK_DIARY_ELITE_COMPLETE);
        addDiaryArea(a,"Western Provinces",VarbitID.WESTERN_DIARY_EASY_COMPLETE,VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE,VarbitID.WESTERN_DIARY_HARD_COMPLETE,VarbitID.WESTERN_DIARY_ELITE_COMPLETE);
        addDiaryArea(a,"Wilderness",VarbitID.WILDERNESS_DIARY_EASY_COMPLETE,VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE,VarbitID.WILDERNESS_DIARY_HARD_COMPLETE,VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE);
        return a;
    }

    private void addDiaryArea(JsonArray out,String area,int easy,int medium,int hard,int elite)
    {
        addDiary(out,area,"Easy",easy);addDiary(out,area,"Medium",medium);addDiary(out,area,"Hard",hard);addDiary(out,area,"Elite",elite);
    }

    private void addDiary(JsonArray out,String area,String tier,int varbit)
    {
        int value=client.getVarbitValue(varbit);
        JsonObject d=new JsonObject();
        d.addProperty("area",area);d.addProperty("tier",tier);d.addProperty("value",value);d.addProperty("completed",value==1);out.add(d);
    }

    private JsonArray buildCombatAchievementTiers()
    {
        JsonArray out=new JsonArray();
        addCaTier(out,"Easy",VarbitID.CA_TIER_STATUS_EASY,VarbitID.CA_TOTAL_TASKS_COMPLETED_EASY);
        addCaTier(out,"Medium",VarbitID.CA_TIER_STATUS_MEDIUM,VarbitID.CA_TOTAL_TASKS_COMPLETED_MEDIUM);
        addCaTier(out,"Hard",VarbitID.CA_TIER_STATUS_HARD,VarbitID.CA_TOTAL_TASKS_COMPLETED_HARD);
        addCaTier(out,"Elite",VarbitID.CA_TIER_STATUS_ELITE,VarbitID.CA_TOTAL_TASKS_COMPLETED_ELITE);
        addCaTier(out,"Master",VarbitID.CA_TIER_STATUS_MASTER,VarbitID.CA_TOTAL_TASKS_COMPLETED_MASTER);
        addCaTier(out,"Grandmaster",VarbitID.CA_TIER_STATUS_GRANDMASTER,VarbitID.CA_TOTAL_TASKS_COMPLETED_GRANDMASTER);
        return out;
    }

    private void addCaTier(JsonArray out,String tier,int statusVarbit,int countVarbit)
    {
        int status=client.getVarbitValue(statusVarbit);
        JsonObject c=new JsonObject();
        c.addProperty("tier",tier);c.addProperty("status",status);
        c.addProperty("completedTasks",client.getVarbitValue(countVarbit));c.addProperty("completed",status==2);out.add(c);
    }

    private JsonArray buildCompletedCombatAchievementTasks()
    {
        JsonArray out=new JsonArray();
        for(int group=0;group<CA_VARPS.length;group++){
            int value=client.getVarpValue(CA_VARPS[group]);
            for(int bit=0;bit<32;bit++){
                if(((value>>>bit)&1)==1){
                    JsonObject o=new JsonObject();o.addProperty("id",group*32+bit);out.add(o);
                }
            }
        }
        return out;
    }

    private JsonArray buildCombatAchievementMeta(Map<Integer,CaMeta> snapshot)
    {
        JsonArray out=new JsonArray();
        for(CaMeta m:snapshot.values()){
            JsonObject o=new JsonObject();o.addProperty("id",m.id);o.addProperty("name",m.name);o.addProperty("tier",m.tier);o.addProperty("monster",m.monster);out.add(o);
        }
        return out;
    }

    private JsonArray buildBossKillCounts()
    {
        JsonArray out=new JsonArray();
        for(Map.Entry<String,Integer> e:BOSS_VARPS.entrySet()){
            JsonObject o=new JsonObject();o.addProperty("name",e.getKey());o.addProperty("kc",Math.max(0,client.getVarpValue(e.getValue())));out.add(o);
        }
        return out;
    }

    private JsonArray buildCollectionLogEntries(Set<String> newSnapshot)
    {
        JsonArray out=new JsonArray();
        for(Map.Entry<String,CollectionEntry> e:observedCollection.entrySet()){
            CollectionEntry v=e.getValue();
            JsonObject o=new JsonObject();
            o.addProperty("itemId",v.itemId);o.addProperty("itemName",v.name);o.addProperty("quantity",v.quantity);o.addProperty("section",v.section);
            boolean isNew=newSnapshot.contains(e.getKey());
            o.addProperty("source",isNew?"new":"scan");
            o.addProperty("occurredAt",isNew?v.occurredAt:0L);
            o.addProperty("isPet",isNew&&v.isPet);
            o.addProperty("sourceName",v.sourceName);
            out.add(o);
        }
        return out;
    }

    private JsonArray buildLootEvents(Map<String,LootEventData> snapshot,Set<String> newCollectionSnapshot)
    {
        JsonArray out=new JsonArray();
        Set<String> collectionNames=new HashSet<>(newCollectionSnapshot);
        for(LootEventData l:snapshot.values()){
            JsonObject o=new JsonObject();
            o.addProperty("eventKey",l.eventKey);o.addProperty("sourceName",l.sourceName);o.addProperty("occurredAt",l.occurredAt);
            JsonArray items=new JsonArray();
            for(LootItemData i:l.items.values()){
                JsonObject it=new JsonObject();
                it.addProperty("id",i.id);it.addProperty("name",i.name);it.addProperty("quantity",i.quantity);
                it.addProperty("collectionUnlock",collectionNames.contains(normalize(i.name)));
                items.add(it);
            }
            o.add("items",items);out.add(o);
        }
        return out;
    }

    private JsonArray buildPetEvents(Map<String,PetEventData> snapshot)
    {
        JsonArray out=new JsonArray();
        for(PetEventData p:snapshot.values()){
            JsonObject o=new JsonObject();
            o.addProperty("petName",p.petName);o.addProperty("sourceName",p.sourceName);o.addProperty("bossKc",p.bossKc);o.addProperty("occurredAt",p.occurredAt);out.add(o);
        }
        return out;
    }

    private JsonArray buildPersonalBestEvents(Map<String,PersonalBestData> snapshot)
    {
        JsonArray out=new JsonArray();
        for(PersonalBestData p:snapshot.values()){
            JsonObject o=new JsonObject();
            o.addProperty("message",p.message);o.addProperty("sourceName",p.sourceName);o.addProperty("occurredAt",p.occurredAt);out.add(o);
        }
        return out;
    }

    private String itemName(int itemId)
    {
        try
        {
            String name=itemManager.getItemComposition(itemId).getName();
            if(name==null || name.trim().isEmpty() || "null".equalsIgnoreCase(name)) return "";
            return name.trim();
        }
        catch(Exception ex)
        {
            return "Item #"+itemId;
        }
    }

    private String storageKeyForContainer(int id)
    {
        switch(id)
        {
            case InventoryID.SEED_VAULT:return "seed_vault";
            case InventoryID.LOOTING_BAG:return "looting_bag";
            case InventoryID.INV_GROUP_TEMP:return "group_storage";
            case InventoryID.POH_COSTUME_ROOM_MAGIC_WARDROBE_INV:return "poh_magic_wardrobe";
            case InventoryID.POH_COSTUME_ROOM_ARMOUR_INV:return "poh_armour_case";
            case InventoryID.POH_COSTUME_ROOM_AME_INV:return "poh_fancy_dress";
            case InventoryID.POH_COSTUME_ROOM_TREASURE_TRAIL_0_INV:return "poh_treasure_chest_0";
            case InventoryID.POH_COSTUME_ROOM_TREASURE_TRAIL_1_INV:return "poh_treasure_chest_1";
            case InventoryID.POH_COSTUME_ROOM_TREASURE_TRAIL_1A_INV:return "poh_treasure_chest_1a";
            case InventoryID.POH_COSTUME_ROOM_TREASURE_TRAIL_2_INV:return "poh_treasure_chest_2";
            case InventoryID.POH_COSTUME_ROOM_TREASURE_TRAIL_2A_INV:return "poh_treasure_chest_2a";
            case InventoryID.POH_COSTUME_ROOM_TREASURE_TRAIL_3_INV:return "poh_treasure_chest_3";
            case InventoryID.POH_COSTUME_ROOM_TREASURE_TRAIL_3A_INV:return "poh_treasure_chest_3a";
            case InventoryID.POH_COSTUME_ROOM_CAPES_INV:return "poh_cape_rack";
            case InventoryID.POH_COSTUME_ROOM_CAPES_INV_PAGE2:return "poh_cape_rack_2";
            case InventoryID.POH_COSTUME_ROOM_HOLIDAY_ITEMS_INV:return "poh_toy_box";
            default:return null;
        }
    }

    private String storageLabel(String key)
    {
        switch(key)
        {
            case "seed_vault":return "Seed Vault";
            case "looting_bag":return "Looting Bag";
            case "group_storage":return "Group Storage";
            case "poh_magic_wardrobe":return "POH Magic Wardrobe";
            case "poh_armour_case":return "POH Armour Case";
            case "poh_fancy_dress":return "POH Fancy Dress Box";
            case "poh_cape_rack":case "poh_cape_rack_2":return "POH Cape Rack";
            case "poh_toy_box":return "POH Toy Box";
            default:return key.startsWith("poh_treasure_chest")?"POH Treasure Chest":key;
        }
    }

    private void captureStorage(String key,String label,ItemContainer container)
    {
        Map<Integer,StorageItemData> snapshot=new LinkedHashMap<>();
        for(Item item:container.getItems())
        {
            if(item==null || item.getId()<=0 || item.getQuantity()<=0) continue;
            String name=itemName(item.getId());
            if(name.isEmpty()) continue;
            StorageItemData old=snapshot.get(item.getId());
            int qty=item.getQuantity()+(old!=null?old.quantity:0);
            snapshot.put(item.getId(),new StorageItemData(item.getId(),name,qty));
        }
        latestStorageItems.put(key,snapshot);
        storageLabels.put(key,label);
        storageSnapshotEpochMs.put(key,System.currentTimeMillis());
        storageComplete.add(key);
        syncSoon();
    }

    private void capturePohObject(int objectId)
    {
        if(!config.enabled() || client.getGameState()!=GameState.LOGGED_IN) return;
        PohIcons icon=PohIcons.getIcon(objectId);
        if(icon==null) return;
        String feature=pohFeatureKey(objectId,icon);
        if(latestPohFeatures.add(feature))
        {
            pohSnapshotEpochMs=System.currentTimeMillis();
            syncSoon();
        }
    }

    private String pohFeatureKey(int objectId,PohIcons icon)
    {
        if(objectId==ObjectID.POH_FAIRY_RING) return "FAIRY_RING";
        if(objectId==ObjectID.POH_SPIRIT_TREE) return "SPIRIT_TREE";
        if(objectId==ObjectID.POH_SPIRIT_RING) return "SPIRIT_TREE_FAIRY_RING";
        if(objectId==ObjectID.POH_WILDERNESS_OBELISK) return "WILDERNESS_OBELISK";

        if(objectId==ObjectID.POH_POOL_RESTORATION) return "POOL_RESTORATION";
        if(objectId==ObjectID.POH_POOL_REVITALISATION) return "POOL_REVITALISATION";
        if(objectId==ObjectID.POH_POOL_REJUVENATION) return "POOL_REJUVENATION";
        if(objectId==ObjectID.POH_POOL_RECOVERY) return "POOL_RECOVERY";
        if(objectId==ObjectID.POH_POOL_REGENERATION) return "POOL_REGENERATION";

        if(objectId==ObjectID.POH_JEWELLERY_BOX_1) return "JEWELLERY_BOX_BASIC";
        if(objectId==ObjectID.POH_JEWELLERY_BOX_2) return "JEWELLERY_BOX_FANCY";
        if(objectId==ObjectID.POH_JEWELLERY_BOX_3) return "JEWELLERY_BOX_ORNATE";

        if(objectId==ObjectID.POH_ALTAR_ANCIENT) return "ALTAR_ANCIENT";
        if(objectId==ObjectID.POH_ALTAR_LUNAR) return "ALTAR_LUNAR";
        if(objectId==ObjectID.ARCHEUS_ALTAR_DARK) return "ALTAR_ARCEUUS";
        if(objectId==ObjectID.POH_ALTAR_OCCULT) return "ALTAR_OCCULT";

        if(objectId==ObjectID.POH_NEXUS_PORTAL_1) return "PORTAL_NEXUS";
        if(objectId==ObjectID.POH_NEXUS_PORTAL_2) return "PORTAL_NEXUS_GILDED";
        if(objectId==ObjectID.POH_NEXUS_PORTAL_3) return "PORTAL_NEXUS_CRYSTALLINE";

        return icon.name();
    }

    private JsonArray buildStorageSnapshots()
    {
        JsonArray out=new JsonArray();

        for(Map.Entry<String,Map<Integer,StorageItemData>> e:latestStorageItems.entrySet())
        {
            JsonObject s=new JsonObject();
            s.addProperty("key",e.getKey());
            s.addProperty("label",storageLabels.getOrDefault(e.getKey(),e.getKey()));
            s.addProperty("complete",storageComplete.contains(e.getKey()));
            s.addProperty("lastSeenEpochMs",storageSnapshotEpochMs.getOrDefault(e.getKey(),0L));
            JsonArray items=new JsonArray();
            for(StorageItemData item:e.getValue().values())
            {
                JsonObject o=new JsonObject();
                o.addProperty("id",item.id);
                o.addProperty("name",item.name);
                o.addProperty("quantity",item.quantity);
                items.add(o);
            }
            s.add("items",items);
            out.add(s);
        }

        appendLiveStorage(out,"inventory","Current Inventory",client.getItemContainer(InventoryID.INV));
        appendLiveStorage(out,"equipment","Worn Equipment",client.getItemContainer(InventoryID.WORN));

        JsonObject runePouch=new JsonObject();
        runePouch.addProperty("key","rune_pouch");
        runePouch.addProperty("label","Rune Pouch");
        runePouch.addProperty("complete",true);
        runePouch.addProperty("lastSeenEpochMs",System.currentTimeMillis());
        runePouch.add("items",buildRunePouchItems());
        out.add(runePouch);

        return out;
    }

    private void markStorageScan(String key,String label)
    {
        latestStorageItems.putIfAbsent(key,new LinkedHashMap<>());
        storageLabels.put(key,label);
        storageSnapshotEpochMs.put(key,System.currentTimeMillis());
        storageComplete.add(key);
        syncSoon();
    }

    private void appendLiveStorage(JsonArray out,String key,String label,ItemContainer container)
    {
        if(container==null || container.getItems()==null) return;
        JsonObject s=new JsonObject();
        s.addProperty("key",key);
        s.addProperty("label",label);
        s.addProperty("complete",true);
        s.addProperty("lastSeenEpochMs",System.currentTimeMillis());
        JsonArray items=new JsonArray();
        for(Item item:container.getItems())
        {
            if(item==null || item.getId()<=0 || item.getQuantity()<=0) continue;
            String name=itemName(item.getId());
            if(name.isEmpty()) continue;
            JsonObject o=new JsonObject();
            o.addProperty("id",item.getId());
            o.addProperty("name",name);
            o.addProperty("quantity",item.getQuantity());
            items.add(o);
        }
        s.add("items",items);
        out.add(s);
    }

    private JsonArray buildRunePouchItems()
    {
        int[] typeVarbits={
            VarbitID.RUNE_POUCH_TYPE_1,VarbitID.RUNE_POUCH_TYPE_2,VarbitID.RUNE_POUCH_TYPE_3,
            VarbitID.RUNE_POUCH_TYPE_4,VarbitID.RUNE_POUCH_TYPE_5,VarbitID.RUNE_POUCH_TYPE_6
        };
        int[] quantityVarbits={
            VarbitID.RUNE_POUCH_QUANTITY_1,VarbitID.RUNE_POUCH_QUANTITY_2,VarbitID.RUNE_POUCH_QUANTITY_3,
            VarbitID.RUNE_POUCH_QUANTITY_4,VarbitID.RUNE_POUCH_QUANTITY_5,VarbitID.RUNE_POUCH_QUANTITY_6
        };
        JsonArray out=new JsonArray();
        EnumComposition runeEnum=client.getEnum(EnumID.RUNEPOUCH_RUNE);
        for(int i=0;i<typeVarbits.length;i++)
        {
            int qty=client.getVarbitValue(quantityVarbits[i]);
            int type=client.getVarbitValue(typeVarbits[i]);
            if(qty<=0 || type<=0) continue;
            int itemId=runeEnum.getIntValue(type);
            JsonObject o=new JsonObject();
            o.addProperty("id",itemId);
            o.addProperty("name",itemName(itemId));
            o.addProperty("quantity",qty);
            out.add(o);
        }
        return out;
    }

    private JsonObject buildSlayerState()
    {
        // Read again at send time: cached data is only a bounded label fallback.
        refreshSlayerStateCache();
        JsonObject cached=slayerStateCache;
        return cached!=null?cached.deepCopy():readSlayerStateDirect();
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

    private JsonObject buildPohState()
    {
        JsonObject o=new JsonObject();
        JsonArray features=new JsonArray();
        for(String f:latestPohFeatures) features.add(f);
        o.add("features",features);
        o.addProperty("lastSeenEpochMs",pohSnapshotEpochMs);
        return o;
    }

    private JsonObject buildCurrencies()
    {
        JsonObject o=new JsonObject();
        o.addProperty("slayerPoints",Math.max(0,client.getVarbitValue(VarbitID.SLAYER_POINTS)));
        o.addProperty("nmzPoints",Math.max(0,client.getVarbitValue(VarbitID.NZONE_CURRENTPOINTS)));
        o.addProperty("tithePoints",Math.max(0,client.getVarbitValue(VarbitID.HOSIDIUS_TITHE_REWARDPOINTS)));
        o.addProperty("soulWarsZeal",Math.max(0,client.getVarpValue(VarPlayerID.SOUL_WARS_ZEAL_TOKENS)));
        o.addProperty("pvpArenaPoints",Math.max(0,client.getVarbitValue(VarbitID.PVPA_POINTS_CURRENCY)));
        o.addProperty("kingdomCoffer",Math.max(0,client.getVarbitValue(VarbitID.MISC_COFFERS)));
        return o;
    }

    private JsonObject buildUnlocks()
    {
        JsonObject o=new JsonObject();
        o.addProperty("rigour",client.getVarbitValue(VarbitID.PRAYER_RIGOUR_UNLOCKED)>0);
        o.addProperty("augury",client.getVarbitValue(VarbitID.PRAYER_AUGURY_UNLOCKED)>0);
        o.addProperty("preserve",client.getVarbitValue(VarbitID.PRAYER_PRESERVE_UNLOCKED)>0);
        o.addProperty("deadeye",client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED)>0);
        o.addProperty("mysticVigour",client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED)>0);
        return o;
    }

    private JsonObject buildDailyState()
    {
        JsonObject root=new JsonObject();
        long now=System.currentTimeMillis()/1000L;

        JsonArray birdhouses=new JsonArray();
        int[] birdVarps={VarPlayerID.BIRDHOUSE_TRANSMIT_A,VarPlayerID.BIRDHOUSE_TRANSMIT_B,VarPlayerID.BIRDHOUSE_TRANSMIT_C,VarPlayerID.BIRDHOUSE_TRANSMIT_D};
        String[] birdNames={"Mushroom Meadow North","Mushroom Meadow South","Verdant Valley Northeast","Verdant Valley Southwest"};
        for(int i=0;i<birdVarps.length;i++)
        {
            JsonObject b=new JsonObject();
            String stored=configManager.getRSProfileConfiguration(TimeTrackingConfig.CONFIG_GROUP,TimeTrackingConfig.BIRD_HOUSE+"."+birdVarps[i]);
            int varp=0;long seen=0L;
            if(stored!=null)
            {
                String[] p=stored.split(":");
                if(p.length==2)
                {
                    varp=safeInt(p[0],0);
                    seen=safeLong(p[1],0L);
                }
            }
            long readyAt=varp>0 && seen>0?seen+3000L:0L;
            b.addProperty("name",birdNames[i]);
            b.addProperty("varp",varp);
            b.addProperty("seenEpochSec",seen);
            b.addProperty("readyEpochSec",readyAt);
            b.addProperty("ready",readyAt>0 && now>=readyAt);
            b.addProperty("empty",varp<=0);
            birdhouses.add(b);
        }
        root.add("birdHouses",birdhouses);

        JsonObject contract=new JsonObject();
        String contractId=configManager.getRSProfileConfiguration(TimeTrackingConfig.CONFIG_GROUP,"contract");
        int itemId=safeInt(contractId,0);
        contract.addProperty("itemId",itemId);
        contract.addProperty("name",itemId>0?itemName(itemId):"");
        contract.addProperty("complete",client.getVarbitValue(VarbitID.FARMGUILD_CONTRACT_COMPLETE)>0);
        root.add("farmingContract",contract);

        JsonObject kingdom=new JsonObject();
        int approval=client.getVarbitValue(VarbitID.MISC_APPROVAL);
        kingdom.addProperty("approvalRaw",approval);
        kingdom.addProperty("approvalPercent",Math.max(0,Math.min(100,Math.round(approval*100f/127f))));
        kingdom.addProperty("coffer",Math.max(0,client.getVarbitValue(VarbitID.MISC_COFFERS)));
        root.add("kingdom",kingdom);
        return root;
    }

    private static int safeInt(String value,int fallback)
    {
        try{return value==null?fallback:Integer.parseInt(value.trim());}
        catch(Exception ex){return fallback;}
    }

    private static long safeLong(String value,long fallback)
    {
        try{return value==null?fallback:Long.parseLong(value.trim());}
        catch(Exception ex){return fallback;}
    }

    private JsonArray buildBankItems()
    {
        JsonArray out=new JsonArray();
        for(BankItemData item:latestBankItems.values())
        {
            JsonObject o=new JsonObject();
            o.addProperty("id",item.id);
            o.addProperty("name",item.name);
            o.addProperty("quantity",item.quantity);
            out.add(o);
        }
        return out;
    }

    private JsonArray buildStashUnits()
    {
        JsonArray out=new JsonArray();
        for(STASHUnit stash:STASHUnit.values())
        {
            JsonObject o=new JsonObject();
            o.addProperty("key",stash.name());
            o.addProperty("objectId",stash.getObjectId());

            boolean built=false;
            boolean filled=false;
            try
            {
                client.runScript(ScriptID.WATSON_STASH_UNIT_CHECK,stash.getObjectId(),0,0,0);
                int[] stack=client.getIntStack();
                built=stack!=null && stack.length>0 && stack[0]==1;
                filled=stack!=null && stack.length>1 && stack[1]==1;
                if(filled) built=true;
            }
            catch(Exception ex)
            {
                log.fine("STASH state failed for "+stash.name()+": "+ex.getMessage());
            }

            o.addProperty("built",built);
            o.addProperty("filled",filled);
            out.add(o);
        }
        return out;
    }

    private static Map<String,Integer> loadBossVarps()
    {
        Map<String,Integer> out=new LinkedHashMap<>();
        try(InputStream in=OneManSyncPlugin.class.getResourceAsStream("boss-killcount.tsv")){
            if(in==null)return out;
            try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
                String line;
                while((line=r.readLine())!=null){
                    if(line.trim().isEmpty())continue;
                    int tab=line.lastIndexOf('\t');
                    if(tab<=0)continue;
                    out.put(line.substring(0,tab),Integer.parseInt(line.substring(tab+1)));
                }
            }
        }catch(Exception e){log.warning("Could not load boss KC map: "+e.getMessage());}
        return out;
    }

    private static String cleanMessage(String s)
    {
        if(s==null)return"";
        String clean=Text.removeTags(s);
        clean=clean.replaceAll("@[^@]+@","");
        return clean.trim();
    }

    private static String normalize(String s){return s==null?"":s.trim().toLowerCase();}
    private static String titleCase(String s){
        if(s==null||s.isEmpty())return"";
        String lower=s.toLowerCase();return Character.toUpperCase(lower.charAt(0))+lower.substring(1);
    }

    private static final class CollectionEntry{
        final int itemId;final String name;final int quantity;final String section;final long occurredAt;final boolean isPet;final String sourceName;
        CollectionEntry(int itemId,String name,int quantity,String section,long occurredAt,boolean isPet,String sourceName){
            this.itemId=itemId;this.name=name;this.quantity=quantity;this.section=section==null?"":section;this.occurredAt=occurredAt;this.isPet=isPet;this.sourceName=sourceName==null?"":sourceName;
        }
    }
    private static final class CaMeta{
        final int id;final String name;final String tier;final String monster;
        CaMeta(int id,String name,String tier,String monster){this.id=id;this.name=name;this.tier=tier;this.monster=monster;}
    }
    private static final class LootItemData{
        final int id;final String name;final int quantity;
        LootItemData(int id,String name,int quantity){this.id=id;this.name=name;this.quantity=quantity;}
    }
    private static final class LootEventData{
        final String eventKey;final String sourceName;final long occurredAt;final Map<String,LootItemData> items=new LinkedHashMap<>();
        LootEventData(String eventKey,String sourceName,long occurredAt){this.eventKey=eventKey;this.sourceName=sourceName;this.occurredAt=occurredAt;}
    }
    private static final class PetEventData{
        final String eventKey;final String petName;final String sourceName;final int bossKc;final long occurredAt;
        PetEventData(String eventKey,String petName,String sourceName,int bossKc,long occurredAt){this.eventKey=eventKey;this.petName=petName;this.sourceName=sourceName==null?"":sourceName;this.bossKc=bossKc;this.occurredAt=occurredAt;}
    }

    private static final class BankItemData{
        final int id;final String name;final int quantity;
        BankItemData(int id,String name,int quantity){this.id=id;this.name=name;this.quantity=quantity;}
    }

    private static final class StorageItemData{
        final int id;final String name;final int quantity;
        StorageItemData(int id,String name,int quantity){this.id=id;this.name=name;this.quantity=quantity;}
    }

    private static final class PersonalBestData{
        final String eventKey;final String message;final String sourceName;final long occurredAt;
        PersonalBestData(String eventKey,String message,String sourceName,long occurredAt){this.eventKey=eventKey;this.message=message;this.sourceName=sourceName==null?"":sourceName;this.occurredAt=occurredAt;}
    }
}
