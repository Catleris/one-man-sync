# Monsters Hub: Lizardman Shaman test

Base: `master` at `250aa049806ad3b6fbb5b86c8b0739dd0725317b` (0.8.2 Superior alert fix).
Test version: `0.8.5-settings-test`. No Plugin Hub submission or master merge is part of this test.

## Implemented

Select **Monsters Hub** in the existing sidebar Hub dropdown, or click the permanent **Monsters Hub** button immediately underneath it, then select **Lizardman Shaman**.
The profile defaults to Off. It works with or without a Slayer assignment and without website sync.
Toggle current minion tiles, model outlines, silhouette fill and the overhead **Spawn** label independently. Choose separate tile and model colours. Adjust tile, outline/label and fill opacity with separate 0–100% sliders in the Hub or native settings. Native settings also offer outline width (1–6 pixels).
The native colour setting also supports alpha.
A confirmed, active Lizardmen assignment suggests this profile but never enables or switches it automatically.
The suggestion is not proof a Shaman kill satisfies a Konar location requirement.

Only `NpcID.ZEAH_LIZARDSHAMAN_SPAWN` is tracked, by NPC object identity.
Spawns/despawns/composition changes maintain the collection; GameTick publishes current occupied tile positions.
A one-time loaded-NPC list scan restores already visible minions after enabling, login or scene loading.
There are no per-tick scene scans. Despawn, scene loading, logout/hop, profile Off, all visual toggles Off and plugin shutdown clear tracking. Tiles Off hides only tiles, leaving enabled model/label features working.
Model outlines use RuneLite ModelOutlineRenderer; coloured fill uses the NPC convex hull silhouette (not a texture or model replacement). The plain label says only Spawn.
Rendering uses local coordinates in the same world view, including instanced local coordinates.
Chambers of Xeric is explicitly excluded via `VarbitID.RAIDS_CLIENT_INDUNGEON`; raid blocker Spawn IDs are not included.

## Rules and timing decision (checked 2026-10-21)

Primary policy sources:

- [Jagex Third Party Client Guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1): boss-mechanic start/end indicators, attack prediction and automatic stand/avoid indicators are restricted. This does not establish a specific ruling on ordinary Shaman minion countdowns.
- [RuneLite Rejected or Rolled Back Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features): restrictions also cover raids and existing plugins are not proof that a new feature is permitted.
- [RuneLite gameval NPC definitions](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/gameval/NpcID.java): separates the ordinary Shaman Spawn and raid blocker IDs.

Mechanic reference:

- [OSRS Wiki Shaman strategies](https://oldschool.runescape.wiki/w/Lizardman_shaman/Strategies) describes three summoned minions, two beside the player and one underneath, with the one underneath exploding first. The retrieved description gives no exact per-minion tick durations or authoritative server-side countdown trigger.

**No explosion countdown is implemented.** The exact server trigger (summon action, NPC creation, movement/proximity, or another event) could not be verified from reliable current sources. The relative ordering in the strategy description does not establish fixed delays between minions. We cannot conclude whether or when durations vary.
`NpcSpawned` means the client has observed an NPC; it does not expose the server's detonation deadline, and a minion may already exist when the player loads the area or enables the helper. NPC index/list order cannot identify first/second/third explosion order.
No fabricated 1/2/3-second offsets, countdown values, or seconds/ticks toggle are provided. There is no hidden timer implementation waiting to be enabled.

The implemented overlay is limited to ordinary current NPC tile marking, like NPC Indicators: no blast radius, future impact location, movement route, safe tile, countdown or player-target information. This is a conservative implementation for review, not a claim of explicit Jagex/RuneLite approval of the new Hub. Any public update still requires normal Plugin Hub review after the owner's game test.

## IntelliJ game test

1. Download this branch ZIP, extract, open the project root in IntelliJ and allow Gradle import with JDK 11 or later. Compilation targets Java 11.
2. Run `src/test/java/lt/oneman/sync/OneManSyncPluginTest.java` as before (or `./gradlew run`). For Jagex accounts follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
3. Open OneMan, confirm build `0.8.5-settings-test`, select Monsters Hub and Lizardman Shaman.
4. At ordinary Shamans, check all already spawned minions have tiles following their current tile each tick. Two simultaneous groups must remain independently tracked; disappearing minions must leave no tile behind.
5. Change tile/model colours and each opacity slider independently. Toggle tiles Off while keeping outline/fill/label On, then toggle each individually. Model highlights must remain when tiles are Off. Select profile Off/Lizardman Shaman; existing visible minions should reappear on the next game tick. Setting opacity to zero hides only that layer.
6. Teleport away, change scene/plane, hop/log out and back in, disable/re-enable the plugin. Check for stale tiles. In Chambers, this helper should show no markers.
7. Check the suggestion with a confirmed Lizardmen task, a different task and no task. Manual selection must always remain available and suggestions must not alter it.
8. Verify Superior notification/red flash still behaves as in 0.8.2 and Slayer, Farming, Clue Hub and saved settings remain intact.

Only the owner can confirm in-game behaviour. Automated JVM/UI tests do not establish real combat timing or game behaviour.

## Navigation verification

The test build has a permanent Monsters Hub button as well as the fifth item in the same Hub dropdown. Automated bootstrap tests click that button, verify it selects Monsters Hub, inspect its visual controls and lay out every Hub at narrow sidebar widths. The launcher prints the exact test version. If the running sidebar does not show 0.8.5-settings-test, it is not this test build. Use the commit-pinned ZIP linked in the handoff to avoid reusing an older source folder.

## Collapsible native settings

OneMan Sync settings now use six collapsed-by-default sections: Sync / Account, Roadmap / Screenshots, Slayer / Alerts / Supplies, Farming, Clue Hub and Monsters / Lizardman Shaman. Click a section heading to expand it. Existing config group, keys, values and defaults are preserved. Hub controls and the native Monsters section write to the same `one-man-sync` keys; they do not create separate effects. Close/reopen the native settings page after changing a Hub control if that RuneLite page is showing an older value.

Manual check: expand Clue Hub and Monsters independently; change a Shaman preference in the Hub, reopen native settings and verify it matches; change it in native settings and return to the Hub to verify the same value. Verify saved Sync Key and Superior notification preferences remain intact after upgrading.
