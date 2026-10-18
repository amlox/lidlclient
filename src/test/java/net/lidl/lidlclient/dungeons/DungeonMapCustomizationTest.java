package net.lidl.lidlclient.dungeons;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;
import static net.lidl.lidlclient.dungeons.DungeonMapPalette.Key.*;

class DungeonMapCustomizationTest {
    @Test void parsesColorCodesAndKeepsDoorAndRoomColorsIndependent() {
        assertEquals(0xFF123ABC,DungeonMapPalette.parse("#123abc"));
        assertEquals(0xFF123ABC,DungeonMapPalette.parse("&#123ABC"));
        assertEquals(0xFF123ABC,DungeonMapPalette.parse("0x123ABC"));
        assertEquals(0xFFFF5555,DungeonMapPalette.parse("&c"));
        assertNull(DungeonMapPalette.parse("#123"));assertNull(DungeonMapPalette.parse("#ZZZZZZ"));
        var palette=new DungeonMapPalette();
        palette.set(BLOOD,0x123456);palette.set(BLOOD_DOOR,0x654321);
        palette.set(WITHER_DOOR,0xABCDEF);palette.set(NORMAL_DOOR,0x112233);
        assertEquals(0xFF123456,palette.room(Type.BLOOD));assertEquals(0xFF654321,palette.door(Door.BLOOD));
        assertEquals(0xFFABCDEF,palette.door(Door.WITHER));assertEquals(0xFF112233,palette.door(Door.NORMAL));
        assertEquals("#ABCDEF",DungeonMapPalette.hex(palette.door(Door.WITHER)));
    }
    @Test void darknessEndpointsKeepAlphaAndIntermediateStrengthScalesRgb() {
        assertEquals(0xFF804020,DungeonMapVisibility.darken(0xFF804020,0));
        assertEquals(0xFF402010,DungeonMapVisibility.darken(0xFF804020,50));
        assertEquals(0xFF000000,DungeonMapVisibility.darken(0xFF804020,100));
    }
    @Test void secretsStartAtZeroThenTrackServerCountAcrossTheWholeRoom() {
        var model=new DungeonMapModel();var room=new RoomCatalog.Room("Room",Type.NORMAL,7,1);
        model.identify(0,0,room);model.identify(2,0,room);model.at(1,0).kind=Kind.CONNECTION;
        var group=model.groups().getFirst();
        assertEquals("0/7",DungeonSecretProgress.label(model.foundSecrets(group),7));
        var count=DungeonSecretProgress.parse("1200/1200❤   §73/7 Secrets§r   100 Mana");
        assertNotNull(count);model.updateSecrets(2,0,count.found(),count.total());
        assertEquals(3,model.foundSecrets(group));
        model.updateSecrets(0,0,7,8);assertEquals(3,model.foundSecrets(group));
        model.updateSecrets(0,0,1,7);assertEquals(3,model.foundSecrets(group));
        model.at(0,0).state=State.GREEN;assertEquals(7,model.foundSecrets(group));
        assertNull(DungeonSecretProgress.parse("8/7 Secrets"));assertNull(DungeonSecretProgress.parse("1200/1200❤"));
        assertEquals("0/0",DungeonSecretProgress.label(0,0));
    }
    @Test void openingWitherDoorDoesNotLoseItsSeparateColorCategory() {
        var model=new DungeonMapModel();var calibration=new DungeonMapPixels.Calibration(5,5,16);
        byte[] pixels=new byte[16384];pixels[13*128+23]=(byte)119;
        DungeonMapPixels.update(model,pixels,calibration);assertEquals(Door.WITHER,model.at(1,0).door);
        pixels[13*128+23]=(byte)63;
        DungeonMapPixels.update(model,pixels,calibration);assertEquals(Door.WITHER,model.at(1,0).door);
    }
}
