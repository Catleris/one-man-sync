# 0.10.1 UI cleanup test

- Fix scroll-arrow components being registered as blank tabs (Slayer, Roadmap and shared tab strips).
- Remove Session/History views and their setting from the interface. Existing saved data and loot tracking remain compatible.
- Show supply counts, nonempty named potion/rune lists and configured low-supply warnings. No combat-style headline or debug maps.
- Short travel references: teleport/code → entrance, spellbook/Magic and access requirements. These are reference options, not an account-aware fastest-path calculation. Uncurated locations retain entrance notes and Location guide access.

Travel references checked 2026-10-22:
- https://oldschool.runescape.wiki/w/Catacombs_of_Kourend
- https://oldschool.runescape.wiki/w/Kourend_Castle_Teleport
- https://oldschool.runescape.wiki/w/Slayer_Tower
- https://oldschool.runescape.wiki/w/Fremennik_Slayer_Dungeon
- https://oldschool.runescape.wiki/w/Mount_Karuulm
- https://oldschool.runescape.wiki/w/Fairy_ring
- https://oldschool.runescape.wiki/w/Taverley_Dungeon
- https://oldschool.runescape.wiki/w/Falador_Teleport
- https://oldschool.runescape.wiki/w/Smoke_Devil_Dungeon

User test: launch OneManSyncPluginTest in IntelliJ. Open Slayer and optional Roadmap: only named tabs should appear, scrolling arrows should work when needed. Check Supplies with empty and populated inventory, and Travel on the current task. Session/History should stay absent even with the old saved setting enabled. Confirm existing Superior alert and other Hubs remain as expected. Game behavior requires user confirmation before public Plugin Hub submission.
