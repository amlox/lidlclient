# Instant menu cursor

Open `/lidl` > **QoL features**, enable **Instant menu cursor**, then right-click
its card and choose **Edit keybinds**. Use **Add keybind** and press your existing
menu hotkey, such as R for Pets. Add as many keyboard bindings as you need; duplicate
keys are ignored. Each row has Remove, and longer lists have Previous/Next pages.
Keys and the module toggle persist across restarts. Escape cancels key capture.
The module starts disabled with no keys.

Keep your original mod's menu hotkey configured. This feature observes the same
key and releases the cursor after normal key dispatch; it does not run a command,
replace a keybind, open a placeholder screen, or shorten the server's response time.
It only activates in a focused world when no screen is open and the mouse is grabbed.
The cursor starts at Minecraft's normal centre position and can move freely while
the menu loads, without turning the camera.

When a real menu opens, it keeps the released cursor. Minecraft 26.1.2's
releaseMouse returns immediately when already released, so the normal menu-open
path does not recenter it again. Attempts to re-grab while waiting are blocked.
New mouse presses during the wait are ignored so they cannot re-grab or click the
world, and they are not replayed into the menu. Releases still pass through to
avoid stuck buttons.

**Wait timeout (seconds)** defaults to 1.5 and is adjustable from 0.25 to 5.
If no menu appears, mouse control returns automatically with queued look motion
cleared. Escape cancels the wait immediately and returns control; a menu that
arrives later will open normally. Repeat key presses do not extend the timeout.
Opening any screen hands off cursor ownership and cancels the wait. Disconnecting
or losing window focus cancels without forcing the mouse to be captured.

In-game checks: bind the same key in both mods, press it, move the cursor during
the server delay, and verify the camera stays still and the menu keeps the cursor
position. Test multiple keys, repeats, no-response timeout, Escape, clicking during
the wait, already-open menus, reconnecting and Alt-Tab. Other mods with custom raw
input/cursor handling may need live compatibility testing.
