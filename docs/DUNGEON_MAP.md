# Dungeon map

Open `/lidl` > QoL features > Dungeon map. Right-click its card for room overlays,
player heads, size, horizontal/vertical position and discovery display. These
settings save in the shared config. The toggle controls the HUD. `/lidlmap` or
**Open dungeon map** opens the larger live view; hover a room for its full name
and its observed secret progress plus total crypts. Secret progress starts at 0/X.

The feature activates on Hypixel when the scoreboard identifies a Catacombs floor
(Entrance, normal F1–F7 or master M1–M7). It samples the server's dungeon map for
room types, connections, doors, unopened rooms, clears and failed puzzles. It also
hashes vertical block columns in already-loaded chunks against a bundled catalogue
of 140 modern rooms to identify names. Long, L-shaped and 2x2 rooms are grouped by
their connections. Missing/unreceived rooms remain unknown; no chunks are requested
and the map does not predict rooms the server has not sent. The boss arena lies
outside the normal room grid and is not mapped.

White checkmarks mean cleared, green checkmarks mean fully complete, red crosses
mean failed and `?` means unopened. The compact Noamm-style square uses 16px rooms,
4px joins and slim doors on a completely transparent canvas. There is no background
fill, outer border, title bar or placeholder grid. Room colors distinguish entrance (green),
blood (red), fairy (pink), puzzle (purple), trap (orange), champion (gold), rare
(light gray) and normal (brown). Large-room overlays are centered across connected
rooms; names wrap and scale to fit rather than being shortened to a few letters.

**Room style** selects Checkmarks (default), Room names or Names + secrets.
Secret labels use found/total (for example 0/7, then 3/7). **Room text scale**, **Player head scale**
and **Map stats** control text size, marker size and the optional compact footer.
Player heads use actual skins, include their hat layers and rotate with player yaw.
Your head has a green outline; other nearby loaded party members have dark outlines.
Other heads must match a dungeon roster row in the tab list (class and level, or
DEAD). Mort, player-skinned mobs, NPCs and unrelated players are not drawn. Dead
party members are hidden. Unloaded teammates do
not have markers. Press **Rescan** if the room layout needs rebuilding. Changing
worlds, leaving a dungeon or disconnecting resets the map automatically.

Scanning runs on the client thread every five ticks with at most six column probes
per update. Room data is bundled, so no web service or Noamm installation is needed.
Source: modern NoammAddons, CC0-1.0. See THIRD_PARTY_NOTICES.md for pinned versions.
Live runs are needed to validate Hypixel's current map palette and scoreboard text.

## Discovery display

**Map view** switches between **Full scanned** (default) and **Entered rooms**.
Full scanned shows the layout identified from map data and loaded chunks. Entered
rooms only shows room groups that the base Hypixel dungeon map has marked
discovered, plus their visible connections. Identifying a room from blocks never
marks it entered. The server map is the reference for exploration; it is not a
record of which individual party member entered a room. Until a valid dungeon map
is received, Entered rooms stays empty. Hidden rooms are also hidden from overlays,
hover details, markers and the optional room count.

**Darken undiscovered rooms** (default on) only affects Full scanned. Undiscovered
room tiles, joins and names are dimmed by **Undiscovered darkness (%)** while explored
rooms stay at their normal color. 0% keeps full brightness; 100% makes them black.
The default is 65% darkness (35% brightness). Exploration persists until the run/world is reset. All
these options save in the shared config. The old background opacity setting is
ignored; the map always remains transparent.

## Colours and secret progress

Click **Room / door colours** in the Dungeon map settings to edit puzzle, base,
trap, Mini Boss and Blood rooms, plus Normal, Wither and Blood doors. Each has
its own input and live colour swatch. Inputs accept #RRGGBB, &#RRGGBB, bare six
hex digits, 0xRRGGBB or a Minecraft colour code (&0 through &f). Save applies all
valid colours and stores them in the shared config; invalid input disables Save.
Defaults restores the default input values, then Save applies them. Cancel/Esc
keeps the previous palette. Room colours and door colours are independent, and a
Wither door keeps its category after opening.

Names + secrets and the hover details display observed found/total secret counts.
They begin at 0/X, update from Hypixel's room-specific action-bar count when you
are in an identified room, and become X/X when the base map marks the room fully
complete. Unknown/mismatched action-bar totals are ignored. Incomplete rooms you
have not observed may stay at 0/X; this is not party-wide secret synchronization.
All counts reset with the dungeon/world. Crypts remain catalogue totals.
