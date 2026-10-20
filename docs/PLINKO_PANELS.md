# Plinko and movable panels

Enable **Plinko** in `/lidl` → Gambling. It starts disabled. Right-click for
background opacity (0–80%), border thickness (1–3 GUI pixels), separate accent,
peg and panel colours, and **Drop test**. Size and row-count sliders have been
removed. The board has twelve triangular rows: one peg, then two, through twelve.

## Editor

Use `/lidlgui`, General settings → **GUI editor**, or Plinko → **Move panels**.

- Drag inside a panel to move it.
- Drag any edge or corner to resize it; corner handles appear only in the editor.
- Middle-click a panel to open its module/category with settings expanded and
  scrolled into view. Escape returns to the editor.
- Plinko's minimum size is 220 × 240 GUI pixels. Width and height are independent.
- Done or Escape exits. Disabled panels remain editable previews.

Positions and dimensions persist in the existing client configuration. Dragging
saves on release or screen exit, not every frame. The editor is a temporary mode
and never opens automatically when settings load. When the viewport is smaller
than a panel, its displayed dimensions are capped to fit; saved dimensions are
retained so expanding the window restores them. Relative placement keeps panels
on-screen through GUI-scale and window changes. Resizing anchors the opposite
edge/corner and clamps to both minimum size and the viewport.

Only the editor handles panel mouse input. HUD panels are hidden while other
screens are open; the editor renders its own previews without a duplicate HUD
pass. Vanilla HUD hiding applies. The editor does not use Minecraft's menu blur.

## Drop test

Click **Drop test** at the bottom of the board in the editor, or use the module's
Drop test action (which opens the editor). This works with disabled previews too.
Each ball starts at the center, falls toward the first peg, then follows a random
left/right bounce path through all twelve rows into a bucket. It rests briefly
and fades. Up to eight concurrent drops are allowed; the button shows when full.
Disabling Plinko clears existing drops. Drops use elapsed monotonic time, so
animation speed is independent of frame rate, and expired drops are removed on
the next render or spawn. Hidden drops do no work and may finish while hidden.
Resizing maps the same normalized path to the new board without restarting it.

This is a visual animation prototype, not a physics/economy simulation. There are
no balances, bets, payouts, profit calculations, or upgrades. `PlinkoDrop` is
separate from rendering so future outcome logic can supply animation paths.

## Future panels and performance

Implement `HudPanel` with a unique module ID, enabled state, preferred/minimum
dimensions, and render method. Optionally implement its local-coordinate click
handler for editor controls. Register an instance in `PanelHost.initialize()` and
its toggle/settings in `ModuleRegistry`. Matching IDs provide the middle-click
settings shortcut automatically. The host owns sizing, placement, and input;
feature state remains outside the editor.

There is one shared HUD hook and no simulation tick loop. Disabled panels do no
render/layout work outside previews. Plinko caches geometry until dimensions
change, draws 78 pegs with simple fills, and caps animated balls at eight. It uses
no blur, textures, particles, or per-frame path generation. Tests cover placement,
anchored resizing, minimum/viewport limits, triangular path contacts, bucket
landing, expiration, and frame-rate independence. In-game appearance, mouse
interaction, and FPS still need a Minecraft client check.
