package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DungeonMapDataTargetsTest {
    @Test void mapDecorationAccessorMatchesMinecraftField() throws Exception {
        var type=Class.forName("net.minecraft.world.level.saveddata.maps.MapItemSavedData",false,getClass().getClassLoader());
        assertEquals(Map.class,type.getDeclaredField("decorations").getType());
    }
}
