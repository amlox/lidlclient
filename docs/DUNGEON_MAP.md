# Dungeon map

Open `/lidl` > QoL features > Dungeon map. Right-click its card for room overlays,
player heads, size, horizontal/vertical position and background opacity. These
settings save in the shared config. The toggle controls the HUD. `/lidlmap` or
**Open dungeon map** opens the larger live view; hover a room for its full name
and its total possible secrets/crypts. These are catalogue totals, not found counts.

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
4px joins, slim doors, a translucent background and a thin white border, without
the old title bar or placeholder grid. Room colors distinguish entrance (green),
blood (red), fairy (pink), puzzle (purple), trap (orange), champion (gold), rare
(light gray) and normal (brown). Large-room overlays are centered across connected
rooms; names wrap and scale to fit rather than being shortened to a few letters.

**Room style** selects Checkmarks (default), Room names or Names + secrets.
Secret labels show catalogue totals only. **Room text scale**, **Player head scale**
and **Map stats** control text size, marker size and the optional compact footer.
Player heads use actual skins, include their hat layers and rotate with player yaw.
Your head has a green outline; other nearby loaded players have dark outlines. Unloaded teammates do
not have markers. Press **Rescan** if the room layout needs rebuilding. Changing
worlds, leaving a dungeon or disconnecting resets the map automatically.

Scanning runs on the client thread every five ticks with at most six column probes
per update. Room data is bundled, so no web service or Noamm installation is needed.
Source: modern NoammAddons, CC0-1.0. See THIRD_PARTY_NOTICES.md for pinned versions.
Live runs are needed to validate Hypixel's current map palette and scoreboard text.
