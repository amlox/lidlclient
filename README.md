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

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
