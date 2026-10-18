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
