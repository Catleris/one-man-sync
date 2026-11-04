# Target info inventory-side trial

Version: 0.11.4-target-test.1. Branch: dev/target-info.
This is a development trial; it is not part of the 0.11.3 Plugin Hub submission.

## Try it

1. Checkout dev/target-info, then launch `./gradlew run` (Windows: `.\gradlew.bat run`).
   Jagex Accounts: https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts .
2. Tools > Monsters Hub > enable **Target info by inventory (beta)**, or use OneMan Sync settings > Monsters / Lizardman Shaman.
3. Keep the inventory tab open and fight an ordinary NPC. The compact gold/dark card defaults to the bottom-right viewport beside the inventory. Alt-drag to place it where comfortable. Right-click the card > Toggle details, or use Expanded target stats in Monsters Hub.
4. Switch NPCs; confirm the name and stats change. End the interaction/kill the NPC; confirm the card disappears. Try a Player target: no NPC card should appear.
5. Toggle Expanded target stats. Verify the card stays readable in fixed/resizable/stretched layouts; inventory items and game tabs remain usable. Close inventory, open bank, teleport, hop and log out: card must disappear. Re-enable after disabling the plugin: no stale NPC should remain.
6. Enable Only my Slayer task monsters. Check a matching and unrelated NPC, then finish the task. Unknown task matches stay hidden. Website sync is not required.
7. Confirm Shaman controls, Collection Log replacement and existing hub navigation still work.

## Reading the card

- Element is the dataset's listed weakness/percent, not a live damage multiplier calculation. Not listed means the snapshot has no usable elemental record.
- Melee bonus identifies the lowest of stab/slash/crush defence bonuses only. Equal bonuses is a tie. It does not compare defence rolls, spells, gear, prayers, immunities or DPS.
- Expanded rows are static reference defence bonuses, defence/magic levels, base HP and base attack speed. They do not track drains, scaling, phases, next attacks or mechanics. Ranged L / S / H means light / standard / heavy.
- Exact transformed NPC ID lookup; no name-based stats guesses. Missing IDs show Reference stats: Unavailable. Ambiguous IDs and non-scalar values are omitted/unknown.
- Data snapshot: 4 Nov 2026, derived from Koitere/monster-stats CSV (source and upstream license in target-info-LICENSE.txt). No runtime download. No upstream plugin code or artwork reused.

## Validation

Focused tests cover exact transformed-ID changes, unknown variants, death/interaction end, disable/loading/shutdown, player exclusion, Slayer matching, inventory visibility and compact/expanded rendering. Automated Java 11 CI result and owner game feedback are recorded in STATUS.md. Automated tests are not game confirmation.
