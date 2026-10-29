# Reels Controls (Firefox and Opera/GX)

This controls Instagram in your regular browser while Minecraft has focus. It does
not embed a browser. The module is off by default and all three keys are unbound.

## Setup

1. Install the updated LidlClient. In `/lidl` → QoL features, enable **Reels Controls**.
   Right-click it and assign Next Reel, Previous Reel and Play / Pause. Choose keys
   that do not conflict with gameplay. Keys also appear in Minecraft Controls.
2. Download/extract the companion extension directory from this repository/build.
3. Firefox: open `about:debugging#/runtime/this-firefox`, choose **Load Temporary
   Add-on**, and select `browser-extension/manifest.json`. Temporary installations
   are removed when Firefox restarts. Permanent installation requires Mozilla
   signing; this development build has not been signed or published.
4. Opera or Opera GX: open `opera://extensions`, enable Developer mode, choose
   **Load unpacked**, and select the `browser-extension` directory.
5. Allow the extension access to Instagram and localhost if the browser asks.
   Reload existing Instagram tabs after installing. Log in normally on Instagram,
   open `https://www.instagram.com/reels/`, and start playing a Reel once.
6. In `/lidl`, choose **Copy connection code**. Open the extension popup in your
   chosen Instagram tab, paste the code, and click **Control this tab**.
7. Return to Minecraft and close its menus. Your three keys now control that tab.
   Use Disconnect in the popup to stop. Reconnect after restarting the browser.

Only the explicitly selected tab receives commands. Connect one browser at a time;
multiple browser connections compete for commands. Keep the browser running and
avoid tab sleeping/memory-saving for Instagram. Browser policies may throttle or
suspend background tabs, and playback can require an initial click in the browser.
Commands expire after two seconds, so disconnected commands do not replay later.

The extension uses Instagram's navigation buttons or its vertical scroll container.
Play/Pause operates on the most visible video. Instagram layout changes or experiments
may require updating those selectors. The popup reports navigation/playback errors.
No live Instagram account/browser testing has been performed for this build.

## Connection and privacy

The mod listens only on `127.0.0.1:18764` while enabled and requires a random local
connection code. It sends only NEXT, PREVIOUS and TOGGLE_PLAY; it receives no page
content, passwords or cookies. The extension requests Instagram/loopback access and
local settings storage, not cookie access. Source is included without remote scripts
or telemetry. The code is saved in the local mod config and extension local storage;
do not share it. Disabling the module closes the listener and clears pending commands.
Keys do nothing in Minecraft menus, without a player, or while Minecraft is unfocused.

If connection fails, enable the module, check **Connection status**, ensure only one
Minecraft instance is listening on port 18764, and copy the code again. Firefox 140+
and current Opera/GX are targeted. Install and DOM behavior still need real-browser
verification; build success alone does not establish Instagram compatibility.
