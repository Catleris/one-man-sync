# Unlocks Hub test — 0.9.0-unlocks-test

This builds on the existing 0.8.6 test branch, retaining Superior 0.8.2 fixes, all hubs, Shaman visuals/pulse and collapsed native settings. No master merge or public RuneLite Plugin Hub submission is included.

## Coverage and navigation

Open OneMan's existing Hub dropdown and choose **Unlocks Hub**. The build label must read `0.9.0-unlocks-test`.

The default curated catalogue has 285 entries: travel networks, spellbooks/spells, learned prayers, quest-gated areas/activities, important items/sets, Farming/guild access, POH/account upgrades, 48 diary reward items, 26 Slayer purchases/recolours, 48 Achievement Diary tiers and six Combat Achievement reward tiers. It is a broad curated registry, not a claim to cover every unlock in OSRS.

Enable **Include quest journal** in the Hub or native Unlocks Hub settings to add every Quest enum entry supplied by the current RuneLite client (214 at implementation), including miniquests and subquest stages. These are explicitly quest-completion records, not extra claims of reward ownership. Future RuneLite quest additions appear automatically.

Categories start collapsed. Expand only what you need. Search titles, categories, quest names, skill gates or descriptions. Filter states, mark favourite entries and use Favourites only. Expanded groups initially show 15 entries with Show more. Each entry has its own collapsed details, evidence, known requirements and Wiki link.

## Evidence semantics

- **Game confirmed**: completed quest gate, explicit learned/purchase flag, diary completion or claimed CA reward state. The title and note define exactly what has been confirmed. It does not imply a physical item is currently owned or the player is ready for a boss.
- **Gate pending / In progress**: a recorded quest, base skill or signal has not reached the tracked condition. Some content has earlier partial-quest/boosted access; relevant entries say so. This is not a universal statement that every route is blocked.
- **Check / claim**: known gates are met, or an action needs manual checking. Additional purchases, dialogue, crafting, training or item collection may remain. No item is awarded just because a quest or skill gate is met.
- **Item observed**: a matching item was actually seen in a non-placeholder inventory/equipment/bank container. Saved evidence is historical and timestamped; it is not proof of current ownership, charges, full-set ownership or equipment suitability. Bank absence never means the item is missing everywhere.
- **Confirmed by you**: explicit, reversible manual confirmation for an acquisition/action. Never counted as game verification. This does not override automatic quest/flag records.
- **Unknown**: logged out, profile unavailable or required game signal unavailable. Old account facts are cleared on scene/login transitions and profile switches.

Fairy rings use the actual `FAIRYRING_PERMISSION` bit, not completion of all Fairytale II. Arceuus has no obsolete favour gate and remains a manual dialogue/access confirmation. Rigour/Augury/Preserve/Deadeye/Mystic Vigour show learned flags, with usage requirements separate. Chivalry/Piety require manual confirmation beyond their recorded quest/level gates. Item sets are never inferred from one piece. Slayer purchase flags are separate from current reward toggles. Legacy Karamja diary completion values retain the existing project decoder.

Requirements shown are the explicitly modelled gates, not a complete generated walkthrough. Read the entry's Wiki guide for the full acquisition process. Some areas deliberately record a completed-quest route even when earlier partial access is possible; their titles/notes state this limitation.

## Data and storage

The original curated descriptions and links are in `src/main/resources/lt/oneman/sync/unlocks.tsv`. Automatic signal mappings use RuneLite gameval constants in UnlockCatalog and the project's existing diary decoder. Quest-state reads use RuneLite's Quest API, not hardcoded quest-stage numbers. Read failures remain Unknown.

Game reads happen on the client thread after a login settle period. Dirty state refresh is throttled to at most once per ten game ticks, with a periodic fallback. Item observations use container events and indexed name lookups rather than scene scans. No network calls are required; Wiki links open only on a user click.

Favourites, manual confirmations and timestamped item observations persist under the current RuneScape profile (`one-man-sync`, profile key, `unlocksHubProgress`). No settings are read or written with an absent profile. There is no website upload of these new notes in this update. The Hub ignores unknown saved IDs and invalid timestamps.

## Manual test checklist

1. Extract the pinned source ZIP into a new folder, open in IntelliJ and run **OneManSyncPluginTest**. For Jagex login follow https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts .
2. Select Unlocks Hub. Before login entries should be Unknown. After login allow about eight game ticks for account loading.
3. Expand Travel, Spellbooks, Slayer and Diaries. Compare fairy-ring permission, your known Slayer purchases and finished quests with the game. Check a Karamja diary tier.
4. Search `fairy`, `Barrows gloves`, a quest name and a skill name. Change each status filter. Enable the quest journal and confirm the distinct Quest journal category appears.
5. A completed RFD finale must not alone mark Barrows gloves as owned. Carry/equip the item or open a bank containing it; evidence should become Item observed with a timestamp. Bank placeholders must not count.
6. Favourite several entries, use Favourites only, then restart. Confirm they persist for this account.
7. Manually confirm a checkable action (e.g. a full outfit or POH feature), restart and confirm it remains labelled Confirmed by you. Undo it and verify it does not count as game confirmed.
8. Log out, hop and switch accounts. Old facts must not appear as current-account unlocks; a second account must not inherit the first account's confirmations, favourites or item history.
9. Expand a category and individual details at narrow/fixed-mode widths; scroll should remain inside the Hub, without resizing the game window. Verify Show more works.
10. Recheck Slayer/Farming/Clue/Monsters navigation, Superior notifications and saved native settings.

Automated tests validate evidence separation, partial fairy permission, missing signals, diary/CA encodings, charged-item matching, account isolation, logout, real sidebar injection and expanded-card layout. Only the owner can confirm live game behaviour.
