# Instant menu cursor

Open `/lidl` > **QoL features**, enable **Instant menu cursor**, then right-click
its card and choose **Edit keybinds**. Use **Add keybind** and press your existing
menu hotkey, such as R for Pets. Add as many keyboard bindings as you need; duplicate
keys are ignored. Each row has Remove, and longer lists have Previous/Next pages.
Keys and the module toggle persist across restarts. Escape cancels key capture.
The module starts disabled with no keys.

Keep your original mod's menu hotkey configured. Saved cursor keys and slot layouts
carry forward automatically; no rebinding is needed. The cursor module now uses
an independent **SharedHotkeys** framework, outside Minecraft's KeyMapping system.
It never registers a competing binding, consumes a key press, clears a key state,
or changes another mod's assigned keys.

The framework observes the raw GLFW keyboard callback and always forwards the
original callback with the same window, key, scancode, action, and modifiers.
Actions run before the next mouse-movement pass, after the original callback has
returned. Short press/release taps between frames are retained; repeats and
releases never retrigger. Multiple listeners may share a key. Removed/rebound
listeners discard stale queued presses, and the input queue is bounded. Screen,
focus, and gameplay eligibility are captured before the original callback runs
and checked again when the action executes. A key that closes another GUI cannot
accidentally start a cursor wait.

During a wait, only the **physical OS cursor** is unlocked. Minecraft's logical
mouse-grabbed flag stays true, allowing other mods' raw/tick/held-key handlers to
continue seeing normal gameplay focus. Camera turning is blocked separately and
queued look motion is cleared. This also removes the previous one-tick handoff
delay. The cursor starts at Minecraft's centre position and moves freely while
the server menu loads. No command or placeholder GUI is opened.

When a real screen arrives, the mouse hook completes the logical release without
moving the already-free cursor. Escape or timeout restores normal mouse capture.
Focus loss and disconnect finish the logical release without forcing capture.
Disabling the module restores control when appropriate. New mouse presses during
a wait are ignored to prevent world clicks/regrabbing, while releases still pass
through. **Wait timeout (seconds)** defaults to 1.5 and is adjustable from 0.25 to 5;
repeat presses do not extend it.

In-game checks: assign the same menu key in both mods, enable the cursor module,
press and quickly release the key, and confirm the other mod still opens its menu
while cursor movement does not turn the camera. Also test held keys, two different
menu keys, no-response timeout, Escape, focus loss, reconnecting, and screen arrival
without cursor recentering. Mods that inspect native GLFW cursor mode directly or
replace callbacks without forwarding them still require testing with the actual
mod pack; unit/build checks cannot verify those integrations.

## Slot aim overlay

Enable **Show slot overlay** in the module and adjust **Slot opacity (%)** (5–80%,
default 30%). Open **Edit keybinds**, then click a key's **Slots** button. Pick the
chest size: 1–6 rows / 9–54 slots. For the Pets menu in the supplied screenshot,
choose **54 slots (6 rows)**.

Left-click grid slots to select/unselect them. **Brush** chooses a colour for new
selections; right-click any slot to choose its individual colour with the mouse
picker or hex input. **Save** commits the entire layout; **Cancel**/Escape discards
the draft, and **Clear** clears it. Drafts survive the colour picker and resizing.
Changing row counts keeps selections from other sizes, but only slots within the
chosen size render. Slot numbers in the editor start at 1.

Each key has its own saved size and colours. Previously configured keybinds still
work and start with no highlighted slots. Removing a key also removes its overlay.
The live overlay consists only of translucent 16×16 squares in selected slots,
with no text, border, chest background or placeholder menu. It appears only during
that key's pending cursor wait, and disappears when a real screen opens, on Escape,
timeout, focus loss, disconnect, or disabling the module.

Alignment uses Minecraft 26.1.2's vanilla chest geometry: a 176-pixel-wide GUI,
height 114 + 18×rows, centred using the current GUI-scaled dimensions, with the
first slot at (8,18) and 18-pixel spacing. No fixed framebuffer coordinates or
extra scale multiplier are used. Changing GUI scale or window size recalculates
positions each frame. Custom menus that move their slots away from vanilla chest
positions are not covered by these chest-size presets.

In game: configure different layouts/colours on two keys, change GUI scale and
window size, and compare the waiting squares to the slots when each menu arrives.
Verify that disabling the overlay keeps the existing cursor-release behaviour.
