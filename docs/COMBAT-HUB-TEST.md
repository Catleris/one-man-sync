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
