# OneMan unified interface — 0.10.0 test

Run `OneManSyncPluginTest` in IntelliJ. All existing hub/configuration keys and gameplay behaviour are retained. This branch includes Combat Hub and Birdhouse work from PR #6; it is not a public Plugin Hub release.

## Visual and functional checks

- Tools: green/gold header, item-icon cards, readable descriptions, arrow buttons opening all six hubs. The persistent selector and native settings button remain available. Test at ordinary sidebar width and a short fixed-mode window.
- Slayer: compact task summary; gold Prep/Tactics/Places/Travel/Supplies/Loot tabs. Text sections are wrapped cards. Places → Map & travel controls expands the existing entrance selector, access notes, map/tracking buttons and guides. Check assigned location, variant selection, loot goal saving and optional Session/History notes. Existing alert and protection logic is untouched.
- Farming: Patches and Seeds share the same cards, fields, gold buttons and tabs. Verify type/seed filters, patch selection/map focus and the previously added Runs cards.
- Clue: coloured tier badges, card-based lookup/results/recent items and Help; STASH and Options remain available. Verify inventory/bank/missing colours, item lookup, option saving and guide links. Credits remain in the existing collapsed section.
- Monsters: profile and suggestion stay visible. Appearance presets, Tile markers and Spawn appearance have collapsible controls. Expand them to check all saved toggles, colours and opacity sliders, including the optional cosmetic pulse.
- Unlocks: shared rounded cards, gold expandable categories/details, search/filter/favourites and manual confirmations. Check a long title/category and expanded quest details.
- Combat: retain tier colours, distinct portraits (including Hunllef and Chambers CM), task tracking and stable scrolling.
- If Optional views → Roadmap is enabled, Next/Prep also use text cards. Normal Tools navigation still hides optional account/session views.
- Scroll partway through long text/card views and wait for a passive update; it should preserve the reader's position. Type in editable Slayer notes and save as before.

`OneManTheme` styles only OneMan-owned Swing components. No global UIManager defaults or RuneLite-owned settings panels are changed. Read-only card text sets the non-scrolling caret policy before inserting content; editable notes remain editable. Existing passive text sources feed a presentation-only card adapter, preserving wording and original action callbacks. The toolbar helmet and local item/sprite icons are reused.

Automated coverage: real Guice startup/native settings action, every hub at 242px, expanded Monsters/Slayer/Unlocks/Runs views, existing controls and editable notes after repeated styling, global-theme isolation, deferred text-card scrolling, existing Combat colour/scroll/account tests and the project's regression suite.
