package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DungeonBossRoomTest {
    @Test void normalAndMasterBossBoundariesRequireBothCoordinatesBeyondTheFloorThreshold() {
        for(String prefix:List.of("F","M")) for(int floor=1;floor<=7;floor++) {
            double x=floor==1?-71:floor<=6?-39:-7,z=floor<=4?-39:-7;
            String name=prefix+floor;
            assertTrue(DungeonBossRoom.contains(name,x+1,z+1),name);
            assertFalse(DungeonBossRoom.contains(name,x,z+1),name);
            assertFalse(DungeonBossRoom.contains(name,x+1,z),name);
            assertFalse(DungeonBossRoom.contains(name,-185,-185),name);
        }
    }
    @Test void entranceUnknownFloorsAndNonFiniteCoordinatesNeverTriggerBossHiding() {
        for(String floor:List.of("E","F0","F8","M8","","F7 queue")) assertFalse(DungeonBossRoom.contains(floor,100,100));
        assertFalse(DungeonBossRoom.contains(null,100,100));assertFalse(DungeonBossRoom.contains("F7",Double.NaN,100));assertFalse(DungeonBossRoom.contains("F7",100,Double.POSITIVE_INFINITY));
    }
}
