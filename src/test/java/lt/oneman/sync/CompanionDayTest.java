package lt.oneman.sync;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class CompanionDayTest {
    @Test public void reconnectDoesNotCountOfflineXpOrQuests(){
        CompanionDay day=new CompanionDay();day.observe("2026-10-08",Collections.singletonMap("Agility",1),Collections.emptySet(),100);
        day.observe("2026-10-08",Collections.singletonMap("Agility",2),Collections.singleton("Q"),150);
        day.resume();day.observe("2026-10-08",Collections.singletonMap("Agility",3),new HashSet<>(Arrays.asList("Q","OFFLINE")),500);
        assertEquals(Integer.valueOf(1),day.gainedLevels.get("Agility"));assertEquals(50,day.xp);assertEquals(Collections.singleton("Q"),day.quests);
        day.observe("2026-10-08",Collections.singletonMap("Agility",3),new HashSet<>(Arrays.asList("Q","OFFLINE")),510);assertEquals(60,day.xp);
    }
    @Test public void utcDateRollsOverAndProfilesAreIndependent(){CompanionDay a=new CompanionDay(),b=new CompanionDay();a.observe("2026-10-08",Collections.emptyMap(),Collections.emptySet(),100);a.observe("2026-10-08",Collections.emptyMap(),Collections.emptySet(),200);b.observe("2026-10-08",Collections.emptyMap(),Collections.emptySet(),200);assertEquals(0,b.xp);a.observe("2026-10-09",Collections.emptyMap(),Collections.emptySet(),220);assertEquals(0,a.xp);}
}
