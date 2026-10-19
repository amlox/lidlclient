# Dungeon map

Open `/lidl` > QoL features > Dungeon map. Right-click its card for room overlays,
player heads, size, horizontal/vertical position and discovery display. These
settings save in the shared config. Disabling the module stops scans, marker updates, secret parsing, prediction,
rendering and relay syncing immediately, closes the relay and clears run data.
Re-enabling starts a fresh scan on the normal tick schedule. **Open dungeon map** opens the larger live view; hover a room for its full name
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
DEAD). Mort, player-skinned mobs, NPCs and unrelated players are not drawn. Party
members remain rendered even when unloaded, dead or in an undiscovered room.
The marker uses a loaded entity first, otherwise the base Hypixel map's decoration,
otherwise its retained last position. Dead members have a gray outline; out-of-grid
positions are pinned to the edge. Until the server has provided any position, the
member appears in a `?` row above the map instead of inventing a room position. Press **Rescan** if the room layout needs rebuilding. Changing
worlds, leaving a dungeon or disconnecting resets the map automatically.

Scanning runs on the client thread every five ticks with at most six column probes
per update. Room data is bundled, so no web service or Noamm installation is needed.
Source: modern NoammAddons, CC0-1.0. See THIRD_PARTY_NOTICES.md for pinned versions.
Live runs are needed to validate Hypixel's current map palette and scoreboard text.

## Discovery display

**Map view** switches between **Full scanned** (default) and **Entered rooms**
(the legit view). Full scanned shows rooms identified from map data and loaded
chunks. Legit view only reveals geometry supplied by Hypixel's dungeon map:
unentered rooms shown by that map use the Undiscovered rooms colour (grey by default), while rooms known only
from block scans stay hidden. Entered rooms use the configured room/state colours.
Unentered rooms have no name, secret overlay or hover identity in legit view.
A normal door shown by the server map takes the colour of its entered connected room
(first the left/top side if both are entered); when neither side is entered it is
the undiscovered colour too. Wither and Blood doors always override this with
their own door palette colours in both views. World-scan-only doors stay hidden. Identifying a room from blocks or
receiving a teammate's secret count never marks it entered. Player markers remain
visible in either view. Exploration persists until the run/world is reset.

**Darken undiscovered rooms** (default on) only affects Full scanned. The base colour of
undiscovered room tiles and joins is dimmed by **Undiscovered darkness (%)** while explored
rooms stay at their normal color. 0% keeps full brightness; 100% makes them black.
The default is 65% darkness (35% brightness). Exploration persists until the run/world is reset. All
these options save in the shared config. The old background opacity setting is
ignored; the map always remains transparent.

## Colours and secret progress

Click **Room / door colours**, then click a category to open its visual picker.
Drag inside the saturation/brightness square and along the hue strip. Before/after
swatches preview the choice; **Use colour** puts it in the palette draft. A hex
field is optional and accepts #RRGGBB, &#RRGGBB, bare six hex digits, 0xRRGGBB or
Minecraft codes (&0 through &f). Invalid input disables Use colour. **Default**
resets that category, and Cancel/Esc abandons that picker edit.

The overview has non-cleared/base, puzzle, trap, Mini Boss, Blood and Fairy colours;
Fairy rooms default to soft pink (#F2B6CE). **Undiscovered rooms** is a separate
colour (default #777777). Legit view uses it for all unentered room types.
Full scanned uses it only for unentered normal rooms; unentered puzzles, Fairy,
Blood and other special rooms retain their own configured category colours.
The Full scanned darkness setting applies to the selected base colour in either
case. Unknown types retain the default unknown-room colour.
There are also Normal, Wither and Blood door colours and independent **Cleared rooms** and
**Cleared + all secrets** colours. The latter two are exact body colours rather
than blends. Non-cleared special rooms retain their own category colours.
Cleared + all secrets requires a server-map clear plus every secret (or the server
map's full-completion mark). The white/green checkmarks still indicate these states.
The configured legit-view undiscovered colour takes priority over all state colours; normal doors
inherit connected room colours, while Full scanned uses the door categories.
Wither and Blood doors always retain their separate categories in both modes,
even after opening. Loaded doorway probes revisit known map doors so an initial
normal classification can be upgraded when the chunk is available.
**Save** applies the entire draft and persists it. **Defaults** resets the whole
draft; Cancel/Esc closes without applying. Drafts survive window resizing and
opening/closing individual pickers.

Names + secrets and the hover details display observed found/total secret counts.
They begin at 0/X, update from Hypixel's room-specific action-bar count when you
are in an identified room, and become X/X when the base map marks the room fully
complete. Unknown/mismatched action-bar totals are ignored. Without a connected LidlClient teammate, rooms you have not observed may stay at
0/X. Rooms with zero catalogue secrets (including zero-secret Mini Boss and puzzle
rooms) omit the secret counter completely.
All counts reset with the dungeon/world. Crypts remain catalogue totals.


## Shared progress and room states

Room bodies, joins and overlays distinguish undiscovered, discovered, cleared,
and cleared with all secrets. Failed puzzles retain their failure marker. The
server map supplies shared clears and full completion; synced counts can complete
the secret requirement for an already-cleared room.

**Secret sync (WebSockets)** is enabled by default. **Lidl secret sync status**
shows the single connection. LidlClient uses Odin's instance-scoped relay at
`wss://ws.odtheking.com/<lobbyId>` as requested; the lobby ID matches Odin's
six-character scoreboard lookup. It uses one socket, with no Noamm or Skyblocker
connections, room aliases, profile-key access, authentication requests, or /locraw
commands. No secret reports are sent to Hypixel chat and there is no chat fallback.

Only versioned LidlClient-tagged reports from current dungeon-roster names are
accepted, with matching instance/floor and validated room name, connected tile
coordinates and count bounds. Native untagged Odin reports are ignored. Reports
retain Odin's base wire fields so the relay can carry them; this does not promise
that other mods will ignore outgoing Lidl reports. The relay is unauthenticated:
roster/name checks reduce accidental cross-run updates but are not proof of a
sender's account identity. No Minecraft account tokens or profile keys are sent.

On connection, a scoped Lidl request asks peers to resend their observed room
snapshots, so clients joining or reconnecting can catch up. Repeat requests from
the same peer are limited to one per ten seconds. Counts are coalesced to at most one changed room per second, never decrease local
progress, and incoming messages are not relayed back. Unknown room geometry waits
up to 60 seconds for server-map data. Unrelated, malformed and oversized reports
are ignored. Disabling sync, leaving a dungeon or changing worlds closes the
socket; failures retry with bounded backoff and the map continues locally.
All LidlClient users in a party need this version and sync enabled. Two isolated test connections exchanged Lidl-tagged counts and catch-up requests
through the live relay using LidlClient's real version header. Live two-client
Hypixel testing is still needed to validate the full in-game path; automated tests
cover protocol and model behaviour.

## Puzzle prediction (legit)

**Puzzle prediction (legit)** is enabled by default and saved with the other map
settings. On an unentered room already revealed geometrically by Hypixel's map,
a full-scan catalogue match adds **P** for a puzzle or **N** for a normal room.
Unknown and other special room types get no prediction. These markers appear even
when normal room overlays are off, and disappear when entered or when using Full
scanned. No unseen geometry, room names or secrets are exposed by the marker, and
it never changes exploration, clears or secret progress. If the relevant chunks
have not loaded or the catalogue cannot identify a room, it stays unpredicted.

Fairy rooms are discovered when entered but are never classified as cleared or
fully secreted. They keep their Fairy palette colour and have no clear checkmark.
In Full scanned, observed Wither/Blood doors render in a final overlay above room
bodies, without requiring either adjacent room to be entered or map-known. Door
caps extend one pixel into both adjoining room edges for horizontal and vertical
connections. Opening a door preserves its category for the rest of the run.
