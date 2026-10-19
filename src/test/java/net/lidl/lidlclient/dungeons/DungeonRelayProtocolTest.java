package net.lidl.lidlclient.dungeons;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.security.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static net.lidl.lidlclient.dungeons.DungeonRelayProtocol.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

class DungeonRelayProtocolTest {
    private final UUID sender=UUID.fromString("12345678-1234-1234-1234-123456789012");
    private Update decode(Backend backend,JsonObject json) {return DungeonRelayProtocol.decode(backend,json.toString(),"mini1",Set.of("Friend"),Set.of(sender));}
    @Test void odinUsesNativeNamesAndTileCoordinatesAndRejectsNonPartyPlayers() {
        JsonObject json=count(Backend.ODIN,"mini1","Friend",sender,"Arrow Trap",null,1,List.of(new Cell(2,3)));
        Update result=decode(Backend.ODIN,json);assertNotNull(result);assertEquals(List.of(new Cell(2,3)),result.cells());assertEquals(1,result.found());
        json.addProperty("playerName","NotInParty");assertNull(decode(Backend.ODIN,json));
        json.addProperty("playerName","Friend");json.getAsJsonArray("tiles").get(0).getAsJsonObject().addProperty("x",99);assertNull(decode(Backend.ODIN,json));
    }
    @Test void noammRegistersRunAndParsesNativeCountAndRoomPackets() {
        var start=noammStart("mini1","F7",List.of("Friend","Me"),new Cell(2,3));
        assertEquals("dungeon_start",start.get("type").getAsString());assertEquals(4,start.getAsJsonObject("entrance").get("first").getAsInt());assertEquals(6,start.getAsJsonObject("entrance").get("second").getAsInt());
        assertEquals(1,decode(Backend.NOAMM,count(Backend.NOAMM,"mini1","Friend",sender,"Arrow Trap",null,1,List.of())).found());
        Update room=decode(Backend.NOAMM,noammRoom("Arrow Trap",new Cell(2,3)));assertEquals(-1,room.found());assertEquals(List.of(new Cell(2,3)),room.cells());
    }
    @Test void skyblockerUsesScopedEnvelopesUuidAndRoomIds() {
        JsonObject publish=count(Backend.SKYBLOCKER,"mini1","Friend",sender,"Arrow Trap","arrow-trap-1",1,List.of());
        assertEquals("publish",publish.get("type").getAsString());publish.addProperty("type","response");
        Update result=decode(Backend.SKYBLOCKER,publish);assertNotNull(result);assertEquals("arrow-trap-1",result.room());
        publish.addProperty("serverId","otherRun");assertNull(decode(Backend.SKYBLOCKER,publish));
        publish.addProperty("serverId","mini1");publish.getAsJsonObject("message").addProperty("uuid",UUID.randomUUID().toString());assertNull(decode(Backend.SKYBLOCKER,publish));
        var catalog=RoomCatalog.bundled();assertEquals("Arrow Trap",catalog.findName("arrow-trap-1").name());assertEquals("arrow-trap-1",catalog.skyName("Arrow Trap"));
        assertNull(count(Backend.SKYBLOCKER,"mini1","Friend",sender,"Unknown",null,1,List.of()));
    }
    @Test void receivedMetadataAndCountsUpdateOtherRoomsWithoutMarkingThemEntered() {
        var model=new DungeonMapModel();var catalog=RoomCatalog.bundled();var cells=List.of(new Cell(2,3));
        var metadata=new Update("Arrow Trap",-1,"Friend",cells);assertFalse(DungeonSecretSync.apply(metadata,model,catalog));
        model.at(4,6).kind=Kind.ROOM;model.at(4,6).mapKnown=true;assertTrue(DungeonSecretSync.apply(metadata,model,catalog));
        assertTrue(DungeonSecretSync.apply(new Update("arrow-trap-1",1,sender.toString(),List.of()),model,catalog));
        assertEquals(1,model.foundSecrets(List.of(6*GRID+4)));assertFalse(model.at(4,6).discovered);
        DungeonSecretSync.apply(new Update("Arrow Trap",99,"Friend",cells),model,catalog);assertEquals(1,model.foundSecrets(List.of(6*GRID+4)));
        assertNull(DungeonRelayProtocol.decode(Backend.NOAMM,"not json","mini1",Set.of(),Set.of()));
    }
    @Test void signedCertificateProofUsesRealModIdentityAndNeverIncludesPrivateKeysOrAccessTokens() throws Exception {
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KeyPair keys=generator.generateKeyPair();
        JsonObject proof=DungeonRelayAuth.proof(sender,keys.getPublic(),new byte[]{1,2,3},1234,keys.getPrivate(),"26.1.2","test");
        assertEquals("lidlclient",proof.get("mod").getAsString());assertFalse(proof.toString().contains("accessToken"));assertFalse(proof.toString().contains("privateKey"));
        var signed=proof.getAsJsonObject("signedData");Signature verifier=Signature.getInstance("SHA256withRSA");verifier.initVerify(keys.getPublic());verifier.update(Base64.getDecoder().decode(signed.get("original").getAsString()));
        assertTrue(verifier.verify(Base64.getDecoder().decode(signed.get("signed").getAsString())));
    }
    @Test void syncToggleCanActuallyDisableNetworkConnections() {
        boolean before=DungeonMap.secretSync();try {DungeonMap.setSecretSync(false);assertFalse(DungeonMap.secretSync());DungeonMap.setSecretSync(true);assertTrue(DungeonMap.secretSync());}finally {DungeonMap.setSecretSync(before);}
    }
}
