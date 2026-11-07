# Known issues and pending checks

Updated: 2026-11-02. This is a short handoff list, not a claim that every historical report is reproducible on current master.

## Pending verification

- 2026-11-02: Session drops test.4 passed Java 11 CI (run 41509955333) and awaits owner checks in docs/SLAYER-SESSION-DROPS-TEST.md. Superior names/notable items are curated; attributed loot does not verify pickup or RDT origin.

- 2026-11-02: Farming health/compost test.3 passed Java 11 CI (run 41505449762) and awaits owner checks in docs/FARMING-HEALTH-TEST.md. Disease is last observed through Time Tracking, bucket amount is last reported; neither predicts unobserved changes.

- 2026-11-02: Owner screenshot reproduced missing unrestricted Lizardmen choices. Source-location fallback implemented in test.2; Java 11 CI passed (run 41503842167), owner retest pending. Reference-only destinations intentionally disable map tracking but allow Location guide.

- 2026-11-02: Smart Slayer bank MVP requires owner checks in [Slayer test checklist](docs/SLAYER-SMART-BANK-TEST.md). Bank status uses a dated per-profile snapshot; generic dragonfire loadouts are outside this MVP.


| Item | Evidence / current status | Next check |
|---|---|---|
| Combat Achievement category icons | docs/COMBAT-ICON-FIX-TEST.md documents mappings and regression checks for previously reported icons. Current in-game appearance was not reverified in this task. | Follow the linked icon checklist on the current build, including Gauntlet variants and scroll/filter behavior; record owner results. |
| Shaman outline pulse visibility | docs/MONSTERS-HUB-TEST.md documents an optional cosmetic outline pulse, default Off, with Clear/Vivid presets. The document describes an older test branch; current behavior and owner acceptance need checking before treating this as fixed or broken. | Inspect current implementation first, then have the owner compare Off/On with nonzero outline opacity. |
| Plugin Hub distribution | 2026-11-02: PR #19815 is merged; live manifest pins e8d07cd. New 0.11.2 fork manifest pins source merge 49bcc90. Upstream PR #19973 is now open after owner-authorized browser submission. | Await maintainer approval and publication; do not claim 0.11.2 is distributed. |

## Design limits, not bugs

- Shaman explosion countdowns are not an established feature. The documented pulse is cosmetic and is not tied to explosion time or order. Do not infer 1/2/3-second timers from it. See [timing decision and test notes](docs/MONSTERS-HUB-TEST.md).
- Build success, previews and source-level tests do not confirm real in-game behavior.
- Old test documents retain their original branch/version context; compare with current source before applying their instructions.

## Resolved / confirmed

No new runtime issue was resolved or tested by this documentation-only change. Add dated results here when evidence is available; do not silently delete unresolved reports.

## Collection Log popup duplication · 2026-11-04

Owner reports native and OneMan Collection Log popups overlap. Fix implemented in PR #11 on fix/collection-popup-duplicate; Java 11 clean build/test/shadowJar passed (run 41787835899, source d883124). In-game retest pending. Verify a new real collection entry shows one OneMan banner; disabling OneMan Collection Log keeps the native notification, and preview does not suppress it.

- 2026-11-04 release status: Collection Log duplication fix 0.11.3 merged via source PR #11 (284c81fa90843acf404b329eca070ade7ab6e094); release-version Java 11 build/tests passed (run 41788334454). Plugin Hub PR https://github.com/runelite/plugin-hub/pull/20096 submitted and awaiting review. In-game retest pending.

## Target info trial · 2026-11-04

Development-only 0.11.4-target-test.1 requires owner inventory-layout, switching/death and Slayer-filter checks in docs/TARGET-INFO-TEST.md. Static source bonuses are not live weakness/DPS calculations; scaled/drained/phase stats are not inferred. Missing/conflicting IDs display unavailable. Card defaults beside the inventory and can be Alt-dragged; it does not replace the game's inventory tab. Java 11 clean build/test/shadowJar passed (run 41792634099, source 82cd740); owner game/layout checks pending.

- 2026-11-04 owner trial: test.1 floated beside chatbox and disappeared after combat; rejected layout. Test.2 implements inventory-bound page and last-target retention. Java 11 clean build/test/shadowJar passed, 188 tests (run 41871982510, source 18aea45). Owner must confirm lower-rim i placement, item click protection, tab/bank access and retention on fixed/resizable/stretched layouts before release.

- 2026-11-04 owner screenshot: test.2 lower-rim i overlaps native bottom tab. Fixed in test.3 with larger side dock and geometry checks; nine focused tests passed locally with Java 11-compatible compilation on JDK 17; full Java 11 CI queued (run 41879083630) and owner layout retest pending. If no side fits on the canvas, use Monsters Hub Open inventory info.

## Target Info shield click with Stretched Mode · 2026-11-06

A player reported that the visible shield would not open. Target Info previously registered ahead of RuneLite's coordinate translator when enabled after Stretched Mode. The fix registers after the translator and includes a scaled-click regression test. Player retest pending.

## Minimized Gemstone Crab alerts · 2026-11-06

The owner saw the green flash but heard no sound and saw no desktop notification while RuneLite was minimized. The trial replaces the system beep with bundled audio and requests taskbar attention. Minimized playback and Windows toast behavior are awaiting an in-game retest.

## OneMan Hub local-only transition · 2026-11-07

Local compilation passed after removing networking. Full Java 11 CI and owner checks are required for migrated settings, Slayer state, bank observations, farming timers, monster references and local sounds. The installed Plugin Hub warning remains until maintainers approve the manifest change.
