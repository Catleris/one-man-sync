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

Adopted the three-file workflow: extended existing AGENTS.md without removing RuneLite guidelines; added this status file and KNOWN_ISSUES.md. No runtime code, assets or build settings changed.

Validation: reviewed the existing guidelines, README, build configuration, CI workflow and referenced issue checklists. No build or in-game test was run for these documentation-only edits.

## Next step

At the next authorized feature/fix, inspect current code and the relevant checklist, run appropriate checks, and record the exact outcome here. For in-game changes, list specific owner checks and wait for the result before marking behavior confirmed.

Related details: [known issues](KNOWN_ISSUES.md), [notification/quest validation](docs/LEGENDARY-ALARMS-TEST.md), [icon checklist](docs/COMBAT-ICON-FIX-TEST.md), [Shaman notes](docs/MONSTERS-HUB-TEST.md).
