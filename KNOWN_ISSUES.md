# Known issues and pending checks

Updated: 2026-11-02. This is a short handoff list, not a claim that every historical report is reproducible on current master.

## Pending verification

| Item | Evidence / current status | Next check |
|---|---|---|
| Combat Achievement category icons | docs/COMBAT-ICON-FIX-TEST.md documents mappings and regression checks for previously reported icons. Current in-game appearance was not reverified in this task. | Follow the linked icon checklist on the current build, including Gauntlet variants and scroll/filter behavior; record owner results. |
| Shaman outline pulse visibility | docs/MONSTERS-HUB-TEST.md documents an optional cosmetic outline pulse, default Off, with Clear/Vivid presets. The document describes an older test branch; current behavior and owner acceptance need checking before treating this as fixed or broken. | Inspect current implementation first, then have the owner compare Off/On with nonzero outline opacity. |
| Plugin Hub distribution | No live manifest or review status was checked during this documentation update. | Verify the reviewed source commit and current PR before reporting that installed clients have an update. |

## Design limits, not bugs

- Shaman explosion countdowns are not an established feature. The documented pulse is cosmetic and is not tied to explosion time or order. Do not infer 1/2/3-second timers from it. See [timing decision and test notes](docs/MONSTERS-HUB-TEST.md).
- Build success, previews and source-level tests do not confirm real in-game behavior.
- Old test documents retain their original branch/version context; compare with current source before applying their instructions.

## Resolved / confirmed

No new runtime issue was resolved or tested by this documentation-only change. Add dated results here when evidence is available; do not silently delete unresolved reports.
