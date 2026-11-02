# OneMan Sync 0.11.2

- Smart Slayer bank preparation: orange required-item / blue utility hints; carrying or equipping a suitable alternative clears duplicate hints. No generic gear highlights for ordinary tasks. Includes helmet component protection, charged spray filtering and Kourend elite floor-boot exemption.
- Unrestricted Slayer destinations: retain named source locations across 118 tasks, including Lizardmen. Known entrances keep map controls; reference-only destinations keep guide links without invented pins. Konar assigned-area filtering stays authoritative.
- Slayer Session drops: superior kills from attributed NPC loot, elapsed time, curated notable loot with quantity/source, bounded recent history and Reset session in the existing Loot tab. Spawn messages do not count; pickup and RDT provenance are not inferred. Resets on logout/account/plugin changes and continues across tasks.
- Farming health: observed diseased/dead herb/tree summary and red patch states from RuneLite Time Tracking; optional deduplicated desktop alerts.
- Bottomless compost reminder: last explicitly reported quantity/age, unknown/multiple-bucket handling, configurable at/below refill threshold (default 50) and optional local alert. Uses Check/fill/remaining/empty observations, not inferred consumption.

Existing birdhouse timers, map controls, loot goals and local tools remain available with website sync disabled. No new server endpoints or data uploads are introduced; existing network opt-in and manifest warning are unchanged. New desktop notifications default off.

Validation: functional Java 11 clean build/test/shadowJar passed in https://github.com/Catleris/one-man-sync/actions/runs/41509955333 including the new Slayer item, location, Farming health and Session drops regressions. Owner has not tested these changes in-game and explicitly requested submission for maintainer review. Automated checks are not gameplay confirmation.

Manual checklists: SLAYER-SMART-BANK-TEST.md, FARMING-HEALTH-TEST.md and SLAYER-SESSION-DROPS-TEST.md in this directory.
