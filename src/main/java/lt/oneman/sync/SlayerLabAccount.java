package lt.oneman.sync;

import com.google.gson.Gson;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
final class SlayerLabAccount
{
    static final class Bank
    {
        long seen;
        Map<String,Integer> items = new TreeMap<>();
    }
    @Inject private Gson gson;
    @Inject private ConfigManager configs;
    String profile = "";
    Bank bank = new Bank();
    SlayerLabJournal journal = new SlayerLabJournal();
    private long lastSave;
    boolean bind(String key, long now)
    {
        if (key == null || key.isEmpty()) return false;
        if (profile.equals(key)) return true;
        pause("Profile changed", now);
        profile = key;
        bank = read("bankSnapshot", Bank.class, new Bank());
        journal = read("journal", SlayerLabJournal.class, new SlayerLabJournal());
        if (bank.items == null) bank = new Bank();
        if (journal.active != null) journal.end("Interrupted session recovered", Math.max(journal.active.started, journal.active.updated));
        save(now, true);
        return true;
    }
    private <T> T read(String name, Class<T> type, T fallback)
    {
        try
        {
            String json = configs.getConfiguration(SlayerLabConfig.GROUP, profile, name);
            T data = json == null ? null : gson.fromJson(json, type);
            return data == null ? fallback : data;
        }
        catch (RuntimeException badStoredData) { return fallback; }
    }
    void captureBank(Map<String,Integer> items, long now)
    {
        if (profile.isEmpty()) return;
        bank.items = new TreeMap<>(items); bank.seen = now;
        configs.setConfiguration(SlayerLabConfig.GROUP, profile, "bankSnapshot", gson.toJson(bank));
    }
    void pause(String reason, long now)
    {
        if (profile.isEmpty()) return;
        journal.end(reason, now); save(now, true);
    }
    void save(long now, boolean force)
    {
        if (profile.isEmpty() || (!force && now - lastSave < 30000)) return;
        configs.setConfiguration(SlayerLabConfig.GROUP, profile, "journal", gson.toJson(journal)); lastSave = now;
    }
    String bankAge(long now)
    {
        if (bank.seen <= 0) return "Bank unknown — open your bank to capture a snapshot.";
        return "Bank last opened: " + java.time.Instant.ofEpochMilli(bank.seen) + " (" + Math.max(0, now-bank.seen)/60000
            + " min ago). Snapshot ownership is not current inventory; items may have changed.";
    }
    String preparation(SlayerLabCatalog.Guide guide, Set<String> inventory, Set<String> equipment, long now)
    {
        StringBuilder text = new StringBuilder(bankAge(now) + "\n\n");
        if (guide != null)
            for (SlayerLabCatalog.Requirement requirement : guide.requirements)
            {
                String status = requirement.status(inventory, equipment);
                if (status.startsWith("MISSING"))
                    status = bank.seen <= 0 ? "NOT CARRIED — bank unknown"
                        : bank.items.entrySet().stream().anyMatch(e -> e.getValue() > 0 && requirement.matches(e.getKey()))
                            ? "TAKE FROM BANK — found in dated snapshot" : "NOT CARRIED / not in dated bank snapshot";
                text.append(requirement.label).append("\n").append(status).append("\n\n");
            }
        text.append("Checklist covers the regular task monster. A manually chosen boss/variant can require different gear. Diary exemptions and protection effects are not verified automatically.\n\n");
        text.append("General supplies: choose your food, combat supplies and an escape/bank teleport. Special protection, charges, quantity and variant suitability still matter.");
        return text.toString();
    }
}
