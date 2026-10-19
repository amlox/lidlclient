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
b2566e40173292f2b408a8ce85b0d255c825cb1a). No WebSocket or profile API is used.


Party map-marker indexing and map-decoration access follow modern NoammAddons'
MapUpdater.kt, TabListUtils.kt and IMapState.java (CC0-1.0). The former party-chat transport has been removed; native WebSocket adapters now
use the services selected by the user.


## Native secret-sync interoperability

LidlClient independently implements JSON wire formats observed in the following
public projects. Skyblocker Java implementation code is not bundled, linked or
copied; its service/envelope fields, UUID/count payloads and room-ID filenames are
used as protocol facts. The room alias table is generated from matching filenames,
public display names and the existing Lidl/Noamm catalogue.

- Odin (BSD-3-Clause), source tree f2840a0aa7d1aea12321d6f3b287010ea7227a8a:
  DungeonMap.kt SyncPayload, ClickGUIModule.kt endpoint and WebSocketConnection.kt.
  https://github.com/odtheking/Odin
- NoammAddons (CC0-1.0), 26.1.2 source tree
  792df9286ccf97daf5b01be0ceb4fb4bd1a5e53a: WebSocket.kt, PacketRegistry.kt,
  ApiAuth.kt, C2SPacketDungeonStart.kt, S2CPacketRoomSecrets.kt and
  S2CPacketDungeonRoom.kt. The certificate proof format follows Noamm's CC0 API.
  https://github.com/Noamm9/NoammAddons
- Skyblocker (LGPL-3.0), source tree ca1163e2b7309113cc4dd3c084829d4417606235:
  Payload, Service, Type, DungeonRoomSecretCountMessage, DungeonRoomMatchMessage
  and authentication request/response protocol definitions.
  https://github.com/SkyblockerMod/Skyblocker

Service credentials and account keys are not included in the source or artifacts.
Provider names identify interoperability and do not imply endorsement.
