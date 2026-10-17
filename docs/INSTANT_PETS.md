# Instant Pets close

Enabled by default. Toggle **Instant Pets close** in /lidl > SkyBlock.

On Hypixel, a normal left-click on a pet with "Click to summon!" or
"Click to despawn!" lore sends one selection packet directly without running local pickup prediction,
so the pet icon never moves onto the cursor. It immediately closes
the Pets chest menu locally. Server attempts to reopen only that menu are suppressed
for two seconds, with a close packet sent so the server also knows it is closed.

Clicks with a non-empty cursor, page arrows, favorites/right-click, shift-clicks, pet items and inventory slots
do not activate this feature. Explicitly typing /pets or /pet resets the suppression
so you can reopen immediately. Opening another menu or changing connections also
clears the guard.

The pet switch itself still waits for Hypixel. This removes the local GUI delay;
it cannot eliminate network latency or make server actions instant.
Live Hypixel testing is needed to verify the current pet lore and reopen timing.
