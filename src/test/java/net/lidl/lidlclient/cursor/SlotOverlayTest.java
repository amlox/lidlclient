package net.lidl.lidlclient.cursor;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SlotOverlayTest {
    @Test void positionsMatchVanillaChestGeometryAtEveryGuiScaleAndRowCount() {
        for (int scale : new int[]{1,2,3,4}) {
            int width=(int)Math.ceil(1919.0/scale),height=(int)Math.ceil(1079.0/scale);
            for (int rows=1;rows<=6;rows++) {
                int left=(width-176)/2,top=(height-114-rows*18)/2;
                for (int slot=0;slot<rows*9;slot++) {
                    assertEquals(left+8+(slot%9)*18,SlotOverlayLayout.x(width,slot));
                    assertEquals(top+18+(slot/9)*18,SlotOverlayLayout.y(height,rows,slot));
                }
            }
        }
        assertEquals(240,SlotOverlayLayout.x(640,0));
        assertEquals(87,SlotOverlayLayout.y(360,6,0));
        assertEquals(384,SlotOverlayLayout.x(640,53));
        assertEquals(177,SlotOverlayLayout.y(360,6,53));
        assertEquals(16,SlotOverlayLayout.SIZE);
    }
    @Test void slotColoursStayIndependentAndSizeChangesKeepTheDraft() {
        var first=SlotOverlay.empty().withSlot(0,0xFFFF0000).withSlot(53,0xFF00FF00);
        var small=first.withRows(1);
        assertEquals(0xFF0000,first.slots().get(0));
        assertEquals(0x00FF00,small.withRows(6).slots().get(53));
        assertEquals(0x0000FF,first.withSlot(0,0x0000FF).slots().get(0));
        assertEquals(0xFF0000,first.slots().get(0));
        assertFalse(first.withSlot(0,null).slots().containsKey(0));
        assertThrows(UnsupportedOperationException.class,()->first.slots().put(1,0));
        assertThrows(IllegalArgumentException.class,()->first.withRows(7));
    }
    @Test void savedLayoutsRoundTripWithoutChangingExistingKeyLists() {
        var gson=new Gson();
        var overlays=Map.of("key.keyboard.r",SlotOverlay.empty().withSlot(10,0x55D9FF),
                "key.keyboard.g",new SlotOverlay(3,Map.of(0,0xFF0000)));
        var restored=SlotOverlayStore.read(gson.toJsonTree(overlays));
        assertEquals(overlays,restored);
        assertTrue(SlotOverlayStore.read(null).isEmpty());
        assertTrue(SlotOverlayStore.read(JsonParser.parseString("[\"key.keyboard.r\"]")).isEmpty());
    }
    @Test void malformedEntriesAreIgnoredWithoutLosingValidSlots() {
        var source=JsonParser.parseString("""
                {"key.keyboard.r":{"rows":99,"slots":{"0":123,"53":16777215,
                "54":42,"-1":42,"x":42,"1":"red","2":-1,"3":1.5,"4":16777216}},
                "key.keyboard.g":{"rows":3.5,"slots":[]},"key.keyboard.h":false}
                """);
        var restored=SlotOverlayStore.read(source);
        assertEquals(6,restored.get("key.keyboard.r").rows());
        assertEquals(Map.of(0,123,53,0xFFFFFF),restored.get("key.keyboard.r").slots());
        assertEquals(SlotOverlay.empty(),restored.get("key.keyboard.g"));
        assertFalse(restored.containsKey("key.keyboard.h"));
    }
    @Test void liveSquaresAreAlwaysTranslucentAndPreserveRgb() {
        assertEquals(0x4D55D9FF,SlotOverlay.translucent(0x55D9FF,30));
        assertEquals(0xCC55D9FF,SlotOverlay.translucent(0x55D9FF,100));
        assertEquals(0x0D55D9FF,SlotOverlay.translucent(0x55D9FF,0));
        assertEquals(0x4D55D9FF,SlotOverlay.translucent(0x55D9FF,Double.NaN));
    }
}
