package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DungeonMapDataTargetsTest {
    @Test void mapDecorationAccessorMatchesMinecraftField() throws Exception {
        var type=Class.forName("net.minecraft.world.level.saveddata.maps.MapItemSavedData",false,getClass().getClassLoader());
        assertEquals(Map.class,type.getDeclaredField("decorations").getType());
    }
    @Test void profileKeyAccessorMatchesMinecraftField() throws Exception {
        var type=Class.forName("net.minecraft.client.Minecraft",false,getClass().getClassLoader());
        assertEquals("net.minecraft.client.multiplayer.ProfileKeyPairManager",type.getDeclaredField("profileKeyPairManager").getType().getName());
    }
}

