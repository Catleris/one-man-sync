# OneMan Hub

OneMan Hub brings local Slayer, Farming, Clue, monster reference and account progression tools into one RuneLite sidebar.

## Local tools

- Slayer preparation with named special items, inventory/equipment checks and bank highlights.
- Slayer task locations, map destinations, supplies, Superior alerts and session loot.
- Farming seed lookup, patch observations, birdhouse timers and optional notifications.
- Clue item classification, STASH requirements and map markers.
- Monsters Hub with elemental/melee/ranged reference information, a manually opened Target Info page, Shaman spawn markers, Gemstone Crab alerts and a manual Grotesque Guardians fight guide.
- Unlocks and Combat Achievements browsers.
- Collection Log, Combat Achievement and Diary banners, and optional styling of the original quest-completion window.
- An optional local roadmap and local milestone screenshots.

## Privacy and storage

OneMan Hub processes gameplay observations on your computer. Version 0.12.0 removes the website synchronization client, upload queue, screenshot uploader and Sync Key controls. It does not make HTTP requests or send gameplay data to OneMan servers.

Settings, per-profile journals and bank observations use the existing RuneLite configuration keys. The internal plugin identifier and data directory remain `one-man-sync` so existing preferences and local records remain accessible after the display name changes.

Local screenshots remain optional and are saved on your computer. Their images include the visible game window.

Wiki buttons open the browser only when clicked. These external pages have their own privacy policies.

See [PRIVACY.md](PRIVACY.md) for details.

## Development

Use JDK 11.

- Build and test with `./gradlew clean build shadowJar`.
- Start the development client with `./gradlew run`.
- Use `./gradlew run -Poneman.isolatedProfile` for an isolated development profile.
- Windows users can use `gradlew.bat` for the same tasks.

The default development launcher uses your normal RuneLite profile. For Jagex accounts follow [RuneLite's development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

Plugin Hub availability requires maintainers to review and merge the manifest update. Source publication alone does not update installed clients.

## Attribution

The plugin uses RuneLite's local game data and bundled catalogs. Monster reference data is derived from Koitere/monster-stats and OSRS Wiki. Resource licenses and attribution are preserved in:

- [Monster data license](src/main/resources/lt/oneman/sync/target-info-LICENSE.txt)
- [Map data license](src/main/resources/lt/oneman/sync/map-data-LICENSE.txt)
- [Clue data license](src/main/resources/lt/oneman/sync/clue-data-LICENSE.txt)

Software license and project identity guidance are in [LICENSE](LICENSE) and [BRANDING.md](BRANDING.md).
