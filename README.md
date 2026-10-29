# Lidl Client

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## Instant menu cursor

Right-click **Instant menu cursor** in `/lidl` to configure its keybinds and settings.

- **Legit mode** (off by default): clears held gameplay keys and blocks keyboard presses
  and hotbar scrolling while waiting for the real menu. Escape cancels the wait;
  key/button releases still pass through. Early mouse clicks are discarded in either mode.
- **Ding on GUI open** (off by default): plays a local ding once when a container menu
  opens during this module's active wait. Ordinary screen openings, expired waits,
  cancellation, focus loss and disconnects do not trigger it.

Input blocking ends when a screen opens, the wait times out, the module is disabled,
or the wait is cancelled.

## Farming armor warning

Enabled by default; toggle **Farming armor warning** in `/lidl` → **QoL features**.
While holding attack on a crop in Hypixel SkyBlock's Garden, a red **WRONG ARMOR**
warning appears in the center for two seconds, refreshed while harvesting.
All four armor slots must contain Helianthus, Fermento, Squash, or Cropie pieces;
mixing those sets is allowed. Empty slots and other armor (including Rancher's Boots)
trigger the warning. Item IDs are checked rather than display names.

The warning clears when you equip allowed armor, leave the Garden, open a screen,
disconnect, or disable the module. Garden detection reads the live SkyBlock sidebar
and tab-list area, including when the sidebar shows a custom plot name. It stays
off if the Garden cannot be identified. No API key or outgoing chat is used.

Manual checks: harvest with each allowed set, a mixed allowed set, an empty slot,
and unrelated armor; repeat on the Private Island and Hub and confirm no warning.
Check named Garden plots, continuous harvesting, armor changes, disconnects, GUI
scales, and that the toggle persists after restarting.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
