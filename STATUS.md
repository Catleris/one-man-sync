# Project status

Updated: 2026-11-02. Repository: Catleris/one-man-sync, branch master.
Release preparation: PR #10, OneMan Sync 0.11.2.

## Current source

- build.gradle and the sidebar declare version 0.11.2 and Java 11 bytecode.
- Latest inspected commit adds shared Back to Tools navigation in OneManCompanion.java and corresponding bootstrap assertions in OneManPluginBootstrapTest.java. Assertions exist in source; they were not executed during this documentation task.
- README.md describes opt-in Collection Log/Combat Achievement/Diary notifications and original quest reward-window styling. It records owner confirmation for the 0.11.0 development release; this is historical documentation, not a fresh test of the current head.
- GitHub Actions is configured to run `./gradlew clean build shadowJar` on master pushes and master pull requests. Current run results were not checked in this task.
- Source publication does not establish Plugin Hub availability. No current Plugin Hub manifest/review status was verified here.

## This change

Added BRANDING.md and README links covering attribution, permission requests and separation of official OneMan identity from independent forks. BSD LICENSE and all existing third-party/asset grants are unchanged. Contact before substantial code reuse is explicitly voluntary; no permission gate or tracking was added to the plugin.

Validation: checked the existing LICENSE, repository guidance and RuneLite Plugin Hub licensing instructions. Reviewed policy links and wording for consistency with BSD. Documentation only; no runtime build/game test needed. Trademark registration, legal enforceability and asset ownership were not established by this task.

## Next step

At the next authorized feature/fix, inspect current code and the relevant checklist, run appropriate checks, and record the exact outcome here. For in-game changes, list specific owner checks and wait for the result before marking behavior confirmed.

Related details: [known issues](KNOWN_ISSUES.md), [notification/quest validation](docs/LEGENDARY-ALARMS-TEST.md), [icon checklist](docs/COMBAT-ICON-FIX-TEST.md), [Shaman notes](docs/MONSTERS-HUB-TEST.md).

## Smart Slayer preparation MVP · 2026-11-02

Implemented on `dev/slayer-smart-bank-prep` from master `51ad900`: compact special-item status rows, required/utility bank outlines, carried/equipped alternative suppression, charged-spray filtering, and elite Kourend diary boot exemption. Does not change master publication or Plugin Hub pin. Added domain regression tests and docs/SLAYER-SMART-BANK-TEST.md.

Validation: `git diff --check` passed. Java 11 GitHub Actions run https://github.com/Catleris/one-man-sync/actions/runs/41499014838 passed `./gradlew clean build shadowJar` on code commit `6e5d8abeba748f592f002a737af860e8e6cbbc50`, including nine new regression tests. Development artifact uploaded successfully; test version `0.11.2-slayer-test.1`. Local Gradle could not download its distribution due to workspace network restrictions. Owner game checks remain pending in docs/SLAYER-SMART-BANK-TEST.md; draft PR https://github.com/Catleris/one-man-sync/pull/10 .

## Unrestricted Slayer locations · 2026-11-02

Owner screenshot confirms Lizardmen has source locations but an empty destination selector without a Konar assigned area. The catalog now retains every named source location as a selectable reference, reuses verified entrances where available, and keeps unverified locations without invented map pins. Assigned-area filtering remains authoritative. Added catalog-wide source-location coverage plus Lizardmen/singular/assigned-area/Wilderness regression checks. Version 0.11.2-slayer-test.2. Java 11 clean build/test/shadowJar passed on code commit d5a3e7221e2368e0da3d9e8aa373acb9aefae583: https://github.com/Catleris/one-man-sync/actions/runs/41503842167 . Artifact upload succeeded. Both the item MVP and location fix remain untested in-game.

## Farming health and compost · 2026-11-02

Version 0.11.2-hubs-test.3 adds observed herb/tree disease/death summary and patch colors, optional deduplicated local notifications, and session bottomless-bucket Check/fill/remaining/empty observations with configurable refill threshold. Unknown and multiple buckets never imply empty. Keeps prior Slayer work on the development branch. Added seven focused state/protocol tests and docs/FARMING-HEALTH-TEST.md. Java 11 clean build/test/shadowJar passed on code commit e0469b9522a473e8484598830c4ef16c9fbed895 (including seven new Farming tests): https://github.com/Catleris/one-man-sync/actions/runs/41505449762 . Artifact upload succeeded. Owner game checks remain pending for the combined update.

## Slayer Session drops · 2026-11-02

Version 0.11.2-hubs-test.4 adds compact superior-kill/elapsed/notable-loot session card in the existing Loot tab, recent-three display/last-five source tooltip and profile-guarded Reset session. Attributed loot only; spawn messages do not count, pickup and RDT provenance are not inferred. Session continues across tasks and clears on logout/profile/plugin changes. Added seven model regression tests and docs/SLAYER-SESSION-DROPS-TEST.md. Combined Java 11 clean build/test/shadowJar passed on d28bacc8dc3d745b6c808ccce823be65c9f37f6c, including seven new Session drops tests: https://github.com/Catleris/one-man-sync/actions/runs/41509955333 . Artifact upload succeeded. All owner gameplay checks remain pending.

## Release submission · 2026-11-02

Owner explicitly authorized merging PR #10 and submitting the combined release to RuneLite review without an owner game test. Release notes: docs/RELEASE-0.11.2.md. Functional code passed Java 11 run 41509955333; release-version Java 11 clean build/test/shadowJar passed (run 41512072580). Owner game checks remain pending and must not be represented as passed. Previous Plugin Hub PR #19815 is merged; a new manifest update is required.

PR #10 merged into master as 49bcc90d059661e18f92282a3c6445e54e5b16ea. Plugin Hub fork branch `Catleris/plugin-hub:update-one-man-sync-0.11.2` pins that exact commit and changes only plugins/one-man-sync. Initial upstream PR creation returned integration HTTP 403. With the owner's explicit browser-fallback permission, submitted https://github.com/runelite/plugin-hub/pull/19973 (open, ready for review), pinning 49bcc90d059661e18f92282a3c6445e54e5b16ea. Full change summary, CI evidence and untested-in-game status are included. Maintainer branch/secrets access is disabled. RuneLite review/publication remains pending.

## Collection Log duplicate popup fix · 2026-11-04

Owner reported the native Collection Log popup showing behind the custom OneMan banner. Native popup suppression previously covered Combat Tasks only. Shared suppression now covers Collection Log when an enabled, real current/queued OneMan collection notice replaces it. Previews and unrelated native notifications remain untouched; disabling, clearing, logout and shutdown restore the widget. Added collection suppression/restoration and real-versus-preview queue regression tests. Java 11 `./gradlew clean build shadowJar` passed on d883124be6f1a32650098fff6e722a90a5af975b (run https://github.com/Catleris/one-man-sync/actions/runs/41787835899), including the focused regression tests; development artifact uploaded. Local Gradle distribution download was blocked by workspace networking. PR: https://github.com/Catleris/one-man-sync/pull/11. Owner game verification pending. No new Plugin Hub submission for this fix yet.

## Collection Log fix release submission · 2026-11-04

Owner authorized the 0.11.3 release and RuneLite submission. Release-version Java 11 clean build/test/shadowJar passed: https://github.com/Catleris/one-man-sync/actions/runs/41788334454 . Source PR #11 merged as 284c81fa90843acf404b329eca070ade7ab6e094. Submitted https://github.com/runelite/plugin-hub/pull/20096 using the owner-authorized browser fallback after the integration returned HTTP 403. The manifest changes only plugins/one-man-sync and pins that merge commit; warning unchanged. PR is open for RuneLite review, not yet approved/distributed. Owner in-game retest remains pending.

## Target info inventory-side trial · 2026-11-04

Implemented on dev/target-info from master 22f2726, version 0.11.4-target-test.1. Opt-in movable compact/expanded reference card by open inventory, current NPC interaction only, optional active-Slayer-match filter. No network calls, live combat calculations or mechanics predictions. Exact transformed NPC IDs from a bundled Monster Stats snapshot; 3,974 unique IDs retained and 74 conflicting IDs omitted. Data attribution/upstream BSD license preserved in target-info-LICENSE.txt. Unknown/non-scalar fields remain unknown; tied melee bonuses are explicit. Existing 0.11.3 Plugin Hub submission is unchanged. Added focused controller/catalog/rendering tests and docs/TARGET-INFO-TEST.md. Java 11 `./gradlew clean build shadowJar` passed on 82cd74074860c9bb69fb778d236bf790410e4f99, run https://github.com/Catleris/one-man-sync/actions/runs/41792634099, including all six new target-info tests and existing regression checks. Development JAR and source artifact uploaded. Draft PR: https://github.com/Catleris/one-man-sync/pull/12. Owner game verification pending.

## Inventory target info redesign · 2026-11-04

Owner screenshot rejected the floating test.1 card and confirmed it disappears after combat. Test.2 replaces it with an inventory-bound Items / Target info page, four stat icons, Details/Less, Clear, and a small reopening i below the native item grid plus a Monsters Hub Open inventory info button. The last NPC snapshot persists after interaction end, death and loading/teleport; session end and disable clear it. Item grid/widgets are not moved or resized; local mouse hooks block only clicks on the information page and its controls. Underlying item menus are suppressed only within that page. Hooks are removed on shutdown. Updated regression tests cover retention, tab switching, inventory bounds, lifecycle and click protection. Java 11 clean build/test/shadowJar passed on 18aea45a2dde411b26f0b42f474e2b28f10ee460 (run https://github.com/Catleris/one-man-sync/actions/runs/41871982510), all 188 tests passed and development source/JAR artifact uploaded. Initial CI exposed missing test-service bindings; those fixtures were corrected. Owner fixed/resizable/stretched layout and click-protection checks remain pending. Still a draft development trial, separate from Plugin Hub 0.11.3.

## Target info appearance and side dock · 2026-11-04

Owner test.2 screenshots confirm the inventory page renders; reported the tiny lower-rim i overlaps a native tab and requested a larger side control and better styling. Test.3 replaces it with a 42x50 book/Info dock at the inventory side (left preferred, right fallback, hidden if neither fits). Native item rectangle and top/bottom tab rows stay untouched. Refined green/gold gradients, grouped target header, larger regular-font primary values, framed stat tiles, alternating detail rows and larger footer controls. Last-target retention and click protection preserved. Added dock geometry regression checks. Changed sources compiled with Java 11 release compatibility using local JDK 17 compiler; all nine focused target-info/rendering/dock tests passed locally on JDK 17. Full Java 11 GitHub Actions run 41879083630 is queued; owner appearance/layout confirmation pending.

## Farming seed menu · 2026-11-05

Added an opt-in Farming Hub entry beside Examine for catalogued planting items in inventory and bank. It opens the exact seed in the existing Seeds tab. The development branch starts from master 22f2726, which includes the prior Slayer and Farming release. Changed Java sources compiled with release 11 compatibility against the existing development dependencies. All 12 focused Farming tests passed locally. Owner inventory and bank checks remain pending.

## Birdhouse ready alerts · 2026-11-05

Owner confirmed the Farming seed menu works. Birdhouse alerts now use a desktop notification and optional RuneLite sound, including while focused. Houses that become ready in the same refresh share one notification. The existing enable setting and ready-transition baseline are retained. Changed sources compiled with Java 11 compatibility and 12 focused Farming tests passed locally. Sound and desktop delivery await the owner’s in-game check.

## Combined hub trial · 2026-11-05

Combined Target Info test.3 with the Farming seed menu and Birdhouse ready notifications in 0.11.4-hubs-test.1. Combined CI and in-game checks pending.

## Target Info manual opening · 2026-11-05

Owner verified the combined test.1 inventory view with Blue dragon and reported that it covered the inventory during combat. In test.2, combat updates the target snapshot while the inventory stays on Items until explicitly switched. The side control is a compact 32 × 36 icon with a book for opening and backpack for returning to items. Combined test.1 CI passed in run 42064473731. Test.2 CI and owner game checks pending.
