# Farming health and compost MVP

2026-11-02 · 0.11.2-hubs-test.3 · same development PR as Slayer improvements.

Farming Hub shows a compact red observed diseased/dead patch summary (up to three rows; hover for the full list), and red status text on the selected affected patch. Reads the existing RuneLite Time Tracking observations for the 17 tracked herb/tree patches. This is last observed state, not remote disease prediction. Visit the patch with Time Tracking enabled to update it. Optional desktop notifications are off by default and suppress repeated alerts until observed recovery/replanting. Birdhouse timers remain in Runs.

A carried bottomless compost bucket shows its last explicitly reported use count and observation age. Empty item form confirms zero. Game Check/remaining/fill/empty messages update it; ordinary patch treatment messages do not invent consumption counts. One carried filled bucket with no observation stays unknown; multiple buckets request a one-at-a-time Check. Counts reset on logout/profile or carried bucket-count changes. These are session observations, not a stored per-item identity. Default refill threshold is at/below 50 uses (zero means only empty); optional desktop refill notification is off by default. Use Check again after use/refill or swapping buckets, especially if one replaces another without an observable count change.

## Owner checks pending

1. With Time Tracking enabled, observe a diseased tree/herb. Farming Hub must show the red warning and affected patch status. Cure/replant and revisit: warning clears. Reopening Hub must not spam optional notifications. An unknown/unvisited patch must never become a disease alert by estimate.
2. Carry exactly one bucket, use Check, and confirm last reported count. Test low/empty, refill, and Check again. Repeated low reports should not spam notifications; a refill above the threshold rearms them.
3. Try no bucket, a filled unknown bucket, multiple buckets, and another account/login. Unknown must not mean empty; no previous account count should appear.
4. Verify Patches/Seeds filters, map focus, birdhouse Runs, existing timers, and Slayer test.2 features still work with website sync disabled.

Launch: `.\gradlew.bat run`; Jagex accounts: https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts . No game input is automated. No master merge or Plugin Hub update in this test.

Protocol reference (game message forms and gameval item names only; independent implementation): https://github.com/TicTac7x/runelite-plugins/blob/plugin-charges/src/main/java/tictac7x/charges/items/utils/U_BottomlessCompostBucket.java .
