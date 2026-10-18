# Name changer

Enabled by default. Toggle **Name changer** in /lidl > Fun features.
The choice is saved in the shared config.

| Real username | Visual alias | RGB color |
| --- | --- | --- |
| SyntocxLeftFoot | Tesco | #000000 |
| SyntocxRightFoot | Lidl | #FF5555 |
| SyntocxFeetPics | petal | #FF75ED, #FC83EB, #FC90EB, #FFA1ED, #FFADEE |
| __beacon | Syntocx | #FF55FF |
| Fawnelle | Konstsus | #5555FF |
| quak07 | quak | #FFFF55 |

Usernames match case-insensitively, only as whole Minecraft-name tokens. Rank
prefixes and surrounding text retain their styling. The petal alias uses the exact
five supplied colors, one per letter.

Aliases are applied to visual component copies, font preparation, and text layout,
covering normal name tags, chat, tab, scoreboards, menus and tooltips. Source chat,
account identities, commands, copied usernames, UUIDs and size matching are not
changed. Click/hover actions retain their original targets.

Only clients running this build see the aliases. Text baked into images and custom
renderers that bypass Minecraft's text pipeline cannot be replaced. A nicked or
hidden username cannot be matched when the actual username is absent.

## Your own custom name

Right-click Name changer and click **Edit your name**. Enter up to 32 characters,
then click Save or press Enter. Cancel/Esc keeps the previous value. Reset or
saving a blank name restores your built-in alias, or your real username if you
are not in the built-in list.

The override applies only to the currently logged-in player's real username on
your own client and saves in config/lidlclient.json as name_changer.self_name.
It retains your built-in alias color/gradient if one exists; otherwise it uses white.
Other players keep their built-in aliases, and other clients do not receive your
custom override. The text editor displays literal input without applying aliases.
