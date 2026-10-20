# Instant Pets close

Enabled by default. Toggle **Instant Pets close** in /lidl > Cheats.

On Hypixel, a normal left-click on a pet with "Left-click to summon!" (or
"Click to summon!") lore sends the selection, then immediately closes the menu
in the same click callback, like selecting a pet and pressing Escape together.
The title matcher accepts **(1/2) Pets** (the paginated Hypixel title), as well as
**Pets** and **Pets (1/2)**. It uses Minecraft's inventory-click handling for the
current container state and stack hashes, clears local pickup prediction, and
closes before the next rendered frame. Server attempts to reopen only that menu
are suppressed for two seconds, with a close packet sent to keep it in sync.

Despawning the active pet, clicks with a non-empty cursor, page arrows,
favorites/right-click, shift-clicks, pet items and inventory slots
do not activate this feature. Explicitly typing /pets or /pet resets the suppression
so you can reopen immediately. Opening another menu or changing connections also
clears the guard.

The pet switch itself still waits for Hypixel. This removes the local GUI delay;
it cannot eliminate network latency or make server actions instant.
Live Hypixel testing is needed to verify the current pet lore and reopen timing.

The physical mouse click is intercepted before vanilla starts pickup or quick-craft,
not only at slot dispatch. This prevents the press/release path from putting the
pet on the cursor for a visible frame before the instant-close handler runs.
