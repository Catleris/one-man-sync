# OneMan Sync 0.11.0 validation

The owner confirmed the final TEST12 build in-game and authorized release on 2026-10-31. Earlier screenshots demonstrated actual quest completion with native rewards, Quest Points, model, ribbon and close control; the final cosmetic backing correction was then accepted.

## Released behavior

Four independent local banner categories: new Collection Log item, individual Combat Achievement task, Combat Achievements tier unlock, Achievement Diary region/tier completion. Quest window style is a separate opt-in cosmetic setting, not a queued quest banner.

All switches default off and preserve existing configuration keys. Collection and CA task chat notifications must be enabled in OSRS. CA tiers use live game thresholds; Diary support covers all 12 regions and four tiers, including legacy Karamja completion states. Login/reconnect establishes a silent baseline; enabling a category does not replay history. The first ten logged-in ticks are baseline warm-up.

Preview buttons produce clearly marked local examples without changing progress or uploading fake achievements. A real quest uses the original completion interface, with game-owned texts, reward model, ribbon and controls. Quests without that native interface do not receive an invented popup.

## Quest implementation

A MANUAL overlay draws after the decorative scroll layer, before native reward siblings. The original decoration's type/content and text styles restore on close, style disable and shutdown. Missing draw callbacks trigger restoration of the original decoration. Geometry uses usable native bounds or the actual congratulations-text anchor instead of a small 3D model viewport. The opaque backing is confined inside the gold frame.

## Automated validation

Java 11 clean build, unit tests and executable development packaging run in GitHub Actions. Tests cover parsing/queueing/deduplication, CA/Diary transitions and baselines, sidebar controls, quest rendering persistence, tiny-model geometry, opaque interior, transparent outer edges, restoration and missing-callback recovery.

No RuneScape input was automated. Automated rendering tests use mocked widgets and BufferedImage output; they do not replace the owner's in-game verification.
