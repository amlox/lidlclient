package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

class DungeonMapTeamTest {
    @Test void remoteMapPositionsSurviveEntityUnloadAndResetBetweenRuns() {
        var tracker=new DungeonMarkerPositions();
        var calibration=new DungeonMapPixels.Calibration(6,6,16);
        tracker.map(List.of(new DungeonMarkerPositions.Icon("icon-0",false,(byte)-60,(byte)-100,(byte)4)),List.of("Teammate"),"Me",calibration);
        var remote=tracker.get("teammate");assertNotNull(remote);
        assertEquals(34,remote.x());assertEquals(14,remote.z());assertEquals(90,remote.yaw());
        tracker.live("Teammate",-121,-185,180);assertEquals(54,tracker.get("teammate").x());
        tracker.map(List.of(),List.of("Teammate"),"Me",calibration);
        assertEquals(54,tracker.get("teammate").x());
        tracker.reset();assertNull(tracker.get("teammate"));
    }
    @Test void legitViewShowsOnlyServerRevealedRoomsAndColoursDoorsFromTheEnteredSide() {
        var model=new DungeonMapModel();model.at(0,0).kind=Kind.ROOM;model.at(0,0).discovered=true;
        var door=model.at(1,0);door.kind=Kind.DOOR;door.mapKnown=true;door.state=State.UNOPENED;
        model.at(2,0).kind=Kind.ROOM;model.at(2,0).mapKnown=true;
        var view=new DungeonMapVisibility(model,false);
        assertTrue(view.visible(1,0));assertTrue(view.visible(2,0));assertFalse(view.discovered(2,0));
        model.at(0,0).type=Type.PUZZLE;
        assertEquals(DungeonMapColors.palette().room(Type.PUZZLE),DungeonMapStyle.legitDoorColor(model,1,0));
        assertEquals(DungeonMapStyle.UNDISCOVERED_GREY,DungeonMapStyle.roomColor(model.at(2,0),State.UNOPENED,false,false,false,0));
        door.mapKnown=false;assertFalse(new DungeonMapVisibility(model,false).visible(1,0));
    }
    @Test void fourRoomStatesHaveDistinctFillsAndSecretCompletionPromotesToGreen() {
        int base=0xFF794600;
        var colors=java.util.Set.of(DungeonMapStyle.stateColor(base,State.UNOPENED,false,true,65),
                DungeonMapStyle.stateColor(base,State.DISCOVERED,true,true,65),
                DungeonMapStyle.stateColor(base,State.CLEARED,true,true,65),
                DungeonMapStyle.stateColor(base,State.GREEN,true,true,65));assertEquals(4,colors.size());
        var model=new DungeonMapModel();model.identify(0,0,new RoomCatalog.Room("Room",Type.NORMAL,3,0));
        assertEquals(State.UNOPENED,DungeonMapStyle.state(model,List.of(0)));
        model.at(0,0).discovered=true;assertEquals(State.DISCOVERED,DungeonMapStyle.state(model,List.of(0)));
        model.updateSecrets(0,0,3,3);assertEquals(State.DISCOVERED,DungeonMapStyle.state(model,List.of(0)));
        model.at(0,0).foundSecrets=2;model.at(0,0).state=State.CLEARED;assertEquals(State.CLEARED,DungeonMapStyle.state(model,List.of(0)));
        model.updateSecrets(0,0,3,3);assertEquals(State.GREEN,DungeonMapStyle.state(model,List.of(0)));
    }
}
