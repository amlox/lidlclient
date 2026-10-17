# Player size

Player size is enabled by default. Every client running this build renders:
- SyntocxLeftFoot at 0.5x.
- SyntocxRightFoot at 3x.
- Other players at 1x unless they adjust their own local slider.

Open /lidl > General > Player size and right-click to adjust **Your size**
from 0.25x to 3x. This override affects only your own player on your own client.
It never changes other players' built-in sizes, and it is not shared with others.
If you are one of the named players, your slider overrides your built-in size
locally; other clients still see your built-in size.

Use /lidsize reset to remove your local override and restore your default.
Disabling Player size returns all models to normal on your client.
Existing saved enable/disable and local slider settings are respected.
Settings persist in config/lidlclient-player-size.json.

Size codes and import/copy commands have been removed. Matching uses the
Minecraft username, case-insensitively. Username changes require updating the list.

This affects third-person models and previews only. It does not modify hitboxes,
reach, movement, camera height, first-person hands or server attributes.
Other players need this LidlClient build to see the built-in sizes.
