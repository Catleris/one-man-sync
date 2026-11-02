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
