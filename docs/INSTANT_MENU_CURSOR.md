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
the server menu loads. The simulated container is rendered immediately when **Simulate container GUI** is enabled (default). No additional command is sent.

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
Use **Area: Container / Inventory** to edit either the chest or the player's 27
main inventory slots and 9 hotbar slots. Inventory highlights have their own colours
and follow the inventory when chest size changes. Changing row counts keeps selections
from other sizes, but only chest slots within the chosen size render. Slot numbers
in each editor area start at 1. The inventory's bottom row is the hotbar.

Each key has its own saved size and colours. Previously configured keybinds still
work and start with no highlighted slots. Removing a key also removes its overlay.
The preview renders Minecraft's vanilla chest texture at the chosen size, with
its inventory label, live player inventory and hotbar, and a loading title. This
is a visual simulation: it does not install a screen or change the player's active
container, so other mods' menu hotkeys retain gameplay focus. Clicking while waiting
cannot move items or send container actions. The real server GUI replaces it when
it arrives, without recentering the cursor.

**Simulate container GUI** (default on), **Show slot overlay**, and **Show item icons**
(default on) are independent controls. The simulation works even with no selected
highlights. All waiting visuals disappear on real-screen arrival, Escape, timeout,
focus loss, disconnect, or disabling the module.

Alignment uses Minecraft 26.1.2's vanilla chest geometry: a 176-pixel-wide GUI,
height 114 + 18×rows, centred using the current GUI-scaled dimensions, with the
first slot at (8,18) and 18-pixel spacing. No fixed framebuffer coordinates or
extra scale multiplier are used. Changing GUI scale or window size recalculates
positions each frame. Custom menus that move their slots away from vanilla chest
positions are not covered by these chest-size presets.

In game: configure different layouts/colours on two keys, change GUI scale and
window size, and compare the waiting squares to the slots when each menu arrives.
Verify that disabling the overlay keeps the existing cursor-release behaviour.

## Item icons, including Hypixel SkyBlock

In the slot editor, switch to **Mode: Icons**. Left-click a target slot to choose
an icon; right-click removes that slot's icon. Icons and colours can be combined in
either area. The picker includes your current inventory and items captured from the
last chest menu opened with that hotkey. To obtain menu-only icons, enable the module,
open the desired server menu with the configured key, wait for its items to load,
then close it and return to the editor. Pick icons and **Save** the layout. The last
menu picker collection lasts for this game session; selected icons persist in config.
Cancel/Escape discards icon edits just like colour edits. Clear removes all colours
and icons from the draft in both areas.

Icons store the actual Minecraft item stack, including server-provided components,
custom names, lore, enchantment glint and head profiles. This allows SkyBlock items
already received by your client to render using Minecraft's normal item renderer
and compatible active resource packs. It is not a catalog of every SkyBlock item:
unseen items must first be obtained or viewed in a server menu. Custom visuals that
require another mod's menu-specific renderer may differ in this preview.

No external item database, texture pack, web request, or new dependency is added.
Only selected icons are saved (at most 54 container + 36 inventory icons per key).
Stacks are decoded once per waiting preview, not every frame; each frame renders
at most 90 item slots. Invalid/unavailable saved stacks are skipped. Custom head
textures use Minecraft's usual profile/texture loading and caching. Performance
with large sets of custom heads still needs an in-game check; no FPS claim is made.

### 0.4 in-game checks

- Set 1 and 6 chest rows on different keys; confirm the full vanilla container and
  live player inventory render immediately, including when no colours are selected.
- Enable slot overlay, colour the first and last inventory/hotbar slots, change
  chest size and GUI scale, and compare alignment when the server menu arrives.
- Choose a SkyBlock custom head and glint item from the picker; save, restart and
  check their icons. Confirm colour edits and icons survive size changes and that
  Cancel, Clear, right-click removal, and both editor areas behave as described.
- Test timeout, Escape, focus loss, disconnect, and screen handoff with the usual
  menu mod. No item action should be sent by clicking the simulated container.
