# OneMan Hub data handling

OneMan Hub version 0.12.0 processes observations locally in RuneLite. It contains no HTTP client, website synchronization endpoint, screenshot uploader or automatic telemetry.

The local tools read relevant game state, including skill/quest progress, Slayer assignments, inventory and equipment, observed bank items, farming timers, clue/STASH requirements and unlock state. They use these observations to display preparation hints, maps, timers, reference information and notifications.

Preferences and selected per-account journals are stored through RuneLite configuration. Some bank observations and session counters are kept in memory or local profile data. The existing configuration group and plugin directory remain in use after the display name changes from OneMan Sync to OneMan Hub.

Milestone screenshots are disabled by default. If enabled, they capture the visible game window and save PNG files in the plugin's local screenshots directory.

Explicit Wiki buttons open an external page in your browser. The destination website receives a normal browser visit and applies its own privacy policy.

Previous OneMan Sync versions could send data to oneman.lt when synchronization was enabled. Upgrading to OneMan Hub stops those plugin uploads. It does not delete data already stored in an existing website account.
