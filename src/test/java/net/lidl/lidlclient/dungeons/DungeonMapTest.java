package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

class DungeonMapTest {
    private static void pixel(byte[] pixels,int x,int z,int color) { pixels[z*128+x]=(byte)color; }
    private static void square(byte[] pixels,int x,int z,int size,int color) {
        for(int dz=0;dz<size;dz++) for(int dx=0;dx<size;dx++) pixel(pixels,x+dx,z+dz,color);
    }
    @Test void calibratesBothRoomSizesAndSmallFloorOffsets() {
        for(int size:new int[]{16,18}) {
            byte[] pixels=new byte[16384]; square(pixels,5,5,size,30);
            assertEquals(new DungeonMapPixels.Calibration(5,5,size),DungeonMapPixels.calibrate(pixels,7));
            assertEquals(new DungeonMapPixels.Calibration(22,11,size),DungeonMapPixels.calibrate(pixels,1));
        }
        assertNull(DungeonMapPixels.calibrate(new byte[16384],7));
        assertNull(DungeonMapPixels.calibrate(new byte[20],7));
    }
    @Test void readsRoomsCheckmarksDoorsAndConnectionsWithoutMergingAcrossDoors() {
        byte[] pixels=new byte[16384];
        square(pixels,5,5,16,30); // Entrance at (0,0).
        square(pixels,25,5,16,66); pixel(pixels,33,13,18); // Failed puzzle at (2,0).
        pixel(pixels,23,13,119); // Wither door between entrance and puzzle.
        square(pixels,45,5,16,63); pixel(pixels,53,13,30); // Complete normal room.
        pixel(pixels,43,13,63); pixel(pixels,43,9,63); // Large-room separator.
        var model=new DungeonMapModel();
        DungeonMapPixels.update(model,pixels,DungeonMapPixels.calibrate(pixels,7));
        assertEquals(Type.ENTRANCE,model.at(0,0).type);
        assertEquals(State.FAILED,model.at(2,0).state);
        assertEquals(State.GREEN,model.at(4,0).state);
        assertEquals(Kind.DOOR,model.at(1,0).kind);
        assertEquals(State.UNOPENED,model.at(1,0).state);
        assertEquals(2,model.groups().size());
        // Blank/unreceived map pixels never delete previously scanned rooms.
        DungeonMapPixels.update(model,new byte[16384],new DungeonMapPixels.Calibration(5,5,16));
        assertEquals(3,model.roomTiles());
    }
    @Test void catalogMatchesBundledRoomHashesAndKeepsNamesWhenMapUpdates() {
        var catalog=RoomCatalog.bundled();
        var admin=catalog.find(-908785306);
        assertNotNull(admin); assertEquals("Admin",admin.name());
        var model=new DungeonMapModel(); model.identify(0,0,admin);
        byte[] pixels=new byte[16384]; square(pixels,5,5,16,63); pixel(pixels,13,13,34);
        DungeonMapPixels.update(model,pixels,new DungeonMapPixels.Calibration(5,5,16));
        assertEquals("Admin",model.at(0,0).name);
        assertEquals(State.CLEARED,model.at(0,0).state);
        model.reset(); assertEquals(0,model.roomTiles()); assertTrue(model.groups().isEmpty());
    }
    @Test void groupsLShapedRoomsAndNormalizesDynamicBlocksForRoomHashes() {
        var model=new DungeonMapModel();
        var room=new RoomCatalog.Room("Large",Type.NORMAL,7,2);
        model.identify(0,0,room); model.identify(2,0,room); model.identify(0,2,room);
        model.at(1,0).kind=Kind.CONNECTION; model.at(0,1).kind=Kind.CONNECTION;
        assertEquals(1,model.groups().size()); assertEquals(3,model.groups().getFirst().size());
        assertEquals(RoomCatalog.token("minecraft:air"),RoomCatalog.token("minecraft:chest"));
        assertEquals(RoomCatalog.token("minecraft:air"),RoomCatalog.token("minecraft:oak_planks"));
        assertNotEquals(RoomCatalog.token("minecraft:air"),RoomCatalog.token("minecraft:stone"));
    }
}
