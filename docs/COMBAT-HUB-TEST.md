# Combat Achievements Hub test

Run `OneManSyncPluginTest` in IntelliJ (Java 11). Tools → Combat Achievements, directly after Unlocks Hub. This test branch does not update the pending Plugin Hub submission.

- Log in; allow about 10 game ticks for account state. Compare total task count, points, reward threshold and completed tasks with the game's Combat Achievements interface.
- Open Filters, choose tiers/status/type; search task descriptions or boss names. Try an empty tier selection. Clear filters to restore all tasks.
- Expand Barrows and another boss. Check title wrapping, progress and descriptions at the normal narrow sidebar width. Boss counts are for all tasks at that boss; filters affect the expanded task list.
- Track a task; inspect Tracked; restart, then switch RS accounts. Tracking must restore only for that account. Logout must clear visible progress.
- Complete a task normally: its state and points should update without reopening the hub. Completed tracked tasks stay tracked until manually removed.
- Browse Tasks, load more, use Wiki. Verify existing Slayer, Farming, Clue, Monsters, Unlocks and settings remain accessible.

No combat overlays, mechanic warnings, prediction, input injection or network requests are added. Task names/descriptions and tier membership come from the client's game cache; completion uses task-ID packed varps (including group 20). Points and reward thresholds come from gameval CA varbits. Unavailable data is not presented as an incomplete task.

## Cache mapping reference

Enum/struct parameter mappings checked against `jimmyununu/combat-achievement-lookup` (`CombatTaskRepository`, `TaskTier`, `TaskType`); no plugin dependency or source code is bundled. RuneLite exposes no named gameval enum/param constants for these mappings. VarPlayerID/VarbitID constants are used wherever available.

The implementation is new Java/Swing code using OneMan's existing cards and fonts. The image concept is adapted to the actual sidebar width: compact header, two-column difficulty chips, one-column collapsible boss cards. It does not use generated screenshots as UI assets.

## 0.9.4 visual / scrolling regression check

- Open Filters and repeatedly toggle Easy, Medium and every other tier. The result list must return to its top after layout settles, rather than jump to its last card. Try the status/type controls and search too.
- Scroll partway down, then expand a boss or let a passive progress update arrive. The reading position should stay stable. Opening Filters itself should not move the result list.
- Verify distinct tier button fills/borders under the normal RuneLite theme: green Easy, blue Medium, red Hard, purple Elite, orange Master, gold Grandmaster. Selected tabs use gold.
- Boss cards display local highscore sprites or Slayer guide item icons. Check Aberrant Spectre, Abyssal Sire, Hydra and Barrows. Encounters without a mapped game portrait show a shield badge. No external image service is contacted.
- Task cards have tier badges; open boss cards have a gold outline and custom progress bars. Check long names at 242px sidebar width.

Read-only Combat Hub text disables automatic caret scrolling; filter results restore their viewport after queued Swing layout, with stale restorations discarded. Custom button/bar painting prevents RuneLite's look and feel from masking the selected colours. Icon requests are cached, detached labels are weakly held, and shutdown rejects stale callbacks.
