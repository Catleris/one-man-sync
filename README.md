# OneMan Sync

Official source repository for the **OneMan Sync** RuneLite plugin used with [oneman.lt](https://oneman.lt/).

OneMan Sync is an opt-in companion plugin for the OneMan OSRS progression tracker. It sends your own account progression data to your OneMan profile so the website can maintain Roadmap, Chronicle, boss/Collection Log history, and bank-aware planning.

> **Plugin Hub status:** this repository is being prepared for RuneLite Plugin Hub review. It is not an official RuneLite or Jagex plugin unless/until it is accepted into the Plugin Hub.

## Clue Hub test build — 0.8.1-clue-test.8

**New map browsers:** Clue Hub → STASH filters all installed RuneLite STASH units by Beginner / Easy / Medium / Hard / Elite / Master. Coloured S pins appear on the world map; nearby same-floor units also get clipped minimap dots. The checkbox hides both OneMan STASH marker types. Farming Hub → Patches filters RuneLite's farming sites by type (Tree, Fruit Tree, Herb, Allotment, etc.). The current snapshot contains 7 regular-tree locations, including Auburnvale, and 10 herb locations. Nearby plots such as allotment pairs, hardwood trees and grape vines are grouped as locations, rather than counted as individual plots. RuneLite's standard map icons remain independent of OneMan's filters.

Open the world map and click a location in either list to centre the map. If it is closed, the selected target waits until it opens. Focus happens once, so subsequent manual panning remains free. Underground locations may require choosing the relevant map area. Locations do not prove account access/unlocks. Filters and marker visibility are saved locally. Farming herb/tree observations appear in selected-location details; birdhouse/contract information remains in Run status. Other farming types currently provide locations, not decoded crop timers. Source/attribution: `src/main/resources/lt/oneman/sync/map-data-LICENSE.txt`.

The sidebar fills the available height. Clue Hub separates Items, Options and About into compact tabs. The **OneMan Sync settings** button opens RuneLite's original OneMan configuration panel, including Sync Key and all Slayer/notification/Clue options. The temporary custom settings form has been removed. The Plugin Hub installer list is separate from this configuration panel.

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

Build/run with JDK 11: `./gradlew clean test shadowJar` then `./gradlew run` (Windows: `gradlew.bat`). The default launcher uses your normal RuneLite home/profile, including existing client display/scaling settings and OneMan configuration. Close the normal client before starting the development copy. To use the old isolated development home instead: `./gradlew run -Poneman.isolatedProfile` (Windows: `.\gradlew.bat run -Poneman.isolatedProfile`). Keep `dev-home` for that optional mode; no files are deleted or copied. The runnable JAR also uses the normal RuneLite home unless explicitly launched with a different `-Duser.home`.
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

Developer launch: `./gradlew run` (`./gradlew.bat run` on Windows) now uses the normal RuneLite profile. Optional isolated mode: `./gradlew run -Poneman.isolatedProfile`; this continues to use the preserved `dev-home` directory. See [Lithuanian setup and test checklist](SLAYER_LAB_LT.md).

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

### Plugin startup validation (test build 4)

Fixes `Recursive load of: lt.oneman.sync.ClueHub.<init>()` during RuneLite plugin instantiation. Both clue overlays are injected independently and connected to their hub only after injection completes; shutdown detaches both references. There is no Guice-injected hub back-reference, including through a Provider.

The full plugin bootstrap regression test mirrors RuneLite PluginModuleFactory: bind the real plugin instance and install its real configuration provider in a child injector. Only RuneLite services are test doubles. It runs the plugin lifecycle, constructs the actual Swing sidebar, checks Overview / Slayer / Farming Hub / Clue Scroll Hub plus the build label, checks configuration availability, and verifies sidebar/overlay cleanup. It does not start or control the game. Simpler graph tests alone do not prove plugin initialization works.

Keep the existing `dev-home` folder for optional isolated launches. The default launch uses your normal RuneLite home. Restart with `gradlew.bat clean run` and verify the sidebar build label is `0.8.1-clue-test.8`.

### Event registration validation (test build 5)

All @Subscribe entry points follow RuneLite's mandatory `on<EventClass>` naming contract. The four clue event forwards are `onItemSpawned`, `onItemDespawned`, `onMenuEntryAdded` and `onConfigChanged`. Both an isolated subscriber-contract test and the full bootstrap test register the real plugin with RuneLite's real EventBus; startup tests that omit registration cannot catch this failure. The development launcher prints the loaded OneMan version before starting RuneLite.

### Native settings and client display (test build 7)

The always-visible settings button posts RuneLite's public RUNELITE_OVERLAY_CONFIG action with the real owning plugin. The active ConfigPlugin opens its own complete configuration panel; no reflection, private UI access or duplicate form is used. The bootstrap test clicks the actual Swing button through a real EventBus and verifies RUNELITE_OVERLAY_CONFIG with the exact loaded OneMan plugin as owner. This checks the supported request contract; the native configuration UI and game rendering still require user confirmation. A separate local smoke check of the real ConfigPlugin lifecycle/subscriber with UI doubles also passed. Test helpers must stay outside RuneLite packages, because dependency JARs are signed.

Earlier launchers forced a fresh user.home and therefore could not inherit the installed client's Stretched Mode, scaling, plugin preferences or stored OneMan settings. Default launches now keep the JVM's normal home. This restores access to the same saved client configuration; it does not reset game widgets, minimap positions, camera or account settings. Rendering must be confirmed by the user.
