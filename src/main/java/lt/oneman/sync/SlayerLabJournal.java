package lt.oneman.sync;

import java.util.*;

/** Observed sessions only; task credits are kept separate from attributed loot kills. */
final class SlayerLabJournal
{
    static final class Session
    {
        String task, assignedArea, chosenRoute = "", variant = "Regular task monsters", gear = "", note = "", status = "Active";
        long started, ended, updated;
        int initial, remaining, credits, kills, slayerXp;
        final Map<String,Integer> loot = new TreeMap<>();
        long duration(long now) { return Math.max(0, (ended > 0 ? ended : now) - started); }
    }
    Session active;
    final List<Session> history = new ArrayList<>();
    void observe(String task, String area, int remaining, int original, long now)
    {
        if (active != null && (!active.task.equals(task) || !Objects.equals(active.assignedArea, area)
            || remaining > active.remaining || (original > 0 && active.initial > 0 && original != active.initial)))
            end("Task changed / new assignment", now);
        if (active == null)
        {
            active = new Session(); active.task = task; active.assignedArea = area; active.started = now;
            active.initial = original; active.remaining = remaining;
        }
        else
        {
            active.credits += Math.max(0, active.remaining - remaining);
            active.remaining = remaining;
        }
        if (active.initial == 0 && original > 0) active.initial = original;
        active.updated = now;
    }
    void end(String reason, long now)
    {
        if (active == null) return;
        active.ended = now; active.status = reason;
        history.add(0, active); active = null;
        while (history.size() > 50) history.remove(history.size() - 1);
    }
    void clear() { active = null; history.clear(); }
    void loot(Map<String,Integer> items)
    {
        if (active == null) return;
        active.kills++;
        items.forEach((item,quantity) -> active.loot.merge(item, quantity, Integer::sum));
    }
    void xp(int delta) { if (active != null && delta > 0) active.slayerXp += delta; }
    static String metrics(Session session, long now)
    {
        if (session == null) return "Waiting for an active Slayer task.";
        long elapsed = session.duration(now);
        double hours = elapsed / 3600000.0;
        String rates = elapsed >= 60000 ? String.format(Locale.ROOT,
            "Attributed loot kills/h: %.1f\nTask credits/h: %.1f\nSession Slayer XP/h: %.0f",
            session.kills / hours, session.credits / hours, session.slayerXp / hours) : "Rates: collecting at least 60 seconds of data";
        String eta = session.credits >= 3 && elapsed >= 60000
            ? String.format(Locale.ROOT, "%.0f min", session.remaining * (elapsed / 60000.0) / session.credits) : "collecting data";
        return session.task + "\nActive session: " + (elapsed / 60000) + "m " + ((elapsed / 1000) % 60) + "s"
            + "\nAttributed loot kills: " + session.kills + "\nTask credits observed: " + session.credits
            + "\nSession Slayer XP: " + session.slayerXp + "\n" + rates + "\nEstimated remaining: " + eta
            + "\n\nKills use attributed NPC loot events; kills without such an event may be missed. Bracelets and multi-credit kills make task credits differ from kills. XP includes any Slayer XP gained in this session. Rates include banking/travel and are estimates.";
    }
    String historyText()
    {
        StringBuilder text = new StringBuilder("Last 50 observed sessions, newest first. Times are UTC. No older gameplay is reconstructed.\n\n");
        for (Session session : history)
        {
            text.append(java.time.Instant.ofEpochMilli(session.started)).append("\n").append(session.task)
                .append(" — ").append(session.status).append("\nDuration: ").append(session.duration(session.ended) / 60000).append("m")
                .append(" | kills: ").append(session.kills).append(" | credits: ").append(session.credits)
                .append(" | XP: ").append(session.slayerXp).append("\nAssigned area: ").append(session.assignedArea)
                .append("\nChosen route: ").append(session.chosenRoute).append("\nVariant: ").append(session.variant)
                .append("\nSetup last seen: ").append(session.gear).append("\nNote: ").append(session.note)
                .append("\nObserved loot: ").append(session.loot).append("\n\n");
        }
        if (history.isEmpty()) text.append("No archived sessions yet. Logout, task change, clear or plugin shutdown archives the active session.");
        return text.toString();
    }
}
