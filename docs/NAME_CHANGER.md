# Name changer

Built-in aliases can be toggled under **/lidl > General settings > Built-in features**.
The master switch controls all built-in features; right-click it to switch **Name
aliases**, **Player sizes**, and **Warp shortcut** individually. These default to on
to preserve existing behaviour. Disabling built-in aliases immediately restores real
usernames, except for an enabled custom self name.

The **Name changer** toggle in Fun features only enables/disables your custom self
name. Disabling it restores your built-in alias when aliases are on, or your real
username otherwise. Your custom input stays saved.

| Real username | Visual alias | RGB color |
| --- | --- | --- |
| SyntocxLeftFoot | ✡ Tesco ✡ | White → blue letters; blue stars |
| SyntocxRightFoot | Lidl | #FF5555 |
| SyntocxFeetPics | petal | #FF75ED, #FC83EB, #FC90EB, #FFA1ED, #FFADEE |
| __beacon | Syntocx | #FF55FF |
| Fawnelle | Kontsus | #5555FF |
| quak07 | quak | #FFFF55 |

Usernames match case-insensitively, only as whole Minecraft-name tokens. Rank
prefixes and surrounding text retain their styling. The petal alias uses the exact
five supplied colors, one per letter. Tesco keeps its name with a Star of David
at either end, blue stars and a white-to-blue five-letter gradient. All standard
rendering paths share this same alias rule, including repeated occurrences.

Aliases are applied to visual component copies, font preparation, and text layout,
covering normal name tags, chat, tab, scoreboards, menus and tooltips. Source chat,
account identities, commands, copied usernames, UUIDs and size matching are not
changed. Click/hover actions retain their original targets.

Only clients running this build see the aliases. Text baked into images and custom
renderers that bypass Minecraft's text pipeline cannot be replaced. A nicked or
hidden username cannot be matched when the actual username is absent.

## Your own custom name

Right-click Name changer and click **Edit your name**. Enter up to 200 characters,
then click Save or press Enter. Cancel/Esc keeps the previous value. Reset or
saving a blank name restores your built-in alias, or your real username if you
are not in the built-in list.

The override applies only to the currently logged-in player's real username on
your own client and saves in config/lidlclient.json as name_changer.self_name.
It retains your built-in alias color/gradient if one exists and built-in aliases are
enabled; otherwise it uses white.
Other players keep their built-in aliases, and other clients do not receive your
custom override. The text editor displays literal input without applying aliases.

### Custom colors

Use Minecraft colors `&0` through `&f`, or hex colors such as `&#FF75ED`.
For example, `&cRed &9Blue` or `&#FF75EDpetal`. Codes apply until another
color code; `&r` restores your default alias color/gradient (white for unlisted
accounts). The section-sign equivalents also work. Codes do not count toward the
200 visible-character limit. A code-only name resets to the default. Unsupported
codes remain literal. Your entered codes are saved with the name.

## Editing built-in aliases in code

Edit `src/main/java/net/lidl/lidlclient/names/BuiltInAliases.java`.
Each entry is `alias("RealUsername", "Display name", 0xRRGGBB)`.
For example, add `alias("AnotherPlayer", "Nickname", 0x55FF55)` to the PLAYERS list.
Use commas between entries. Supply several colors for a per-letter gradient.
Usernames are matched case-insensitively. Rebuild the mod after editing.
