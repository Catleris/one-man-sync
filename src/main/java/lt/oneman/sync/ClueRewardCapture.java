package lt.oneman.sync;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Correlates server clue completion with a fresh reward container in either event order. */
final class ClueRewardCapture
{
    private static final Pattern COMPLETED = Pattern.compile("You have completed ([0-9,]+) (beginner|easy|medium|hard|elite|master) Treasure Trails?\\.", Pattern.CASE_INSENSITIVE);
    private ClueTier pending;
    private int deadline, version, consumed, changedTick = -1;
    private String last = "";
    void clear() { pending = null; deadline = version = consumed = 0; changedTick = -1; last = ""; }
    boolean completion(String message, int tick)
    {
        Matcher match = COMPLETED.matcher(message);
        if (!match.find()) return false;
        String token = match.group(2).toLowerCase(Locale.ROOT) + ":" + match.group(1);
        if (token.equals(last)) return false;
        last = token; pending = ClueTier.valueOf(match.group(2).toUpperCase(Locale.ROOT)); deadline = tick + 5;
        return changedTick == tick && version > consumed;
    }
    boolean containerChanged(int tick)
    {
        version++; changedTick = tick;
        return pending != null && tick <= deadline;
    }
    ClueTier tier() { return pending; }
    void consumed() { consumed = version; pending = null; }
    void tick(int tick) { if (tick > deadline) pending = null; }
}
