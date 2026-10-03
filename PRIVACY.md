# OneMan Sync — data disclosure

OneMan Sync is an opt-in RuneLite plugin that sends gameplay progression data to the third-party service `oneman.lt`.

## Destination

`https://oneman.lt/runelite_sync.php`

## Data categories

When enabled, the plugin may transmit:

- IP address (inherent in the HTTPS request)
- RuneScape display name
- Skill levels and XP
- Quest states
- Achievement Diary states
- Combat Achievement task/tier state
- Boss kill counts
- Collection Log observations/unlock events
- Pet events
- Personal-best chat messages
- Recent tracked boss loot
- Bank item IDs, names, and quantities when the bank is opened
- Clue Scroll STASH built/filled state for each STASH unit
- Current inventory and worn equipment snapshot
- Selected storage snapshots when the relevant storage is viewed or available (Seed Vault, Looting Bag, Rune Pouch, Group Storage, POH Costume Room)
- Slayer task/progression state, POH feature observations, selected minigame/progression currencies, Kingdom state, Bird House tracker timestamps, and Farming Contract state
- OneMan Sync Key used to authenticate the request

## Not collected by this plugin

The plugin does not request or read:

- Jagex passwords
- Jagex account login credentials
- Authenticator codes
- Bank PINs

## Control

Sync is disabled by default. Disabling **Enable OneMan Sync** stops the plugin from sending new data.

OneMan users can revoke RuneLite devices/keys, export their OneMan data, deactivate their account, or delete an eligible member account from the website.

Full website privacy policy and retention periods: https://oneman.lt/privacy.php

Terms and copyright notice: https://oneman.lt/terms.php