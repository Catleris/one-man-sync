# Project status

Updated: 2026-11-02. Repository: Catleris/one-man-sync, branch master.
Baseline inspected: `e8d07cd5936ea9726c894b1e35358f78527ed58c`.

## Current source

- build.gradle declares version 0.11.1 and Java 11 bytecode.
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

Version 0.11.2-hubs-test.4 adds compact superior-kill/elapsed/notable-loot session card in the existing Loot tab, recent-three display/last-five source tooltip and profile-guarded Reset session. Attributed loot only; spawn messages do not count, pickup and RDT provenance are not inferred. Session continues across tasks and clears on logout/profile/plugin changes. Added seven model regression tests and docs/SLAYER-SESSION-DROPS-TEST.md. Combined CI pending; all owner gameplay checks remain pending.
