# Player size

Open /lidl > General > Player size, enable it, then right-click to reveal:
- **X (width)**: side-to-side scale.
- **Y (height)**: vertical scale.
- **Z (depth)**: front-to-back scale.

Each slider goes from 0.25x to 3x and affects only your own model on your client.
Axes follow the player's model orientation, not fixed world directions.
Settings persist between launches; old uniform slider values migrate to all three axes.
Use /lidsize reset to restore all three of your default axes.

Every client running this build defaults SyntocxLeftFoot to 0.5x on all axes
and SyntocxRightFoot to 3x on all axes. Your sliders never edit those defaults
for other players. If you are one of those players, your own local sliders can
override your appearance locally; other clients still see the hardcoded size.
Player size is enabled by default for new installs; existing settings are respected.
Disabling it returns all visual axes to 1x.

This is cosmetic model scaling in third-person and player previews only.
It does not change hitboxes, reach, camera height, movement or first-person hands.
There are no size codes, broadcasts or server changes.
