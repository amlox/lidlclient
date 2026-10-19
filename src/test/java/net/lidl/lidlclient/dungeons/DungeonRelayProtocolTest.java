package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static net.lidl.lidlclient.dungeons.DungeonRelayProtocol.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

class DungeonRelayProtocolTest {
    private static final List<Cell> CELLS=List.of(new Cell(0,0),new Cell(1,0));
    @Test void usesOnlyTheFixedOdinRelayAndRejectsInvalidInstancePaths() {
        assertEquals("wss://ws.odtheking.com/m123AB",endpoint("m123AB").toString());
        assertNull(endpoint("../bad"));assertNull(endpoint("long-server-name"));assertNull(endpoint(""));assertNull(endpoint(null));
    }
    @Test void lidlPeersExchangeTaggedCountsWithoutChatOrOtherMods() {
        var packet=count("m123AB","F7","Teammate","Arrow Trap",1,CELLS);
        var update=decode(packet.toString(),"m123AB","F7",Set.of("teammate"));
        assertNotNull(update);assertEquals("Arrow Trap",update.room());assertEquals(1,update.found());assertEquals(CELLS,update.cells());
        assertEquals("Teammate",packet.get("playerName").getAsString());assertEquals(1,packet.get("foundSecrets").getAsInt());
        assertEquals(1,packet.getAsJsonArray("tiles").get(1).getAsJsonObject().get("x").getAsInt());
        // Native Odin reports are not accepted as Lidl reports.
        packet.remove("lidlclient");assertNull(decode(packet.toString(),"m123AB","F7",Set.of("Teammate")));
    }
    @Test void lateJoinRequestsAreScopedToLidlPartyMembersAndAreNotSecretReports() {
        String packet=request("m123AB","F7","Teammate").toString();
        assertEquals("Teammate",requestedBy(packet,"m123AB","F7",Set.of("Teammate")));
        assertNull(requestedBy(packet,"m123AC","F7",Set.of("Teammate")));
        assertNull(requestedBy(packet,"m123AB","M7",Set.of("Teammate")));
        assertNull(requestedBy(packet,"m123AB","F7",Set.of("Stranger")));
        assertNull(decode(packet,"m123AB","F7",Set.of("Teammate")));
    }
    @Test void rejectsOtherPartiesRunsFloorsAndMalformedOrOutOfBoundsCounts() {
        var packet=count("m123AB","F7","Teammate","Arrow Trap",1,CELLS);
        assertNull(decode(packet.toString(),"m123AB","F7",Set.of("Stranger")));
        assertNull(decode(packet.toString(),"m123AC","F7",Set.of("Teammate")));
        assertNull(decode(packet.toString(),"m123AB","M7",Set.of("Teammate")));
        packet.addProperty("foundSecrets",100);assertNull(decode(packet.toString(),"m123AB","F7",Set.of("Teammate")));
        packet.addProperty("foundSecrets",0.5);assertNull(decode(packet.toString(),"m123AB","F7",Set.of("Teammate")));
        assertNull(decode(count("m123AB","F7","Teammate","Room",1,List.of(new Cell(6,0))).toString(),"m123AB","F7",Set.of("Teammate")));
        assertNull(decode(count("m123AB","F7","Teammate","Room",1,List.of(new Cell(0,0),new Cell(0,0))).toString(),"m123AB","F7",Set.of("Teammate")));
        assertNull(decode("{bad","m123AB","F7",Set.of()));assertNull(decode(" ".repeat(8193),"m123AB","F7",Set.of()));
    }
    @Test void remoteCountsWaitForServerMapAndNeverMarkAnUnenteredRoomDiscovered() {
        var model=new DungeonMapModel();var catalog=RoomCatalog.bundled();var room=catalog.findName("Arrow Trap");assertNotNull(room);
        var update=new Update("Arrow Trap",1,"Teammate",List.of(new Cell(0,0)));
        assertFalse(DungeonSecretSync.apply(update,model,catalog));
        model.at(0,0).kind=Kind.ROOM;model.at(0,0).mapKnown=true;
        assertTrue(DungeonSecretSync.apply(update,model,catalog));assertEquals(1,model.at(0,0).foundSecrets);assertFalse(model.at(0,0).discovered);
        assertTrue(DungeonSecretSync.apply(new Update("Arrow Trap",0,"Teammate",List.of(new Cell(0,0))),model,catalog));assertEquals(1,model.at(0,0).foundSecrets);
        assertTrue(DungeonSecretSync.apply(new Update("Arrow Trap",99,"Teammate",List.of(new Cell(0,0))),model,catalog));assertEquals(1,model.at(0,0).foundSecrets);
        assertTrue(DungeonSecretSync.apply(new Update("Arrow Trap",1,"Teammate",List.of(new Cell(0,0),new Cell(4,4))),model,catalog));assertFalse(model.at(8,8).identified);
    }
    @Test void syncToggleCanBeDisabledAndReenabled() {
        boolean initial=DungeonMap.secretSync();try {DungeonMap.setSecretSync(false);assertFalse(DungeonMap.secretSync());DungeonMap.setSecretSync(true);assertTrue(DungeonMap.secretSync());} finally {DungeonMap.setSecretSync(initial);}
    }
}
