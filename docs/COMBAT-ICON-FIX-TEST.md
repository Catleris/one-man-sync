# 0.10.2 icons test

Based on approved 0.10.1. No task completion, tracking, filtering or gameplay logic changes.

- Add Sarachnis, Shellbane Gryphon, Mad Angel, Maggot King and Doom of Mokhaiotl native boss portraits; resolve Moons of Peril and TzHaar-Ket-Rak category names.
- Add Kurask and further Slayer guide icons; use theme-related local item icons for Demonic Gorilla (zenyte shard), Fragment of Seren (Seren book), Galvek (red dragon mask), Glough (Kruk greegree), and Tormented Demon (synapse). These are thematic items, not newly generated boss portraits.
- General uses the Combat Achievements book. Others uses Book of the dead, matching its thrall task. Future/unmapped groups request the achievement book instead of retaining the generic shield.
- Existing Gauntlet blue/red and Chambers CM mappings are retained. All images come from the local game cache, with no image downloads.

References: current RuneLite gameval ItemID.java and SpriteID.IconBoss25x25, and the legacy SpriteID.HISCORE_SARACHNIS constant. New regression checks use the actual display names reported in the client, rather than pre-normalised lookup keys.

IntelliJ: run OneManSyncPluginTest. In Combat Achievements open Easy/Medium/Hard/Elite and check the reported category icons. Also check Unknown/General, Gauntlet variants, scroll position and filtering. Actual in-game appearance requires user verification before public Plugin Hub submission.
