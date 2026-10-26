# Player size

Open /lidl > Fun features > Player size, enable it, then right-click to reveal:
- **X (width)**: side-to-side scale.
- **Y (height)**: vertical scale.
- **Z (depth)**: front-to-back scale.

Each slider goes from 0.25x to 3x and affects only your own model on your client.
Axes follow the player's model orientation, not fixed world directions.
Settings persist between launches; old uniform slider values migrate to all three axes.
Click **Reset to default** beneath the sliders to restore all three of your default axes.
There are no player-size chat commands.

Every client running this build defaults SyntocxLeftFoot to 0.5x on all axes
and SyntocxRightFoot to 2x on all axes. Your sliders never edit those defaults
for other players. If you are one of those players, your own local sliders can
override your appearance locally; other clients still see the hardcoded size.
Player size is enabled by default for new installs; existing settings are respected.
The Fun features toggle controls your custom X/Y/Z override. To switch built-in
sizes off, open **General settings > Built-in features** and disable the module or
its **Player sizes** setting. Built-in sizes then fall back to 1x for every player;
your enabled custom self axes still work and remain saved. Disabling the custom
Player size module restores the applicable built-in size, or 1x when presets are off.

This is cosmetic model scaling in third-person and player previews only.
It does not change hitboxes, reach, camera height, movement or first-person hands.
There are no size codes, broadcasts or server changes.

## Editing built-in sizes

Edit `src/main/java/net/lidl/lidlclient/size/BuiltInSizes.java` and rebuild.
Each entry is `Map.entry("lowercaseusername", 1.5)`. Add entries separated by
commas, or change the multiplier. A value of 1.0 is normal size. These defaults
apply to all three axes and are controlled by General settings > Built-in features,
independently of the custom-size toggle.
