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

## TEST4: quest completion alarms

A fifth independent toggle, Quest completions, and Preview Quest button are in the same alarm Hub. The preview is Waterfall Quest. Actual names come from every Quest entry exposed by RuneLite, including miniquests and RFD subquests; no manually maintained whitelist, guessed quest points or XP. A blue native quest-tab icon is drawn in the Legendary hanging frame.

Quest alarms reuse the Companion's existing quest scan every five logged-in ticks, so no second periodic quest scan is added. Only a previously observed unfinished -> finished transition is announced. Initial/login/profile-change/enable snapshots and newly discovered finished quests are silent. Disabled completions do not replay. Failed reads retain known completion and cannot cause duplicate banners. Expect up to roughly three seconds between the real completion state and a banner, plus any existing queue.

Test all five previews and independent toggles, a real quest and/or miniquest completion, multiple unlocks queued together, login/world hop/restart, and enabling the feature with old quests completed. Previews never change progress or upload fake quests. Confirm version 0.11.0-alarms-test4 before testing. Public Plugin Hub release remains pending owner verification.
