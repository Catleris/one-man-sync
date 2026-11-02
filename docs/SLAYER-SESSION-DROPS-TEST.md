# Slayer Session drops MVP

2026-11-02 · 0.11.2-hubs-test.4 · combined development PR #10.

Slayer Hub → Loot contains a compact Session drops card: observed superior kills, elapsed session time, total notable loot entries, latest three entries with quantity/age, and Reset session. Hover for the last five entries and NPC sources. No removed Session/History tab is restored; the existing loot-goal controls remain available.

Session starts with an observed active task or eligible attributed NPC loot, continues across task changes and includes banking/travel time. Logout, account change and plugin stop clear it; manual reset only clears this new counter/log, not the existing Slayer journal or loot goal. World hops that do not reach the login screen retain the session. No past gameplay is reconstructed or written to public files.

Superior kills use attributed NpcLootReceived events and an explicit superior name catalog. Spawn messages and nearby NPC deaths do not count. A superior can still count after the final task kill. Events lacking attribution cannot be reconstructed; unknown future superior names need catalog updates. Same NPC/index/id/tick events are deduplicated with bounded session memory. Regular monster notable loot must match the latest confirmed task's monster family; an old task retained after completion allows final delayed loot attribution, not proof that the assignment remains active.

The notable-item allowlist includes the four shared superior uniques, Eternal/Pegasian/Primordial crystals and selected other Slayer items. Eternal crystal is logged as notable Cerberus loot, not as a shared superior unique. It is a curated MVP, not every OSRS unique. No RDT provenance is inferred, no bank/inventory additions are treated as loot, and actual pickup is not verified.

## Owner checks pending

1. Open Slayer → Loot with an active task: zero superior kills and elapsed time should appear. Existing loot-goal controls remain usable.
2. A superior spawn must not increment the count. Kill your own superior and receive its attributed loot: increment once, including after the last task kill. Ordinary gargoyles/basilisk knights/bosses must not count as superiors.
3. On an eligible notable drop, check item, quantity and source tooltip. Leaving it on the ground must still say Loot observed / pickup not verified. Common loot must not fill the small log. Recent entries are capped at five, with three shown inline.
4. Switch tasks: totals remain. Reset session: new counters/log clear, old journal and loot goal stay. Logout/relogin or switch accounts: no previous session data should appear.
5. Recheck prior Slayer bank/location and Farming health/compost behavior in the same build.

Run `.\gradlew.bat run` or IntelliJ OneManSyncPluginTest. For Jagex accounts see https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts . No master merge or Plugin Hub pin update yet.

Superior identity/drop reference: https://oldschool.runescape.wiki/w/Superior_slayer_monster ; bundled slayer-knowledge.tsv provides task-family aliases. No remote network lookup occurs during play.
