# Dungeon map attribution

The LidlClient dungeon map adapts the grid layout, map calibration, palette
interpretation and vertical block-name hash algorithm from Noamm9/NoammAddons
(modern branch, CC0-1.0 Universal):

https://github.com/Noamm9/NoammAddons

Reference code revision: 3aa6806f567c8ad6df6097c1c9bb86f6dfc97dc2.
Relevant files: HotbarMapScanner.kt, DungeonScanner.kt, MapUtils.kt, ScanUtils.kt,
RoomType.kt and DoorType.kt. The source repository's LICENSE is CC0-1.0, the same
license as LidlClient's LICENSE.

Bundled rooms.json contains names, types, shapes, cores and total secret/crypt
counts selected from rooms-modern.json on NoammAddons' data branch, revision
3c7b32c471148899e68022cfd2afdfc843be91a1. Secret coordinates and all unrelated data
are omitted. No Noamm branding, textures, network services or binaries are used.

https://github.com/Noamm9/NoammAddons/blob/data/rooms-modern.json

The room-secret action-bar count pattern and total-matching rule also follow
NoammAddons' modern ActionBarParser.kt (main, revision
b2566e40173292f2b408a8ce85b0d255c825cb1a). The action-bar parser itself uses no network API.


Party map-marker indexing and map-decoration access follow modern NoammAddons'
MapUpdater.kt, TabListUtils.kt and IMapState.java (CC0-1.0).

## Lidl secret-sync transport

LidlClient uses the Odin WebSocket relay selected by the user. The independent
transport and JSON fields follow Odin's public DungeonMap.kt SyncPayload,
ClickGUIModule.kt endpoint, LocationUtils.kt scoreboard lobby identifier and
WebSocketConnection.kt at source tree
f2840a0aa7d1aea12321d6f3b287010ea7227a8a (BSD-3-Clause):
https://github.com/odtheking/Odin

LidlClient adds its own version, instance and floor fields and only accepts
LidlClient-tagged party reports. No Odin source implementation or assets are
bundled. Noamm and Skyblocker sync adapters, authentication and Skyblocker room
aliases have been removed. There are no account keys or service credentials in
source or artifacts. Relay usage does not imply endorsement.
