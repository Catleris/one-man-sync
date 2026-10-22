package lt.oneman.sync;

import com.google.gson.GsonBuilder;
import com.google.inject.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static lt.oneman.sync.CombatAchievementCatalog.*;

public class CombatAchievementsHubTest
{
    private Client cache(){
        Client client=mock(Client.class);EnumComposition bosses=mock(EnumComposition.class);
        when(client.getEnum(BOSS_NAMES)).thenReturn(bosses);when(bosses.getStringValue(1)).thenReturn("Barrows");
        for(Tier tier:Tier.values()){
            EnumComposition entries=mock(EnumComposition.class);int structId=100+tier.ordinal();
            when(client.getEnum(tier.enumId)).thenReturn(entries);when(entries.getIntVals()).thenReturn(new int[]{structId});
            StructComposition value=mock(StructComposition.class);when(client.getStructComposition(structId)).thenReturn(value);
            when(value.getIntValue(TASK_ID)).thenReturn(tier.ordinal());when(value.getIntValue(BOSS)).thenReturn(1);
            when(value.getStringValue(NAME)).thenReturn("Task "+tier.label);when(value.getStringValue(DESCRIPTION)).thenReturn("A readable task description.");
        }
        return client;
    }
    @Test public void catalogUsesGameEnumsAndRejectsPartialOrDuplicateData(){
        Client client=cache();List<Task> tasks=load(client);assertEquals(6,tasks.size());assertEquals("Barrows",tasks.get(0).boss);
        when(client.getEnum(Tier.MASTER.enumId)).thenReturn(null);assertTrue(load(client).isEmpty());
        client=cache();when(client.getStructComposition(101).getIntValue(TASK_ID)).thenReturn(0);assertTrue(load(client).isEmpty());
    }
    @Test public void completionIncludesSignBitAndTwentyFirstPackedVarp(){
        int[] values=new int[21];values[0]=Integer.MIN_VALUE;values[1]=1;values[20]=Integer.MIN_VALUE;
        assertTrue(completed(31,values));assertTrue(completed(32,values));assertTrue(completed(671,values));
        assertFalse(completed(30,values));assertFalse(completed(672,values));assertFalse(completed(-1,values));
    }
    @Test public void lifecycleClearsProgressAndRestoresOnlyCurrentProfileTracking() throws Exception {
        Client client=cache();ConfigManager configs=mock(ConfigManager.class);ClientThread thread=mock(ClientThread.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);when(configs.getRSProfileKey()).thenReturn("account-a");
        when(configs.getConfiguration("one-man-sync","account-a",CombatAchievementsHub.TRACKED_KEY)).thenReturn("[0,1]");
        when(configs.getConfiguration("one-man-sync","account-b",CombatAchievementsHub.TRACKED_KEY)).thenReturn("[3]");
        when(client.getVarpValue(CombatAchievementsHub.VARPS[0])).thenReturn(1);
        when(client.getVarbitValue(VarbitID.CA_POINTS)).thenReturn(1);
        CombatAchievementsHub hub=Guice.createInjector(new AbstractModule(){protected void configure(){
            bind(Client.class).toInstance(client);bind(ConfigManager.class).toInstance(configs);bind(ClientThread.class).toInstance(thread);
            bind(com.google.gson.Gson.class).toInstance(new GsonBuilder().create());
            bind(net.runelite.client.game.ItemManager.class).toInstance(mock(net.runelite.client.game.ItemManager.class));
            bind(net.runelite.client.game.SpriteManager.class).toInstance(mock(net.runelite.client.game.SpriteManager.class));
        }}).getInstance(CombatAchievementsHub.class);
        hub.startUp();for(int i=0;i<16;i++)hub.onGameTick();assertTrue(hub.snapshot().ready);
        assertEquals(new HashSet<>(Arrays.asList(0,1)),hub.snapshot().tracked);assertTrue(hub.snapshot().completed.contains(0));
        SwingUtilities.invokeAndWait(()->{
            JPanel panel=hub.companionPanel();find(panel,"combatBoss:Barrows",JToggleButton.class).doClick();
            find(panel,"combatTrack:2",JCheckBox.class).doClick();
        });
        ArgumentCaptor<Runnable> actions=ArgumentCaptor.forClass(Runnable.class);verify(thread).invokeLater(actions.capture());
        actions.getValue().run();verify(configs).setConfiguration(eq("one-man-sync"),eq("account-a"),eq(CombatAchievementsHub.TRACKED_KEY),any());
        hub.changed();for(int i=0;i<6;i++)hub.onGameTick();assertTrue(hub.snapshot().tracked.contains(2));
        when(configs.getRSProfileKey()).thenReturn("account-b");
        clearInvocations(configs);actions.getValue().run();
        verify(configs,never()).setConfiguration(anyString(),anyString(),eq(CombatAchievementsHub.TRACKED_KEY),any());
        hub.onGameTick();assertFalse(hub.snapshot().ready);assertTrue(hub.snapshot().tracked.isEmpty());
        when(client.getVarpValue(CombatAchievementsHub.VARPS[0])).thenReturn(0);
        for(int i=0;i<16;i++)hub.onGameTick();assertTrue(hub.snapshot().completed.isEmpty());assertEquals(Collections.singleton(3),hub.snapshot().tracked);
        GameStateChanged out=new GameStateChanged();out.setGameState(GameState.LOGIN_SCREEN);hub.onGameStateChanged(out);assertFalse(hub.snapshot().ready);assertTrue(hub.snapshot().completed.isEmpty());
        hub.shutDown();hub.onGameTick();assertFalse(hub.snapshot().ready);SwingUtilities.invokeAndWait(()->{});
    }
    @Test public void expandedBossFiltersAndTaskViewsFitNarrowSidebar() throws Exception {
        List<Task> tasks=new ArrayList<>();for(int i=0;i<40;i++)tasks.add(new Task(i,"Long readable combat achievement task "+i,"Complete this task using the requirements shown in game.","Barrows","Restriction",Tier.EASY));
        CombatAchievementsHub.Snapshot state=new CombatAchievementsHub.Snapshot(tasks,Collections.singleton(0),Collections.singleton(1),new int[]{33,115,304,820,1465,2300},1,true,"account");
        SwingUtilities.invokeAndWait(()->{
            CombatAchievementsPanel panel=new CombatAchievementsPanel((id,selected,profile)->{});panel.update(state);panel.setSize(242,700);
            find(panel,"combatFiltersToggle",JToggleButton.class).doClick();find(panel,"combatBoss:Barrows",JToggleButton.class).doClick();
            for(int i=0;i<6;i++)layout(panel);fits(panel);
            assertNotNull(find(panel,"combatTrack:1",JCheckBox.class));
            find(panel,"combatView:Tracked",JToggleButton.class).doClick();assertNotNull(find(panel,"combatTask:1",JPanel.class));assertNull(find(panel,"combatTask:0",JPanel.class));
            find(panel,"combatView:Tasks",JToggleButton.class).doClick();assertNull(find(panel,"combatTask:39",JPanel.class));find(panel,"combatShowMore",JButton.class).doClick();assertNotNull(find(panel,"combatTask:39",JPanel.class));
            find(panel,"combatSearch",JTextField.class).setText("No matching task");assertNull(find(panel,"combatTask:0",JPanel.class));
            for(int i=0;i<6;i++)layout(panel);fits(panel);
            assertTrue(CombatAchievementsPanel.nextTier(state).contains("32 pts"));
        });
    }
    private static <T extends Component>T find(Container parent,String name,Class<T> type){for(Component c:parent.getComponents()){
        if(name.equals(c.getName())&&type.isInstance(c))return type.cast(c);if(c instanceof Container){T value=find((Container)c,name,type);if(value!=null)return value;}}
        return null;
    }
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
    private static void fits(Container c){for(Component child:c.getComponents())if(child.isVisible()){
        assertTrue(child.getClass()+" overflows right edge",child.getX()+child.getWidth()<=c.getWidth());assertTrue(child.getX()>=0);if(child instanceof Container)fits((Container)child);
    }}
}
