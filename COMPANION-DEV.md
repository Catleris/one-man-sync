# OneMan Companion 0.7.0-dev

Development branch: `dev/companion-suite`. The Hub release/pending Slayer PR is unchanged.

## Included

- OneMan Companion sidebar navigation (`C`), Next tab: three local catalog steps, real level gaps, missing quests and route reasons. Catalog version 2026-10-06.1 matches the website catalog; no projected quest XP or assumed levels. Quest points use the live game QP varp. This is the default Ironman route, not a guarantee of globally optimal routing or combat readiness. Web-only manual selections are not imported into this local planner.
- Prep tab: select a route/catalog goal, compare required quantity with unnoted inventory, equipment and dated bank snapshot, open guide. Bank outlines follow the chosen checklist.
- Verified starter checklists: Cook's Assistant, Doric's Quest, Druidic Ritual, Sheep Shearer, Goblin Diplomacy, Chronicle; generic mining/woodcutting tools. Goals without a checklist explicitly say unknown. Existing OneMan Sync Slayer Prep covers regular task protection separately. Tool level, charges, encounters and stage-specific extras still need the guide.
- Slayer tab: observed session start/status, remaining, task credits, attributed loot kills, XP and loot. Existing Session/History tabs retain detailed journal history. Task credits and loot kills are distinct.
- Sync tab: this build's version and successful Skills, Quests, confirmed Slayer uploads; Bank shows the capture time of the successfully uploaded snapshot. Failure keeps previous success times. Values reset for another RS profile.
- Memories tab: optional 99/quest-completion captures and manual progress screenshot. Local capture defaults off. Server upload has a separate default-off toggle and requires OneMan Sync/key. Screenshot includes the visible game canvas. Saved using RuneLite's plugin directory under `screenshots/<profile UUID>/`; panel reports the exact path. Work is bounded to three pending captures; upload failure retains the local file, with no retry.
- Summary tab: observed UTC-day XP/level gains/completed quests across reconnects, current-session progress including Slayer totals/loot, chosen next goal, previous logout summary. Daily/goal/manual confirmations and summaries use the RS profile, not another account. Offline XP/quests are excluded. Data is sampled every five ticks after login settles.

## OneMan upload support

The matching website `dev/companion-suite` adds `runelite_memory_upload.php`, validation tests and CI. It requires a live device token, active account and synced matching player; validates image content/dimensions/5 MB, serializes per-account quota checks, and inserts a client-labelled screenshot memory. It does not accept arbitrary event IDs. Limits: 30 RuneLite uploads/hour, 500 total screenshots, 250 MB/account. Until that endpoint is deployed, server upload fails gracefully; local captures work.

## Run locally

Use Java 11 or newer. From this branch: `./gradlew run` (Windows: `gradlew.bat run`). The task uses a separate `dev-home` RuneLite profile. Enable OneMan Sync in development mode; use the `C` sidebar button for Companion and `S` for Slayer tools.

Alternatively run the CI `one-man-sync-0.7.0-dev-all.jar` with `java -jar <file> --developer-mode --debug`. The packaged development launcher uses a separate RuneLite home; see `OneManSyncPluginTest.java`.

For a Jagex Account follow https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts . Do not run the Hub copy and development plugin against the same session simultaneously.

## User-run checks (required)

1. Fresh account: correct real skill levels, no invented 20 Agility; complete a quest/train a target and verify the route advances. Confirm only an actual manual unlock using the Prep button. Completed quests cannot be manually fabricated with that button.
2. Choose Doric: Clay 6, Copper ore 4, Iron ore 2. Open bank, check outlines; withdraw items and verify inventory count changes. Noted items do not count as ready inventory. Switch to a goal with unknown supplies: no readiness or outlines.
3. Slayer: kill a task monster, check remaining/credits/observed loot/XP, bank/travel, finish or logout, inspect archived history.
4. Enable sync, inspect timestamps. Try invalid/revoked key and ensure previous successes remain visible with failure status. Switch accounts and confirm no prior bank/sync/day/goal data appears.
5. Capture stays off by default. Enable local capture, click manual screenshot, inspect file; complete a quest or reach 99 and inspect one capture. Enable upload only after the website endpoint is deployed; inspect your own OneMan memory. Disable capture/upload and verify no new upload is sent. Stop plugin with work pending: no lingering callbacks/network request.
6. Summary: gain XP/level/quest, logout and reconnect. Previous summary remains, same UTC-day observed totals accumulate, offline XP is excluded, another RS profile has independent history.

Automated checks validate planner prerequisites/unknown levels/manual skipping/maxing exclusion, supply quantities/names, reconnect/day/profile accounting, and existing Slayer logic. Compilation and unit tests cannot confirm actual in-game event timing or UI behaviour.
