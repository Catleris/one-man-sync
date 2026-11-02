# Smart Slayer preparation MVP

2026-11-02 · version `0.11.2-slayer-test.2` · branch `dev/slayer-smart-bank-prep` · based on master `51ad900`.

The Slayer Hub header lists special required/recommended items. Green means carried/equipped (wear protective equipment before combat), yellow means found in the profile's dated bank snapshot, red means absent from that snapshot, and grey means the bank has not been observed. Hover a row for restrictions. A dated snapshot cannot prove current ownership.

Bank outlines are static orange for required items and blue for utility items. Taking any suitable alternative clears the whole group's bank outlines, including duplicate bank items. Ordinary tasks have no generic armour, food, potion or helmet highlights. Disable **Slayer task bank hints** in OneMan Sync settings to hide only the bank outlines. Local tools work without website sync.

Supported MVP: gargoyle tools, salt/brine sabre, ice coolers, charged fungicide spray, leaf-bladed/broad/Slayer Dart options, helmet component protection, mirror/V's shields, witchwood icon, fishing explosive (optional Kraken boss activation), wyvern shields, Karuulm boots with elite diary exemption, and flails for vampyre variants. Weapon/ammo/rune combinations and consumable quantities still need manual checking. Generic dragon tasks deliberately do not impose an antifire/shield setup on babies or other variants. Ancient wyverns have stricter shield options. Slayer helmet does not replace mirror shields or witchwood icons.

## Owner game checks (pending)

Launch from this branch with `.\gradlew.bat run` or IntelliJ `OneManSyncPluginTest`. For a Jagex account follow https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts . Keep your existing RuneLite profile.

1. Active gargoyle task: open bank with a rock hammer present. Orange outline; withdraw a hammer and confirm duplicate outlines disappear. Deposit it and confirm outline returns. Granite hammer and rock thrownhammer should satisfy the same group.
2. Dust devil/banshee/spectre task: carry or equip any Slayer helmet recolour/imbued form. No redundant facemask/earmuff/nose peg outlines. A helmet in the bank alone does not satisfy the carried check. A carried protective item is green but must still be equipped before fighting.
3. Basilisk/cockatrice/cave horror: helmet must not suppress the shield/icon reminder. Cave horror icon is utility because prayer/safespot alternatives exist.
4. Wyrm/drake/hydra: blue floor-boot outline without elite Kourend diary; none with completed elite diary. Withdraw any suitable boots and confirm the alternatives clear.
5. Zygomites: empty spray and refills alone do not satisfy charged spray. Check consumable quantities yourself.
6. Bloodveld/greater demon/hellhound/abyssal demon/kalphite: no additional bank outlines. No task/task completion/logout/account switch: previous task outlines clear. Check placeholders, bank search, scrolling and duplicate stacks.
7. Disable bank hints: no Slayer outlines; header states still work. Verify existing Roadmap gold highlights and Clue/STASH markers remain usable.

No Plugin Hub manifest is updated by this test PR. Automated checks do not establish game behavior.

Reference mechanics: https://oldschool.runescape.wiki/w/Slayer_equipment , https://oldschool.runescape.wiki/w/Slayer_helmet , https://oldschool.runescape.wiki/w/Witchwood_icon .

## Unrestricted task locations (test.2)

With a non-Konar Lizardmen task, Places must list Lizardmen Settlement and Lizardman canyon. Select each and open Location guide. These references have no verified entrance pin, so Centre map / Track entrance stay disabled. Check Hellhounds, Dark beasts and Lizards as available. A Konar assignment must still show only its assigned area; known entrances should retain map tracking. Existing item highlights still need owner testing.
