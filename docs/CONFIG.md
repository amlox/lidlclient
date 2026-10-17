# Saved settings

LidlClient stores its settings in **config/lidlclient.json** in your Minecraft
instance. Changes save automatically and load before gameplay.

The file includes:
- All module on/off switches, including Slots, green tint, instant Pets close,
  Dungeonbreaker swap, Player size and PartyCMDS.
- Slots mute-chat and bet/balance display preferences.
- Each PartyCMDS command switch.
- Your X/Y/Z size overrides. Reset to default removes the overrides.
- The Dungeonbreaker keyboard or mouse binding (including changes in vanilla Controls).
- Registered boolean, numeric and choice settings, including temporary test modules.

Existing party-command and player-size files are imported once without overwriting
newer choices. Existing Minecraft keybind preferences are retained if no key is
saved in the shared config. Old files are left intact as backups; new writes go
to lidlclient.json. An import marker prevents old size overrides returning after
you reset them.

Saves use a temporary file and atomic replacement where supported. Malformed
config files are backed up as lidlclient.json.broken-<timestamp> before defaults
are used. Unsupported newer versions and unreadable files are not overwritten.
Read/write errors are recorded in the Minecraft log.

Buttons are actions, not saved values: startup never presses Reset. Temporary
gameplay activity such as a pending command, an active spin, menu scroll position,
and session balances/statistics is not a setting and is not resumed on startup.
