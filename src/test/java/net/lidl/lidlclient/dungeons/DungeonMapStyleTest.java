package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;
import static net.lidl.lidlclient.dungeons.DungeonMapStyle.*;

class DungeonMapStyleTest {
    @Test void joinsHaveNoGapsButDoorsStayNarrow() {
        assertEquals(new Rect(6,6,16,16),tile(0,0,Kind.ROOM));
        assertEquals(new Rect(22,6,4,16),tile(1,0,Kind.CONNECTION));
        assertEquals(new Rect(22,11,4,6),tile(1,0,Kind.DOOR));
        assertEquals(new Rect(22,22,4,4),tile(1,1,Kind.CONNECTION));
        assertEquals(new Rect(106,106,16,16),tile(10,10,Kind.ROOM));
    }
    @Test void centersLabelsOverWholeRoomsWithoutPuttingLLabelsInEmptyCorners() {
        assertEquals(new Rect(6,6,36,16),label(List.of(0,2)));
        assertEquals(new Rect(6,6,36,36),label(List.of(0,2,22,24)));
        assertEquals(new Rect(6,6,36,16),label(List.of(0,2,22)));
    }
    @Test void markerCoordinatesMatchRoomCentersAndFailureWinsStateSelection() {
        assertEquals(14,world(-185));assertEquals(34,world(-153));
        var model=new DungeonMapModel();
        model.at(0,0).state=State.GREEN;model.at(2,0).state=State.FAILED;
        assertEquals(State.FAILED,state(model,List.of(0,2)));
        var tile=new Tile();tile.type=Type.PUZZLE;assertEquals(0xFF7B007B,color(tile));
        tile.state=State.UNOPENED;assertEquals(0xFF414141,color(tile));
    }
}
