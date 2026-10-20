# GUI appearance

Open `/lidl` > **General settings**, then right-click **GUI appearance**.
Choose **Accent colour** or **Background colour**. Drag the colour square and hue
bar, or type a six-digit hex value. **Use colour** saves the choice; **Cancel**
and Escape discard it. **Default** restores that picker's original default, and
**Reset colours** restores both saved colours. Text stays light for readability.

Left-click the GUI appearance card to toggle the animated star background.
Colours still apply when stars are disabled. Colours and the animation toggle
survive restarts. Stars remain white, with stronger glow and a wider twinkle range.

The animated starfield submits one render state, using simple dust/connection
geometry rather than rounded panels per particle. Offscreen connections are culled.
Rounded panel curves are cached in a bounded cache and translated with the panel;
glow uses four gradient rings. The colour picker uses eight gradient quads rather
than thousands of tiny rectangles. No GIF decoding, frame uploads, or extra mod is
required.

Validation in game: compare frame times with `/lidl` closed, open with stars, and
open without stars at the same location/GUI scale. Check slide-in, category changes,
scrolling, dropdowns, and slider drags. Select colours, cancel/save/reset, resize,
and restart to verify persistence. Actual FPS gains depend on the client/GPU and
must be measured in Minecraft.
