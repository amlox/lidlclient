package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

class DungeonFairyAndDoorTest {
    @Test void fairyRoomMapMarkersOnlyDiscoverItAndNeverApplyClearColours() {
        var model=new DungeonMapModel();model.identify(0,0,new RoomCatalog.Room("Fairy",Type.FAIRY,0,0));
        assertEquals(State.UNOPENED,DungeonMapStyle.state(model,List.of(0)));
        byte[] map=new byte[16384];for(int z=5;z<21;z++) for(int x=5;x<21;x++) map[z*128+x]=82;
        for(int middle:List.of(82,34,30)) {
            map[13*128+13]=(byte)middle;
            DungeonMapPixels.update(model,map,new DungeonMapPixels.Calibration(5,5,16));
            assertTrue(model.at(0,0).discovered);assertEquals(State.DISCOVERED,model.at(0,0).state);
            assertEquals(State.DISCOVERED,DungeonMapStyle.state(model,List.of(0)));
            for(boolean full:List.of(true,false)) assertEquals(DungeonMapColors.palette().room(Type.FAIRY),DungeonMapStyle.roomColor(model.at(0,0),State.GREEN,true,full,true,65));
        }
        model.at(0,0).state=State.CLEARED;assertEquals(State.DISCOVERED,DungeonMapStyle.state(model,List.of(0)));
    }
    @Test void specialDoorCapsTouchBothRoomEdgesInBothOrientationsRegardlessOfExploration() {
        for(Door type:List.of(Door.WITHER,Door.BLOOD)) for(boolean horizontal:List.of(true,false)) {
            int dx=horizontal?1:0,dz=horizontal?0:1;
            var model=new DungeonMapModel();model.identify(0,0,new RoomCatalog.Room("First",Type.NORMAL,1,0));
            model.identify(dx*2,dz*2,new RoomCatalog.Room("Second",Type.NORMAL,1,0));
            model.observeDoor(dx,dz,type);var cap=DungeonMapStyle.specialDoorBounds(dx,dz);
            var first=DungeonMapStyle.tile(0,0,Kind.ROOM);var second=DungeonMapStyle.tile(dx*2,dz*2,Kind.ROOM);
            if(horizontal) {assertTrue(cap.x()<first.x()+first.width());assertTrue(cap.x()+cap.width()>second.x());}
            else {assertTrue(cap.y()<first.y()+first.height());assertTrue(cap.y()+cap.height()>second.y());}
            for(boolean a:List.of(true,false)) for(boolean b:List.of(true,false)) {
                model.at(0,0).discovered=a;model.at(dx*2,dz*2).discovered=b;
                assertTrue(new DungeonMapVisibility(model,true).visible(dx,dz));
                assertEquals(DungeonMapColors.palette().door(type),DungeonMapStyle.doorColor(model,model.groups(),dx,dz,true));
            }
            // A stale generic kind cannot hide the full-map overlay or merge the rooms.
            model.at(dx,dz).kind=Kind.CONNECTION;
            assertTrue(DungeonMapStyle.specialDoor(model.at(dx,dz)));assertEquals(2,model.groups().size());
        }
    }
}
