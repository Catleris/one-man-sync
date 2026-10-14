# OneMan Sync

Official source repository for the **OneMan Sync** RuneLite plugin used with [oneman.lt](https://oneman.lt/).

OneMan Sync is an opt-in companion plugin for the OneMan OSRS progression tracker. It sends your own account progression data to your OneMan profile so the website can maintain Roadmap, Chronicle, boss/Collection Log history, and bank-aware planning.

> **Plugin Hub status:** this repository is being prepared for RuneLite Plugin Hub review. It is not an official RuneLite or Jagex plugin unless/until it is accepted into the Plugin Hub.

## Slayer and Prayer tools (0.6)

OneMan Slayer Lab is now part of this single plugin. The S sidebar contains preparation, static tactics, locations, travel, supplies, session, loot, history and Prayer resource tabs. These tools work with website sync disabled. They do not predict attacks or select/switch prayers. Existing Lab preferences are migrated once; per-account bank snapshots, goals and journals retain their original profile storage.

Developer launch: `./gradlew run` (`./gradlew.bat run` on Windows). Keep your existing `dev-home` directory when updating to preserve login and local account data. See [Lithuanian setup and test checklist](SLAYER_LAB_LT.md).

## Data sent when sync is enabled

The plugin can send the following to `https://oneman.lt/runelite_sync.php`:

- RuneScape display name
- Skill levels and XP
- Quest states
- Achievement Diary states
- Completed Combat Achievement tasks and tier state
- Boss kill counts
- Collection Log observations and new unlock events
- Pet events, personal-best messages, and recent tracked boss loot
- A bank snapshot when the bank is opened: item ID, item name, and quantity
- Clue Scroll STASH states: STASH key/object ID and whether each STASH is built and filled
- Current inventory and worn equipment snapshot
- Selected storage snapshots when viewed/available: Seed Vault, Looting Bag, Rune Pouch, Group Storage and POH Costume Room categories
- Slayer state: current stored task, remaining amount, points, streak and unlock/block bitfields
- POH feature observations when house objects are loaded (teleports, pools, altar, jewellery box and similar facilities)
- Selected account currencies and daily-state data such as NMZ/Tithe/Slayer points, Kingdom approval/coffer, Bird House tracker timestamps and Farming Contract state
- The user's OneMan Sync Key as a bearer token for authenticating the request

As with any connection to a third-party server, the server also receives the user's IP address as part of the HTTPS request.

The plugin does **not** request or read a Jagex password, Jagex account credentials, authenticator code, or bank PIN.

## Privacy and opt-in

- Sync is **disabled by default**.
- Enabling sync shows RuneLite's required third-party-server warning.
- The destination is fixed to `https://oneman.lt`; there is no configurable arbitrary upload endpoint.
- The OneMan Sync Key is stored as a RuneLite secret configuration value.
- Network calls use RuneLite's injected `OkHttpClient` asynchronously via `enqueue()` and are not executed on the client thread.
- You can revoke RuneLite devices/keys or deactivate your OneMan account from the website.

Plugin-side disclosure: [PRIVACY.md](PRIVACY.md)

Website privacy policy: https://oneman.lt/privacy.php

Website terms and copyright: https://oneman.lt/terms.php

## Development

This repository follows the RuneLite example-plugin layout, uses `build=standard`, and targets Java 11 bytecode.

The development runner is `lt.oneman.sync.OneManSyncPluginTest`.

Run locally with the Gradle `run` task after following RuneLite's Jagex Account development-login instructions.

## License

BSD 2-Clause. See [LICENSE](LICENSE).

## 0.8.0 — assigned Slayer destinations and Farming

- Konar's assigned area stays visible even when the monster/location catalogue is incomplete. Known entrances have map controls; unmapped areas retain a Location guide without invented coordinates. General alternatives are only shown for tasks without an assigned area.
- The shared OneMan sidebar now has Overview, Slayer and Farming sections. Farming reads profile-scoped RuneLite Time Tracking observations for 10 herb and 6 standard tree patches, four birdhouses and Farming contracts. Keep Time Tracking enabled and visit patches to record observations.
- Growing crop readiness is an estimate; visit the patch to confirm health and harvestability. Missing observations stay Unknown. Diseased/dead crops and weeds never become ready through elapsed time.
- Optional local birdhouse notifications are off by default, reset across profiles/logout and suppress notifications for houses already ready at the first observation.
- Daily farming data is included in the existing opt-in OneMan sync. No separate endpoint or additional network opt-in is introduced.

Validation: all production and test sources compiled for Java 11 against the supplied RuneLite test runtime using ECJ; 69 JUnit tests pass. Gradle verification runs in GitHub Actions. In-game verification remains pending: check a Konar assignment, mapped/unmapped destinations, Farming observations after visiting a patch, and logout/profile switching. Plugin Hub availability requires its own reviewed manifest update.
