# Plinko and movable panels

Enable **Plinko** in `/lidl` → Gambling. It starts disabled. This version provides
the board and customization foundation; balls, bets, currency, payouts, upgrades,
and progression are not implemented yet.

Right-click Plinko for panel size (50–175%), background opacity (0–80%), border
thickness (1–3 GUI pixels), peg rows (6–16), and separate accent, peg, and panel
colours. The default is a translucent dark panel with a two-pixel border. Setting
background opacity to zero makes the interior completely transparent. Settings
and positions persist in the existing client configuration.

## Moving panels

Use `/lidlgui`, click **GUI editor** in General settings, or use Plinko's
**Move panels** action. Left-drag anywhere on a panel. Done or Escape exits.
Disabled panels appear as previews so they can be arranged without enabling them.
Only the editor handles panel mouse input; normal gameplay stays untouched.
Positions save on drag release and if the editor closes during a drag. The editor
is a temporary mode and is never reopened by loading saved settings.

The host uses Minecraft GUI coordinates, fits oversized panels to the viewport,
and stores position as a fraction of available horizontal/vertical travel. Window
resizing and GUI scale changes retain relative placement without moving panels
off-screen. Panels are hidden while other screens are open; the editor renders
its own previews without a duplicate HUD pass. Vanilla HUD hiding also applies.

## Adding a future panel

Implement `HudPanel` with a unique ID, enabled state, positive preferred dimensions,
and a render method. Register one instance in `PanelHost.initialize()`. Declare its
toggle and settings in `ModuleRegistry`. The host supplies fitted GUI bounds and
the editing flag, manages dragging, and saves placement under `panel_layout`.
Keep gameplay/progression state outside the editor and renderer. The editor does
not enable modules or advance games. The editor card bypasses module persistence
because it is a screen action, not a lasting feature toggle.

## Performance and validation

There is one shared HUD hook, no background tick simulation or animation, and no
blur, shaders, textures, or particles. Disabled panels return before rendering or
layout. Plinko caches peg and bucket coordinates until dimensions/rows change,
uses simple GUI fills and cached title components, and caps rows at 16 (168 pegs).
Dragging writes configuration only at release/exit, never every frame.

Placement tests cover fixed coordinates at different viewport sizes, edge
clamping, position restoration, tiny windows, and corrupt coordinates. Test live
with `/lidlgui` at several GUI scales, drag to every edge, reopen/restart, and
toggle Plinko off to check the editor preview and gameplay visibility separately.
Actual in-game visual appearance and FPS still need a Minecraft client check.
