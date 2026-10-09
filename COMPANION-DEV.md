# OneMan Companion 0.7.2

Release candidate: 0.7.2. User requested submission through the existing Plugin Hub PR #18006. In-game validation remains pending.

## 0.7.2 additions

- Opt-in Superior alert uses the official game message, with red flash and configurable notification settings. Duplicate local spawn signals are suppressed; extra sound/tray output is suppressed when built-in Slayer alerts are enabled.
- Prayer tab removed; alarm settings retained.
- Task protection overlay: red missing, purple in last observed bank, green carried/equipped, grey unknown bank.
- Passive text updates preserve scroll position. Slayer config defaults are declared directly for RuneLite initialization.
- Karamja Easy/Medium/Hard completion uses raw state 2; Elite and other region completion bits use 1.

## Earlier feedback fixes

- Bloodveld singular assignment now resolves to the full Bloodvelds task facts, entrance list and protection notes. Assigned-area filtering remains strict. Adds Iorwerth / God Wars entrances and a Meiyerditch reference entry without an invented map pin. Stronghold cannon permission is explicit; Bloodveld-specific Iorwerth/Meiyerditch cannon permission is explicit; tower, Catacombs and God Wars prohibition remain explicit; unaudited chambers say unverified.
- A selected skill/quest goal now controls Next, with actual gaps and prerequisite steps. Slayer targets show real XP remaining, observed task, known eligible master choices filtered by combat/Slayer/quests, and a practical task loop. Other skills use their catalog method. Default roadmap can be restored with its button.
- Observed Chronicle ownership in inventory/equipment/dated bank is recorded per profile and skips the obtain step. This does not infer remaining charges.
- C/S navigation merged into a single OneMan shield, with Overview and Slayer sections. Existing settings and profile keys remain compatible.
- Sync displays a bounded/redacted JSON server error for failed uploads. HTTP 500 is a server failure, not proof of client sync; live root cause remains unconfirmed until the server error is observed. No production change/deployment is made.

Cannon source facts: OSRS Wiki Stronghold Slayer Cave / Dwarf multicannon; Bloodveld chamber coverage cross-checked against https://github.com/FreeArcanes/slayer-best-in-bank/blob/master/CANNON-TASK-COVERAGE.md (audit 2026-07-25). Entrance pins come from RuneLite DungeonLocation. Meiyerditch is deliberately reference-only until a precise entrance is verified. Location guide opens the selected dungeon page.

## Included

- Single OneMan shield sidebar navigation; Overview / Slayer sections. Next tab: three local catalog steps, real level gaps, missing quests and route reasons. Catalog version 2026-10-06.1 matches the website catalog; no projected quest XP or assumed levels. Quest points use the live game QP varp. This is the default Ironman route, not a guarantee of globally optimal routing or combat readiness. Web-only manual selections are not imported into this local planner.
- Prep tab: select a route/catalog goal, compare required quantity with unnoted inventory, equipment and dated bank snapshot, open guide. Bank outlines follow the chosen checklist.
- Verified starter checklists: Cook's Assistant, Doric's Quest, Druidic Ritual, Sheep Shearer, Goblin Diplomacy, Chronicle; generic mining/woodcutting tools. Goals without a checklist explicitly say unknown. Existing OneMan Sync Slayer Prep covers regular task protection separately. Tool level, charges, encounters and stage-specific extras still need the guide.
- Slayer tab: observed session start/status, remaining, task credits, attributed loot kills, XP and loot. Existing Session/History tabs retain detailed journal history. Task credits and loot kills are distinct.
- Sync tab: this build's version and successful Skills, Quests, confirmed Slayer uploads; Bank shows the capture time of the successfully uploaded snapshot. Failure keeps previous success times. Values reset for another RS profile.
- Memories tab: optional 99/quest-completion captures and manual progress screenshot. Local capture defaults off. Server upload has a separate default-off toggle and requires OneMan Sync/key. Screenshot includes the visible game canvas. Saved using RuneLite's plugin directory under `screenshots/<profile UUID>/`; panel reports the exact path. Work is bounded to three pending captures; upload failure retains the local file, with no retry.
- Summary tab: observed UTC-day XP/level gains/completed quests across reconnects, current-session progress including Slayer totals/loot, chosen next goal, previous logout summary. Daily/goal/manual confirmations and summaries use the RS profile, not another account. Offline XP/quests are excluded. Data is sampled every five ticks after login settles.

## OneMan upload support

The matching website `dev/companion-suite` adds `runelite_memory_upload.php`, validation tests and CI. It requires a live device token, active account and synced matching player; validates image content/dimensions/5 MB, serializes per-account quota checks, and inserts a client-labelled screenshot memory. It does not accept arbitrary event IDs. Limits: 30 RuneLite uploads/hour, 500 total screenshots, 250 MB/account. Until that endpoint is deployed, server upload fails gracefully; local captures work.

## Run locally

Use Java 11 or newer. From this branch: `./gradlew run` (Windows: `gradlew.bat run`). The task uses a separate `dev-home` RuneLite profile. Enable OneMan Sync in development mode; use the single OneMan shield sidebar button and its Overview / Slayer selector.

Alternatively run the CI `one-man-sync-0.7.2-all.jar` with `java -jar <file> --developer-mode --debug`. The packaged development launcher uses a separate RuneLite home; see `OneManSyncPluginTest.java`.

For a Jagex Account follow https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts . Do not run the Hub copy and development plugin against the same session simultaneously.

## User-run checks (required)

1. Fresh account: correct real skill levels, no invented 20 Agility; complete a quest/train a target and verify the route advances. Confirm only an actual manual unlock using the Prep button. Completed quests cannot be manually fabricated with that button.
2. Choose Doric: Clay 6, Copper ore 4, Iron ore 2. Open bank, check outlines; withdraw items and verify inventory count changes. Noted items do not count as ready inventory. Switch to a goal with unknown supplies: no readiness or outlines.
3. Slayer: kill a task monster, check remaining/credits/observed loot/XP, bank/travel, finish or logout, inspect archived history.
4. Enable sync, inspect timestamps. Try invalid/revoked key and ensure previous successes remain visible with failure status. Switch accounts and confirm no prior bank/sync/day/goal data appears.
5. Capture stays off by default. Enable local capture, click manual screenshot, inspect file; complete a quest or reach 99 and inspect one capture. Enable upload only after the website endpoint is deployed; inspect your own OneMan memory. Disable capture/upload and verify no new upload is sent. Stop plugin with work pending: no lingering callbacks/network request.
6. Summary: gain XP/level/quest, logout and reconnect. Previous summary remains, same UTC-day observed totals accumulate, offline XP is excluded, another RS profile has independent history.

Automated checks validate planner prerequisites/unknown levels/manual skipping/maxing exclusion, supply quantities/names, reconnect/day/profile accounting, and existing Slayer logic. Compilation and unit tests cannot confirm actual in-game event timing or UI behaviour.
