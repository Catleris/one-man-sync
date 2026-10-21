# Clean game-tool interface — 0.9.1-clean-ui-test

## Default experience

The first Hub is now **Tools**, a compact set of buttons for Slayer, Farming, Clue, Monsters and Unlocks. It contains no account name/type, XP summary, session timestamps or upload history. The duplicate permanent Monsters button has moved into this tools list; the existing shared selector and settings button remain available.

The old Overview is removed. Its useful Next/Prep planner is available as **Roadmap**, initially hidden. Enable **Optional views → Show Roadmap** in native settings to add it to the selector. Toggle updates immediately. Roadmap-specific bank highlighting is also inactive while the Roadmap is hidden. Roadmap no longer prints player identity, combat/quest-point introduction, Chronicle ownership commentary, duplicate Slayer analytics, Sync, Memories or Summary tabs.

The removed Overview daily/session summary is no longer generated, read or written. Existing saved `companionDay` / `companionSummary` records are retained untouched. Screenshot milestone options, per-account goals, existing Slayer journals and the optional website-sync functionality are preserved. Removing a status page does not change the website upload permission, scope or API behaviour; website sync remains controlled by its existing opt-in setting and key. Sync errors remain in existing debug logging without a player-facing feed.

Slayer's Session and History tabs start hidden, and can be enabled under **Optional views → Show Slayer session / history**. Functional Prep, Tactics, Places, Travel, Supplies and Loot tabs remain. Task name/count/assigned area remain visible once in the header; required protection, area restrictions and travel eligibility are retained.

Monsters has one collapsed Help control instead of two permanent technical text cards. Pulse remains cosmetic with the same fixed timing and settings. Clue's Help tab has brief definitions and a collapsed Credits / coverage section; source notices and bundled licenses remain. Farming no longer prints raw varp numbers when a birdhouse state is unknown.

## Unlocks cards

Cards use plain statuses: Unlocked, Locked, Requirements missing, Quest unfinished, Diary unfinished, Check progress, Previously seen, Marked complete, Not checked. These display labels do not alter evidence rules. Past observations remain **Previously seen**, never current ownership. Manual checks stay **Marked complete**.

**How to unlock** contains concrete steps and missing requirements without source/flag terminology, raw observation timestamps or repetitive provenance boilerplate. Favourite/account data remains saved under the same keys. The header shows only catalogue size, not account identity or a personal progress summary.

Seeing Red is named explicitly, shows **Locked · 50 Slayer points** when not bought and points to **Slayer master → Rewards → Unlock**. Confirmed current costs are included for Bigger and Badder (50), Slug Salter (10), Reptile Freezer (10) and Seeing Red (50). Unverified prices are not invented for other rewards.

The obsolete standalone Mithril dragon task unlock was removed, leaving 284 curated entries / 498 including the optional 214 Quest enum entries at implementation. Metal-dragon tasks were combined in Jagex's [27 August 2025 update](https://secure.runescape.com/m=news/summer-sweep-up-slayer--more?oldschool=1); the same update confirms the reduced Superior/salt/freezer costs. Seeing Red's current cost is documented by the [Slayer rewards table](https://oldschool.runescape.wiki/w/Slayer_Rewards).

## Manual checks

1. Run OneManSyncPluginTest from the pinned new ZIP. Build label: `0.9.1-clean-ui-test`.
2. Default selection must be Tools with working buttons for each Hub. There must be no Overview, Sync, Summary or Memories in normal navigation.
3. Enable Show Roadmap, select Roadmap and check Next/Prep. Disable it while selected: the selector should return to Tools without restarting.
4. In Slayer, verify task/count/assigned area plus Prep/Tactics/Places/Travel/Supplies/Loot. Toggle session/history visibility and confirm those optional tabs appear/disappear without restarting; assignment/protection information must remain accurate.
5. In Unlocks, open Seeing Red and check its price and useful instructions. Confirm no Source/flag paragraphs or raw timestamps are visible. Previously observed items must remain Previously seen rather than Owned/Unlocked. Search, filters, favourites and manual confirmations must still work.
6. Check Clue STASH/item functions, Farming timers, Shaman overlays/pulse and Superior notifications. Check narrow sidebar layout and scrolling with cards expanded.
7. Existing website sync and milestone screenshots retain the user's explicit saved options. Old saved histories are not erased.

This remains a draft test update awaiting the owner's game test, not a public Plugin Hub submission.
