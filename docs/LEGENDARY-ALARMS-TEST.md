# Legendary alarms development test

Based on master 9ee4a3372a91619d916902fbac884fdcbaaa7bce (0.10.2).
Run OneManSyncPluginTest in IntelliJ, or ./gradlew run. Disable the installed Plugin Hub copy while testing the development copy.

Open OneMan tools > Collection / Combat alarms. Both switches are off by default and independent of website sync. Enable each switch, log in and use each Preview button. Preview never changes task/log completion or uploads an example unlock.

The test2 banner uses bundled transparent artwork edited from the exact selected Legendary reference: textured gold wings, cracked stone, crest, hanging item frame and detailed crossed-sword shield. Dynamic text and native item icons are drawn over the artwork. It scales to the viewport. Configure duration (3–15 seconds) and scale in OneMan Sync settings.

Enable Collection log notifications and Combat task completion messages in OSRS settings. A real new Collection Log message triggers the item banner; a real completed Combat Achievement message triggers the task name, tier and points. Every item name and all six task tiers are accepted without a hand-maintained whitelist or fixed task-ID limit. Item icons use the RuneLite item cache (including pets and untradeables); until resolved, a gold emblem is shown. No drop rates, source names or KC are guessed. Opening an old Collection Log/CA page does not replay historical unlocks.

Test multiple unlocks in succession, duplicate game messages, disabling each feature during its banner, logout/login, world hop and plugin disable/re-enable. Queued banners are cleared on leaving LOGGED_IN and on shutdown. No game inputs are automated; no alert data leaves the client. Existing sync and Hubs remain unchanged.

The branch is for manual in-game verification only. Do not submit to Plugin Hub until the owner confirms gameplay behavior and visual size.


## TEST3: tier and Diary alarms

Enable the new independent Combat tier unlocks and Achievement Diaries switches in the same alarm Hub. CA task messages remain a separate switch. Use the six-tier selector for both CA previews; use the twelve-region and four-tier selectors for Diary previews. All previews are local, marked Preview and never update completion or sync fake data.

Real CA tier unlocks follow the game's current CA_POINTS and six threshold varbits (not fixed historical point cutoffs). Real Diary notifications follow all 48 completion states, including legacy Karamja Easy/Medium/Hard state 2. Completion means the game's completion flag, not a guessed chat phrase, an individual Diary task or reward collection. No bonus CA points are invented for unlocking a tier. Login, reconnect and world hop establish a silent baseline after ten logged-in ticks; already-completed tiers are not replayed. Ordinary scene loading preserves the progress baseline. Disabled categories are not backfilled on enabling.

Test Collection Log, individual CA task, CA tier and Diary previews, each of the six CA tiers, each Diary tier and a long region name (Lumbridge & Draynor or Western Provinces). For real unlocks, test a task that crosses a CA points threshold (both banners should queue), final Diary tier completion, independent toggles, logout/login, world hop and plugin restart. The first ten logged-in ticks establish history; unlocks during that warm-up are intentionally not replayed. Public release remains pending in-game verification.

## TEST5: original quest completion window skin

TEST5 replaces TEST4's quest toast. The persisted alarmQuests key is retained, but its visible label is Quest window style. It themes only the native Questscroll background and text colors; there is no queued quest banner or quest status scanner. When OSRS opens that completion window, its real congratulation, quest title, reward texts, quest points, 3D reward model, original ribbon/rosette and close button remain owned by the game, in the original positions. Quests or miniquests without this native completion window do not get an extra invented popup.

Use Preview Quest Window for a local, explicitly marked example of the texture. This preview is illustrative and does not open a server-owned interface, award rewards, modify progress or show fabricated XP/QP. If the real window is already open, Preview simply applies the skin to it.

Test a real quest completion: a single themed original scroll should remain, with no large banner above it. Check every actual reward line, total quest points, reward model and ribbon, then close the native window normally and with Escape. Toggle the style off/on while it is open and confirm original colors/background restore. Check logout, world hop, plugin disable/re-enable and other notification categories. Existing widget sprite overrides are restored rather than erased, and overrides subsequently supplied by another plugin are preserved. Actual game layout remains for owner verification; tests do not automate OSRS.

Version: 0.11.0-alarms-test5. Public Plugin Hub release remains pending in-game verification.

## TEST6: native background rendering fix
Uses an UNDER_WIDGETS background at native scroll bounds instead of sprite-only overrides. Only decorative SCROLL is hidden and restored; rewards, model, ribbon and controls remain game-owned. One widget lookup per tick detects late layout and already-open windows.
Enable Quest window style in 0.11.0-alarms-test6. Toggle with real completion open, inspect rewards, ribbon and close/Escape. Preview remains illustrative. In-game confirmation required.

## TEST7: visible activation diagnostics
Alarm panel shows BUILD 0.11.0-alarms-test7 and Quest OFF / waiting for window / APPLIED / widget type, size and visibility. Enable & Preview Quest Window enables the style explicitly and previews without awarding any progress. Existing live windows can be tested by toggling, no additional quest required. New artifact and source zip names distinguish this build from older downloads.

## TEST8: persistent native widget background
The decorative background widget is changed from its original type to GRAPHIC with a per-widget sprite override. No background overlay or hidden parchment remains. Native interface drawing keeps the skin visible for the entire reward-window lifetime, independently of preview duration. Original type, sprite, tiling, opacity and existing sprite override restore on close, disable and shutdown. No reward text, model, ribbon or controls are moved or replaced.
Verify with the completion window left open longer than the configured preview duration; toggle style off/on and close normally/Escape. In-game result remains unverified.

## TEST9: dynamic background child
The legacy decorative background is temporarily a layer with a dynamic GRAPHIC child, using the per-widget texture override. Dynamic children use the IF3 drawing path. Native sibling reward texts, model, ribbon and close remain unchanged. Parent type/content and original children restore at cleanup. Verify actual background persistence and restoration manually; build success alone does not verify game rendering.
