# Legendary alarms development test

Based on master 9ee4a3372a91619d916902fbac884fdcbaaa7bce (0.10.2).
Run OneManSyncPluginTest in IntelliJ, or ./gradlew run. Disable the installed Plugin Hub copy while testing the development copy.

Open OneMan tools > Collection / Combat alarms. Both switches are off by default and independent of website sync. Enable each switch, log in and use each Preview button. Preview never changes task/log completion or uploads an example unlock.

The test2 banner uses bundled transparent artwork edited from the exact selected Legendary reference: textured gold wings, cracked stone, crest, hanging item frame and detailed crossed-sword shield. Dynamic text and native item icons are drawn over the artwork. It scales to the viewport. Configure duration (3–15 seconds) and scale in OneMan Sync settings.

Enable Collection log notifications and Combat task completion messages in OSRS settings. A real new Collection Log message triggers the item banner; a real completed Combat Achievement message triggers the task name, tier and points. Every item name and all six task tiers are accepted without a hand-maintained whitelist or fixed task-ID limit. Item icons use the RuneLite item cache (including pets and untradeables); until resolved, a gold emblem is shown. No drop rates, source names or KC are guessed. Opening an old Collection Log/CA page does not replay historical unlocks.

Test multiple unlocks in succession, duplicate game messages, disabling each feature during its banner, logout/login, world hop and plugin disable/re-enable. Queued banners are cleared on leaving LOGGED_IN and on shutdown. No game inputs are automated; no alert data leaves the client. Existing sync and Hubs remain unchanged.

The branch is for manual in-game verification only. Do not submit to Plugin Hub until the owner confirms gameplay behavior and visual size.
