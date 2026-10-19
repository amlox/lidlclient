package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

class DungeonMapDisabledTest {
    @Test void disablingClearsTheRunAndStopsAllMapEntryPointsBeforeTouchingMinecraft() throws Exception {
        var field=DungeonMap.class.getDeclaredField("scanner");field.setAccessible(true);
        Object previous=field.get(null);boolean enabled=DungeonMap.isEnabled();
        var scanner=new DungeonMapScanner();
        try {
            field.set(null,scanner);DungeonMap.setEnabled(true);
            scanner.model().identify(0,0,new RoomCatalog.Room("Room",Type.NORMAL,3,0));
            scanner.party().update(java.util.List.of("[100] Teammate (Mage L)"));
            DungeonMap.setEnabled(false);
            assertFalse(scanner.active());assertEquals(Kind.EMPTY,scanner.model().at(0,0).kind);
            assertTrue(scanner.party().names().isEmpty());assertTrue(scanner.markers().markers().isEmpty());
            assertEquals("Map disabled",scanner.sync().status());
            // Null Minecraft inputs prove these paths perform no world/network/render work.
            assertDoesNotThrow(()->scanner.tick(null));
            assertDoesNotThrow(()->scanner.markers().update(null,null));
            assertDoesNotThrow(()->scanner.markers().map(null,null,null,null));
            assertDoesNotThrow(()->scanner.sync().local(null,0,0));
            assertDoesNotThrow(()->scanner.sync().tick(null,null,null));
            assertDoesNotThrow(()->DungeonMap.recordSecrets("1/3 Secrets"));
            assertDoesNotThrow(()->DungeonMapRenderer.draw(null,null,0,0,128,true,true,"F7"));
            assertDoesNotThrow(DungeonMap::open);
            DungeonMap.setEnabled(true);
            assertFalse(scanner.active());assertEquals(Kind.EMPTY,scanner.model().at(0,0).kind);
        } finally {scanner.reset();field.set(null,previous);DungeonMap.setEnabled(enabled);}
    }
}
