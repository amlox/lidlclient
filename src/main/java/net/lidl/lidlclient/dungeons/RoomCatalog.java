package net.lidl.lidlclient.dungeons;

import com.google.gson.JsonParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bundled Noamm modern room signatures; no network request at runtime. */
public final class RoomCatalog {
    public record Room(String name, DungeonMapModel.Type type, int secrets, int crypts) { }
    private final Map<Integer, Room> byCore = new HashMap<>();
    private final Map<Integer, Room> byNameHash = new HashMap<>();
    private final Map<String,Room> byName=new HashMap<>();
    private final Map<String,String> skyAliases=new HashMap<>();
    private final java.util.Set<Integer> collisions=new java.util.HashSet<>();

    public RoomCatalog(Reader reader) {
        for (var element : JsonParser.parseReader(reader).getAsJsonArray()) {
            var json = element.getAsJsonObject();
            Room room = new Room(json.get("name").getAsString(),
                    DungeonMapModel.Type.valueOf(json.get("type").getAsString()),
                    json.has("secrets") ? json.get("secrets").getAsInt() : 0,
                    json.has("crypts") ? json.get("crypts").getAsInt() : 0);
            for (var core : json.getAsJsonArray("cores")) byCore.put(core.getAsInt(), room);
            int hash=room.name().hashCode();
            if(byNameHash.containsKey(hash)&&!byNameHash.get(hash).name().equals(room.name())) collisions.add(hash);
            byNameHash.put(hash,room);
            byName.put(normalize(room.name()),room);
        }
    }

    public static RoomCatalog bundled() {
        InputStream stream = RoomCatalog.class.getResourceAsStream("/assets/lidlclient/dungeons/rooms.json");
        if (stream == null) throw new IllegalStateException("Missing dungeon room catalog");
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            RoomCatalog catalog=new RoomCatalog(reader);
            try(InputStream aliases=RoomCatalog.class.getResourceAsStream("/assets/lidlclient/dungeons/skyblocker-room-aliases.json")) {
                if(aliases!=null) com.google.gson.JsonParser.parseReader(new InputStreamReader(aliases,StandardCharsets.UTF_8)).getAsJsonObject()
                        .entrySet().forEach(entry->catalog.skyAliases.put(entry.getKey(),entry.getValue().getAsString()));
            }
            return catalog;
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public Room find(int core) { return byCore.get(core); }
    public Room findNameHash(int hash) {return collisions.contains(hash)?null:byNameHash.get(hash);}
    private static String normalize(String name) {return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    public Room findName(String name) {return name==null?null:byName.get(normalize(skyAliases.getOrDefault(name,name)));}
    public String skyName(String canonical) {
        return skyAliases.entrySet().stream().filter(entry->entry.getValue().equals(canonical)).map(Map.Entry::getKey).sorted().findFirst().orElse(null);
    }

    /** Matches Noamm's 140..12 descending block-name hash, including ignored dynamic blocks. */
    public static int token(String name) {
        return (switch (name) {
            case "minecraft:chest", "minecraft:trapped_chest", "minecraft:piston_head",
                 "minecraft:moving_piston", "minecraft:water", "minecraft:lava",
                 "minecraft:fire", "minecraft:soul_fire" -> "minecraft:air";
            default -> name.endsWith("_planks") ? "minecraft:air" : name;
        }).hashCode();
    }
}


