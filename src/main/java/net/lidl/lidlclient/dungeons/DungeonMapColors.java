package net.lidl.lidlclient.dungeons;

import net.lidl.lidlclient.config.ClientConfig;
import net.minecraft.client.Minecraft;
import java.util.Map;

public final class DungeonMapColors {
    private static final DungeonMapPalette PALETTE=new DungeonMapPalette();
    private static boolean loaded;
    private DungeonMapColors() { }
    public static DungeonMapPalette palette() {return PALETTE;}
    public static void initialize() {
        if(loaded) return;
        loaded=true;
        for(var key:DungeonMapPalette.Key.values()) {
            Integer color=DungeonMapPalette.parse(ClientConfig.string("dungeon_map","color."+key.name()));
            if(color!=null) PALETTE.set(key,color);
        }
    }
    public static void save(Map<DungeonMapPalette.Key,Integer> colors) {
        Map<String,Object> values=new java.util.LinkedHashMap<>();
        for(var entry:colors.entrySet()) {
            PALETTE.set(entry.getKey(),entry.getValue());
            values.put("color."+entry.getKey().name(),DungeonMapPalette.hex(entry.getValue()));
        }
        ClientConfig.setAll("dungeon_map",values);
    }
    public static void open() {
        Minecraft client=Minecraft.getInstance();
        client.setScreen(new DungeonMapColorScreen(client.screen));
    }
}

