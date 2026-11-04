# Target info inventory-side trial

Version: 0.11.4-target-test.2. Branch: dev/target-info.
This is a development trial; it is not part of the 0.11.3 Plugin Hub submission.

## Try it

1. Checkout dev/target-info, then launch `./gradlew run` (Windows: `.\gradlew.bat run`).
   Jagex Accounts: https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts .
2. Tools > Monsters Hub > enable **Target info in inventory (beta)**, or press **Open inventory info**. Keep the game's Inventory tab open. The information page is drawn inside the item area, with Items / Target info tabs; it no longer floats beside the chatbox.
3. Attack an NPC. Confirm its name, elemental record and Stab/Slash/Crush/Magic icon/bonus row update. Stop attacking, run away, kill it and teleport: the last selected NPC reference must remain available. Attack another NPC: its reference replaces the previous one.
4. Press **Items** to return to the normal bag. Click the small **i** at the lower-right rim under the item grid to reopen Target info, or use Open inventory info in Monsters Hub. Verify the i fits in the rim in fixed, resizable and stretched layouts. If it does not fit, use the hub button and report the layout.
5. In Target info, press Details / Less. Press Clear: the selected NPC disappears until a new interaction begins. No selected NPC shows a short instruction instead of guessing data.
6. CRITICAL: click and right-click where food/equipment sits under the info page; no item must be eaten, equipped or dragged, and no hidden item menu/tooltip should appear. Switch to Items and confirm ordinary item actions work. A drag already in progress must not become covered by the info page. Check other game tabs and bank access are unchanged.
7. Enable Only my Slayer task monsters. Check matching/unrelated NPCs and task completion; unknown task matches stay hidden. Website sync is not required. Switching other game tabs hides the page, returning to inventory restores it. Logging out/hopping/plugin shutdown clears the selected reference and removes input hooks.
8. Confirm Shaman controls, Collection Log replacement and existing hub navigation still work.

## Reading the card

- Element is the dataset's listed weakness/percent, not a live damage multiplier calculation. Not listed means the snapshot has no usable elemental record.
- Melee bonus identifies the lowest of stab/slash/crush defence bonuses only. Equal bonuses is a tie. It does not compare defence rolls, spells, gear, prayers, immunities or DPS.
- Expanded rows are static reference defence bonuses, defence/magic levels, base HP and base attack speed. They do not track drains, scaling, phases, next attacks or mechanics. Ranged L / S / H means light / standard / heavy.
- Exact transformed NPC ID lookup; no name-based stats guesses. Missing IDs show Reference stats: Unavailable. Ambiguous IDs and non-scalar values are omitted/unknown.
- Data snapshot: 4 Nov 2026, derived from Koitere/monster-stats CSV (source and upstream license in target-info-LICENSE.txt). No runtime download. No upstream plugin code or artwork reused.

## Validation

Focused tests cover exact transformed-ID changes, unknown variants, retention after death/interaction end, disable/loading/shutdown, player exclusion, Slayer matching, inventory visibility and inventory-bound rendering and click protection. Automated Java 11 CI result and owner game feedback are recorded in STATUS.md. Automated tests are not game confirmation.
