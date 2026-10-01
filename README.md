# OneMan Sync

Official source repository for the **OneMan Sync** RuneLite plugin used with [oneman.lt](https://oneman.lt/).

OneMan Sync is an opt-in companion plugin for the OneMan OSRS progression tracker. It sends your own account progression data to your OneMan profile so the website can maintain Roadmap, Chronicle, boss/Collection Log history, and bank-aware planning.

> **Plugin Hub status:** this repository is being prepared for RuneLite Plugin Hub review. It is not an official RuneLite or Jagex plugin unless/until it is accepted into the Plugin Hub.

## Data sent when sync is enabled

The plugin can send the following to `https://oneman.lt/runelite_sync.php`:

- RuneScape display name
- Skill levels and XP
- Quest states
- Achievement Diary states
- Completed Combat Achievement tasks and tier state
- Boss kill counts
- Collection Log observations and new unlock events
- Pet events, personal-best messages, and recent tracked boss loot
- A bank snapshot when the bank is opened: item ID, item name, and quantity
- The user's OneMan Sync Key as a bearer token for authenticating the request

As with any connection to a third-party server, the server also receives the user's IP address as part of the HTTPS request.

The plugin does **not** request or read a Jagex password, Jagex account credentials, authenticator code, or bank PIN.

## Privacy and opt-in

- Sync is **disabled by default**.
- Enabling sync shows RuneLite's required third-party-server warning.
- The destination is fixed to `https://oneman.lt`; there is no configurable arbitrary upload endpoint.
- The OneMan Sync Key is stored as a RuneLite secret configuration value.
- Network calls use RuneLite's injected `OkHttpClient` asynchronously via `enqueue()` and are not executed on the client thread.
- You can revoke RuneLite devices/keys or deactivate your OneMan account from the website.

Plugin-side disclosure: [PRIVACY.md](PRIVACY.md)

Website privacy policy: https://oneman.lt/privacy.php

Website terms and copyright: https://oneman.lt/terms.php

## Development

This repository follows the RuneLite example-plugin layout, uses `build=standard`, and targets Java 11 bytecode.

The development runner is `lt.oneman.sync.OneManSyncPluginTest`.

Run locally with the Gradle `run` task after following RuneLite's Jagex Account development-login instructions.

## License

BSD 2-Clause. See [LICENSE](LICENSE).