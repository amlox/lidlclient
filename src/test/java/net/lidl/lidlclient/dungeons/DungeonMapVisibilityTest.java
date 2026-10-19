package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

class DungeonMapVisibilityTest {
    private static void square(byte[] data,int x,int z,int color) {
        for(int dz=0;dz<16;dz++) for(int dx=0;dx<16;dx++) data[(z+dz)*128+x+dx]=(byte)color;
    }
    @Test void identifyingLoadedRoomsNeverCountsAsEnteringThem() {
        var model=new DungeonMapModel();
        model.identify(0,0,new RoomCatalog.Room("Scanned",Type.NORMAL,5,1));
        assertFalse(model.at(0,0).discovered);
        assertEquals(State.UNKNOWN,model.at(0,0).state);
        assertTrue(new DungeonMapVisibility(model,true).visible(0,0));
        assertFalse(new DungeonMapVisibility(model,false).visible(0,0));
        byte[] pixels=new byte[16384]; square(pixels,5,5,85);
        DungeonMapPixels.update(model,pixels,new DungeonMapPixels.Calibration(5,5,16));
        model.identify(0,0,new RoomCatalog.Room("Scanned",Type.NORMAL,5,1));
        assertEquals(State.UNOPENED,model.at(0,0).state);
        assertTrue(new DungeonMapVisibility(model,false).visible(0,0));
        assertEquals(DungeonMapStyle.UNDISCOVERED_GREY,DungeonMapStyle.roomColor(model.at(0,0),State.UNOPENED,false,false,false,0));
        square(pixels,5,5,63);
        DungeonMapPixels.update(model,pixels,new DungeonMapPixels.Calibration(5,5,16));
        assertTrue(model.at(0,0).discovered);
        assertTrue(new DungeonMapVisibility(model,false).visible(0,0));
        // A missing map update doesn't undo exploration, and reset clears it.
        DungeonMapPixels.update(model,new byte[16384],new DungeonMapPixels.Calibration(5,5,16));
        assertTrue(model.at(0,0).discovered);
        model.reset();assertFalse(model.at(0,0).discovered);
    }
    @Test void discoveryRevealsTheWholeConnectedRoomButNotItsUnenteredNeighbor() {
        var model=new DungeonMapModel();
        var room=new RoomCatalog.Room("Long room",Type.NORMAL,5,1);
        model.identify(0,0,room);model.identify(2,0,room);model.identify(4,0,room);
        model.at(1,0).kind=Kind.CONNECTION;model.at(3,0).kind=Kind.DOOR;
        model.at(0,0).discovered=true;
        var entered=new DungeonMapVisibility(model,false);
        assertTrue(entered.visible(0,0));assertTrue(entered.visible(1,0));assertTrue(entered.visible(2,0));
        assertFalse(entered.visible(4,0));assertFalse(entered.visible(3,0));
        assertTrue(DungeonMapVisibility.marker(model,entered,-185,-185));
        assertFalse(DungeonMapVisibility.marker(model,entered,-121,-185));
        var full=new DungeonMapVisibility(model,true);
        assertTrue(full.visible(4,0));assertFalse(full.discovered(4,0));assertTrue(full.discovered(2,0));
        assertEquals(0xFF592C16,DungeonMapVisibility.darken(0xFFFF8040));
    }
    @Test void legitUnenteredJoinsNeedServerMapGeometryAndNeverRevealBlockOnlyRooms() {
        var model=new DungeonMapModel();var room=new RoomCatalog.Room("Long room",Type.NORMAL,5,0);
        model.identify(0,0,room);model.identify(2,0,room);model.identify(4,0,new RoomCatalog.Room("Block only",Type.PUZZLE,0,0));
        model.at(0,0).mapKnown=true;model.at(2,0).mapKnown=true;model.at(1,0).kind=Kind.CONNECTION;
        var mask=new DungeonMapVisibility(model,false);assertTrue(mask.visible(0,0));assertTrue(mask.visible(2,0));
        assertFalse(mask.visible(1,0));assertFalse(mask.visible(4,0));
        model.at(1,0).mapKnown=true;mask=new DungeonMapVisibility(model,false);
        assertTrue(mask.visible(1,0));assertFalse(mask.discovered(1,0));
        assertEquals(DungeonMapStyle.UNDISCOVERED_GREY,DungeonMapStyle.roomColor(model.at(0,0),State.UNOPENED,false,false,false,0));
    }
    @Test void dungeonRosterExcludesMortMobsUnrelatedPlayersAndDeadPlayers() {
        var party=new DungeonParty();
        party.update(List.of("[270] [MVP+] ActualPlayer (Mage L)","§a[98] Teammate (Tank XX)",
                "[100] DeadPlayer (DEAD)","Mort","[NPC] Mort","[Lvl 50] Zombie", "[100] Random (Merchant)"));
        assertTrue(party.show("ActualPlayer",false));assertTrue(party.show("teammate",false));
        assertFalse(party.show("Mort",false));assertFalse(party.show("Zombie",false));
        assertFalse(party.show("Random",false));assertFalse(party.show("DeadPlayer",false));
        assertFalse(party.show("UnrelatedRealPlayer",false));assertTrue(party.show("Myself",true));
        party.reset();assertFalse(party.show("ActualPlayer",false));
    }
}

