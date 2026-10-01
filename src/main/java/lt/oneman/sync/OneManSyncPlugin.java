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
import net.runelite.api.GameState;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
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
    description = "Syncs OSRS progress and memories to oneman.lt",
    tags = {"progress", "quests", "ironman", "sync", "achievements", "collection", "log", "boss", "kc", "pets", "loot"},
    internalName = "one-man-sync"
)
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

    @Inject private Client client;
    @Inject private OneManSyncConfig config;
    @Inject private OkHttpClient http;
    @Inject private Gson gson;
    @Inject private ItemManager itemManager;

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

    private volatile long pendingPetSignalAt = 0L;
    private volatile int pendingPetSignalTick = -1;
    private volatile String lastLootSource = "";
    private volatile long lastLootAt = 0L;

    @Provides
    OneManSyncConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OneManSyncConfig.class);
    }

    @Override
    protected void startUp()
    {
        if (client.getGameState() == GameState.LOGGED_IN) loginCountdown = 10;
    }

    @Override
    protected void shutDown()
    {
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
        pendingPetSignalAt=0L;
        pendingPetSignalTick=-1;
        lastLootSource="";
        lastLootAt=0L;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if(event.getGameState()==GameState.LOGGED_IN){
            loginCountdown=10;
            progressDirty=true;
        } else if(event.getGameState()==GameState.LOGIN_SCREEN){
            loginCountdown=-1;
            progressDirty=false;
            lastKnownTotalLevel=-1;
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if(!ready()) return;
        int total=calculateTotalLevel();
        if(lastKnownTotalLevel>0 && total>lastKnownTotalLevel) syncSoon();
        lastKnownTotalLevel=total;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if(ready()) progressDirty=true;
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event)
    {
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
        if(!ready() || event.getContainerId()!=InventoryID.BANK) return;

        ItemContainer bank=event.getItemContainer();
        if(bank==null || bank.getItems()==null) return;

        Map<Integer,BankItemData> snapshot=new LinkedHashMap<>();
        for(Item item:bank.getItems())
        {
            if(item==null || item.getId()<=0 || item.getQuantity()<=0) continue;

            String name;
            try
            {
                name=itemManager.getItemComposition(item.getId()).getName();
            }
            catch(Exception ex)
            {
                name="Item #"+item.getId();
            }

            if(name==null || name.trim().isEmpty() || "null".equalsIgnoreCase(name)) continue;

            BankItemData old=snapshot.get(item.getId());
            int qty=item.getQuantity()+(old!=null?old.quantity:0);
            snapshot.put(item.getId(),new BankItemData(item.getId(),name.trim(),qty));
        }

        latestBankItems.clear();
        latestBankItems.putAll(snapshot);
        bankSnapshotComplete=true;
        bankSnapshotEpochMs=System.currentTimeMillis();
        syncSoon();
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if(!ready()) return;
        ChatMessageType type=event.getType();
        if(type!=ChatMessageType.GAMEMESSAGE && type!=ChatMessageType.SPAM) return;

        String message=cleanMessage(event.getMessage());
        long now=System.currentTimeMillis();

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

    private static final class PersonalBestData{
        final String eventKey;final String message;final String sourceName;final long occurredAt;
        PersonalBestData(String eventKey,String message,String sourceName,long occurredAt){this.eventKey=eventKey;this.message=message;this.sourceName=sourceName==null?"":sourceName;this.occurredAt=occurredAt;}
    }
}
