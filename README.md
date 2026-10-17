# OneMan Sync

Official source repository for the **OneMan Sync** RuneLite plugin used with [oneman.lt](https://oneman.lt/).

OneMan Sync is an opt-in companion plugin for the OneMan OSRS progression tracker. It sends your own account progression data to your OneMan profile so the website can maintain Roadmap, Chronicle, boss/Collection Log history, and bank-aware planning.

> **Plugin Hub status:** this repository is being prepared for RuneLite Plugin Hub review. It is not an official RuneLite or Jagex plugin unless/until it is accepted into the Plugin Hub.

## Clue Hub test build — 0.8.1-clue-test.3

Development branch: `dev/clue-scroll-hub`. This test does not update the official Plugin Hub build.
Choose **Clue Hub** in the existing OneMan sidebar selector (Overview / Slayer / Farming / Clue Hub).

- Grey Beginner, green Easy, blue Medium, red Hard, purple Elite, gold Master.
- Ground labels and item-menu names end with coloured tier dots and tier abbreviations. Inventory, equipment and bank icons use dots in the lower-right corner.
- `Req` means an item used in emote clues or beginner Charlie tasks. It does not mean the current clue is complete. All matching tiers and accepted RuneLite item alternatives are retained. Sherlock skill challenges are not indexed in this first test.
- `Reward table` means a possible reward source; it never proves the actual origin of an item. Common supplies such as runes/coins are hidden by default and can be enabled separately.
- An observed casket opening is recorded only when a server clue-completion message correlates with a fresh reward container; recent NPC drops are labelled separately. The session history is bounded and cleared on account/world changes. No new network service or sync setting is needed.
- Ground labels are a separate overlay; use **Clue ground label height** if they overlap RuneLite Ground Items labels.
- Item name / ID lookup is available in the hub. Bundled data attribution is shown in the panel; source details and licenses are in `src/main/resources/lt/oneman/sync/clue-data-LICENSE.txt`.

### Testing in the development client

Build/run with JDK 11: `./gradlew clean test shadowJar` then `./gradlew run` (Windows: `gradlew.bat`). The existing development profile is retained.
For Jagex accounts follow [RuneLite's development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

1. Disable website sync; Clue Hub must still work alongside Slayer and Farming.
2. Put a gold ring / bronze axe (Beginner) and an abyssal whip (Master) in inventory, then bank them. Check coloured dots and item menu suffixes. Leather chaps should have multiple tiers.
3. Drop and pick up a marked item, then obtain a marked NPC drop during Slayer or ordinary combat. Check name + dots, no stale ground label after pickup, and NPC source in the hub. Test with Ground Items enabled and disabled, multiple items on the same tile, region changes and hopping.
4. Open a casket; the hub should record **Opened [tier] casket (observed)** with the actual reward items. Reopening the reward interface must not duplicate an event; a Barrows reward must not be recorded as a clue reward.
5. Check Ranger boots (Medium) and Mole slippers (Beginner). An ordinary coin/rune stack should not show reward dots unless **Include common rewards** is enabled; noted versions should share the base item's classification.
6. Toggle settings while logged in, log out/switch account, and disable/re-enable the plugin. Check that no old session history or overlays leak across accounts or survive shutdown.

GitHub Actions produces `oneman-clue-hub-test`, containing the runnable development JAR, test source ZIP and test reports.

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

### Test build 2: Slayer task source and navigation

The hub now uses OneMan's direct game-DB/chat Slayer state even with website sync disabled. Unconfirmed/stale labels cannot choose a destination, and unknown assignments show a visible refresh message. Konar area filtering uses the same current assignment as sync. The sidebar shows the test build version above the hub selector; Farming Hub and Clue Scroll Hub share this selector. Slayer assignment/count/area are visible above destination controls, with narrow-panel alignment corrected.

### Test build 3: plugin injection failure

Fixes `Recursive load of: lt.oneman.sync.ClueHub.<init>()` during RuneLite plugin instantiation. Both clue overlays now obtain the singleton hub lazily through `Provider<ClueHub>`, breaking the eager Guice construction cycle. A dedicated Guice binding-graph regression test checks the hub and both overlays without starting the game. Keep the existing `dev-home` folder when replacing project source files. Restart with `gradlew.bat clean run` and verify the sidebar build label is `0.8.1-clue-test.3`.
