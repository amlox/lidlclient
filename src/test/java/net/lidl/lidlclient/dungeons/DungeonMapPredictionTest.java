package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;
import static net.lidl.lidlclient.dungeons.DungeonMapPalette.Key.*;

class DungeonMapPredictionTest {
    @Test void scannedSpecialDoorsOverrideNormalColoursInBothViewsAndPersistAfterOpening() {
        var model=new DungeonMapModel();model.identify(0,0,new RoomCatalog.Room("Room",Type.PUZZLE,0,0));model.at(0,0).discovered=true;
        model.observeDoor(1,0,Door.NORMAL);model.at(1,0).mapKnown=true;
        assertEquals(DungeonMapColors.palette().room(Type.PUZZLE),DungeonMapStyle.doorColor(model,model.groups(),1,0,false));
        for(var block:List.of("minecraft:coal_block","minecraft:red_terracotta")) {
            Door detected=DungeonDoorScan.classify(block);model.observeDoor(1,0,detected);
            for(boolean full:List.of(true,false)) assertEquals(DungeonMapColors.palette().door(detected),DungeonMapStyle.doorColor(model,model.groups(),1,0,full));
            model.observeDoor(1,0,Door.NORMAL);assertEquals(detected,model.at(1,0).door);
            // Both an open middle and brown side pixel used to turn doors into connections.
            byte[] map=new byte[16384];map[13*128+23]=63;map[9*128+23]=63;
            DungeonMapPixels.update(model,map,new DungeonMapPixels.Calibration(5,5,16));
            assertEquals(Kind.DOOR,model.at(1,0).kind);assertEquals(detected,model.at(1,0).door);assertTrue(model.at(1,0).mapKnown);
        }
        assertEquals(Door.NORMAL,DungeonDoorScan.classify("minecraft:air"));
    }
    @Test void doorDetectionDoesNotTurnRoomCentresOrJunctionsIntoDoors() {
        var model=new DungeonMapModel();model.observeDoor(0,0,Door.WITHER);model.observeDoor(1,1,Door.BLOOD);
        assertEquals(Kind.EMPTY,model.at(0,0).kind);assertEquals(Kind.EMPTY,model.at(1,1).kind);
        model.observeDoor(0,1,Door.BLOOD);assertEquals(Kind.DOOR,model.at(0,1).kind);
        model.observeDoor(0,1,Door.WITHER);assertEquals(Door.BLOOD,model.at(0,1).door);
    }
    @Test void fairyAndUndiscoveredColoursAreIndependentAndSaveAsOrdinaryPaletteKeys() {
        var palette=DungeonMapColors.palette();int fairy=palette.get(FAIRY),unknown=palette.get(UNDISCOVERED);
        assertEquals(0xFFF2B6CE,new DungeonMapPalette().room(Type.FAIRY));
        try {
            palette.set(FAIRY,0xF0A0BC);palette.set(UNDISCOVERED,0x123456);
            var room=new Tile();room.kind=Kind.ROOM;room.type=Type.FAIRY;
            assertEquals(0xFFF0A0BC,DungeonMapStyle.roomColor(room,State.DISCOVERED,true,false,false,0));
            assertEquals(0xFF123456,DungeonMapStyle.roomColor(room,State.UNOPENED,false,false,true,65));
            assertEquals(0xFFF0A0BC,DungeonMapStyle.roomColor(room,State.UNOPENED,false,true,false,65));
            assertEquals(DungeonMapVisibility.darken(0xFFF0A0BC,50),DungeonMapStyle.roomColor(room,State.UNOPENED,false,true,true,50));
            assertEquals("#123456",DungeonMapPalette.hex(palette.get(UNDISCOVERED)));
        } finally {palette.set(FAIRY,fairy);palette.set(UNDISCOVERED,unknown);}
    }
    @Test void predictionOnlyLabelsConfirmedNormalOrPuzzleRoomsInLegitView() {
        var model=new DungeonMapModel();model.identify(0,0,new RoomCatalog.Room("Puzzle name",Type.PUZZLE,0,0));
        Tile room=model.at(0,0);
        assertEquals("P",DungeonMapStyle.prediction(room,false,false,true));
        room.type=Type.NORMAL;assertEquals("N",DungeonMapStyle.prediction(room,false,false,true));
        assertNull(DungeonMapStyle.prediction(room,true,false,true));assertNull(DungeonMapStyle.prediction(room,false,true,true));assertNull(DungeonMapStyle.prediction(room,false,false,false));
        room.identified=false;assertNull(DungeonMapStyle.prediction(room,false,false,true));
        room.identified=true;room.type=Type.FAIRY;assertNull(DungeonMapStyle.prediction(room,false,false,true));
        assertFalse(room.discovered);assertEquals(State.UNKNOWN,room.state);
    }
    @Test void onlyNormalUnenteredFullScanRoomsUseTheUndiscoveredPaletteColour() {
        var palette=DungeonMapColors.palette();int previous=palette.get(UNDISCOVERED);
        try {
            palette.set(UNDISCOVERED,0x123456);
            var room=new Tile();room.kind=Kind.ROOM;room.state=State.UNOPENED;
            for(Type type:Type.values()) {
                room.type=type;int full=type==Type.NORMAL?0xFF123456:palette.room(type);
                assertEquals(full,DungeonMapStyle.roomColor(room,State.UNOPENED,false,true,false,65),type.name());
                assertEquals(DungeonMapVisibility.darken(full,50),DungeonMapStyle.roomColor(room,State.UNOPENED,false,true,true,50),type.name());
                assertEquals(0xFF123456,DungeonMapStyle.roomColor(room,State.UNOPENED,false,false,false,0),type.name());
            }
        } finally {palette.set(UNDISCOVERED,previous);}
    }
}
