package net.lidl.lidlclient.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import java.util.regex.Pattern;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

/** Bounded scans of already loaded chunks, supplemented by the server's hotbar map. */
public final class DungeonMapScanner {
    private static final Pattern FLOOR = Pattern.compile("The Catacombs \\((E|[FM][1-7])\\)");
    private final RoomCatalog catalog = RoomCatalog.bundled();
    private final Map<Block, Integer> tokens = new IdentityHashMap<>();
    private static final Pattern INSTANCE=Pattern.compile("\\b\\d{2}/\\d{2}/\\d{2}\\s+(\\w{1,24})\\b");
    private int cursor;
    private Object level;
    private DungeonMapPixels.Calibration calibration;
    private boolean active;
    private String floor = "", instance = "";
    private final DungeonMapModel model = new DungeonMapModel();
    private final DungeonParty party = new DungeonParty();

    public DungeonMapModel model() { return model; }
    public boolean active() { return active&&DungeonMap.isEnabled(); }
    public String floor() { return floor; }
    private final DungeonPlayerMarkers markers=new DungeonPlayerMarkers();
    private final DungeonSecretSync sync=new DungeonSecretSync();
    public DungeonPlayerMarkers markers() {return markers;}
    public DungeonSecretSync sync() {return sync;}
    // Match Odin's scoreboard lobby identifier (up to six characters).
    public String instance() {return instance.substring(0,Math.min(6,instance.length()));}
    public DungeonParty party() { return party; }
    public void reset() { model.reset(); party.reset(); markers.reset(); sync.reset(); instance=""; cursor = 0; calibration = null; active = false; floor = ""; }

    public void tick(Minecraft client) {
        if(!DungeonMap.isEnabled()) return;
        if (client.level != level) { reset(); level = client.level; }
        if (client.level == null || client.player == null) { reset(); return; }
        var server = client.getCurrentServer();
        String host = server == null ? "" : server.ip.toLowerCase(Locale.ROOT).split(":", 2)[0];
        if (!host.equals("hypixel.net") && !host.endsWith(".hypixel.net")) { reset(); return; }
        String detected = "", detectedInstance="";
        for (var team : client.level.getScoreboard().getPlayerTeams()) {
            String text = (team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString())
                    .replaceAll("(?i)§[0-9a-fk-or]", "");
            var serverId=INSTANCE.matcher(text);if(serverId.find()) detectedInstance=serverId.group(1);
            if (text.contains("Queue")) continue;
            var match = FLOOR.matcher(text);
            if (match.find()) detected = match.group(1);
        }
        if (detected.isEmpty()) { reset(); return; }
        if (!floor.equals(detected)||(!instance.isEmpty()&&!detectedInstance.isEmpty()&&!instance.equals(detectedInstance))) reset();
        floor=detected;if(!detectedInstance.isEmpty()) instance=detectedInstance;
        active = true;
        if(client.getConnection()!=null) party.update(client.getConnection().getOnlinePlayers().stream()
                .sorted(Comparator.comparing((PlayerInfo info)->info.getTeam()==null?"":info.getTeam().getName())
                        .thenComparing(info->info.getProfile().name()))
                .map(info->info.getTabListDisplayName()).filter(Objects::nonNull).map(text->text.getString()).toList());
        int floorNumber = floor.equals("E") ? 0 : Character.digit(floor.charAt(1), 10);
        // Prefer the dungeon's usual slot, but allow moving the map elsewhere.
        for (int offset = 0; offset < 36; offset++) {
            int slot = offset == 0 ? 8 : offset <= 8 ? offset - 1 : offset;
            var stack = client.player.getInventory().getItem(slot);
            var id = stack.get(DataComponents.MAP_ID);
            if (id == null) continue;
            var data = client.level.getMapData(id);
            if (data == null) continue;
            var candidate = DungeonMapPixels.calibrate(data.colors, floorNumber);
            if (candidate == null) continue;
            calibration = candidate;
            DungeonMapPixels.update(model, data.colors, calibration);
            markers.map(client,party,data,calibration);
            break;
        }
        markers.update(client,party);
        sync.tick(client,this,catalog);
        // Six column probes per update; no background world access or chunk requests.
        for (int i = 0; i < 6; i++) {
            int index = cursor++ % (GRID * GRID), x = index % GRID, z = index / GRID;
            Tile tile = model.at(x,z);
            if (center(x,z) && tile.identified) continue;
            int worldX = WORLD_START + x * WORLD_STEP, worldZ = WORLD_START + z * WORLD_STEP;
            if (!client.level.hasChunk(worldX >> 4, worldZ >> 4)) continue;
            var pos = new BlockPos.MutableBlockPos(worldX, 0, worldZ);
            // Revisit known map doors: map discovery can precede the loaded-block scan.
            Door detectedDoor=Door.NORMAL;
            if(x%2!=z%2) {
                Block block=client.level.getBlockState(pos.setY(69)).getBlock();
                detectedDoor=DungeonDoorScan.classify(BuiltInRegistries.BLOCK.getKey(block).toString());
                if(tile.kind==Kind.DOOR) {model.observeDoor(x,z,detectedDoor);continue;}
                if(tile.kind==Kind.CONNECTION && detectedDoor==Door.NORMAL) continue;
            }
            int roof = -1;
            for (int y = 255; y >= 0; y--) {
                var state = client.level.getBlockState(pos.setY(y));
                if (!state.isAir() && !state.is(Blocks.GOLD_BLOCK)) { roof = y; break; }
            }
            if (roof < 0) continue;
            if (center(x,z)) {
                int hash = 1;
                for (int y = 140; y >= 12; y--) {
                    Block block = client.level.getBlockState(pos.setY(y)).getBlock();
                    int token = tokens.computeIfAbsent(block, b -> RoomCatalog.token(BuiltInRegistries.BLOCK.getKey(b).toString()));
                    hash = hash * 31 + token;
                }
                var room = catalog.find(hash);
                if (room != null) model.identify(x,z,room);
            } else if (x % 2 != z % 2) {
                boolean doorway = roof == 73 || roof == 74 || roof == 81 || roof == 82;
                tile.kind = doorway ? Kind.DOOR : Kind.CONNECTION;
                if(doorway) {
                    model.observeDoor(x,z,detectedDoor);
                    tile.type=switch(tile.door) {case BLOOD->Type.BLOOD;case ENTRANCE->Type.ENTRANCE;case WITHER->Type.UNKNOWN;default->Type.NORMAL;};
                    if(!tile.mapKnown) tile.state=tile.door==Door.WITHER?State.UNOPENED:State.DISCOVERED;
                }

            }
        }
    }
}


