package net.lidl.lidlclient.cursor;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SlotOverlayTest {
    @Test void positionsMatchVanillaChestGeometryAtEveryGuiScaleAndRowCount() {
        // Fixed reference coordinates from vanilla chest layouts, including an
        // odd-sized framebuffer. Inputs are Window's ceil-scaled GUI dimensions.
        int[] firstX={879,400,240,160},lastX={1023,544,384,304};
        int[][] firstY={{491,482,473,464,455,446},{222,213,204,195,186,177},
                {132,123,114,105,96,87},{87,78,69,60,51,42}};
        int[][] lastY={{491,500,509,518,527,536},{222,231,240,249,258,267},
                {132,141,150,159,168,177},{87,96,105,114,123,132}};
        for (int scale=1;scale<=4;scale++) {
            int width=(int)Math.ceil(1919.0/scale),height=(int)Math.ceil(1079.0/scale);
            for (int rows=1;rows<=6;rows++) {
                assertEquals(firstX[scale-1],SlotOverlayLayout.x(width,0));
                assertEquals(lastX[scale-1],SlotOverlayLayout.x(width,rows*9-1));
                assertEquals(firstY[scale-1][rows-1],SlotOverlayLayout.y(height,rows,0));
                assertEquals(lastY[scale-1][rows-1],SlotOverlayLayout.y(height,rows,rows*9-1));
            }
        }
        assertEquals(258,SlotOverlayLayout.x(640,10));
        assertEquals(105,SlotOverlayLayout.y(360,6,10));
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
    @Test void inventoryAndHotbarAlignForEveryChestSize() {
        for (int rows=1;rows<=6;rows++) {
            int top=(360-(114+rows*18))/2;
            assertEquals(top+rows*18+31,SlotOverlayLayout.inventoryY(360,rows,0));
            assertEquals(top+rows*18+67,SlotOverlayLayout.inventoryY(360,rows,26));
            assertEquals(top+rows*18+89,SlotOverlayLayout.inventoryY(360,rows,27));
            assertEquals(top+rows*18+89,SlotOverlayLayout.inventoryY(360,rows,35));
            assertEquals(SlotOverlayLayout.x(640,8),SlotOverlayLayout.x(640,35));
        }
    }
    @Test void inventoryHighlightsAndIconsSurviveSizeChangesAndSaveRoundTrips() {
        String icon="{\"id\":\"minecraft:player_head\",\"count\":1}";
        var overlay=SlotOverlay.empty().withSlot(53,0x123456)
                .withColour(true,0,0xFF0000).withColour(true,35,0x00FF00)
                .withIcon(53,icon).withIcon(89,icon);
        var small=overlay.withRows(1);
        assertEquals(overlay,small.withRows(6));
        assertEquals(0x00FF00,small.inventorySlots().get(35));
        var restored=SlotOverlayStore.read(new Gson().toJsonTree(Map.of("key.keyboard.r",small)));
        assertEquals(small,restored.get("key.keyboard.r"));
        assertEquals(overlay.slots(),overlay.withColour(true,0,null).slots());
        assertFalse(overlay.withColour(true,0,null).inventorySlots().containsKey(0));
        assertFalse(overlay.withIcon(89,null).icons().containsKey(89));
        assertThrows(UnsupportedOperationException.class,()->overlay.inventorySlots().put(1,0));
        assertThrows(UnsupportedOperationException.class,()->overlay.icons().put(1,icon));
    }
    @Test void malformedInventoryAndIconsDoNotDiscardOldHighlights() {
        var restored=SlotOverlayStore.read(JsonParser.parseString("""
                {"r":{"rows":2,"slots":{"0":42},
                "inventorySlots":{"0":123,"35":456,"36":789,"-1":1,"x":1,"2":1.5},
                "icons":{"0":"{}","89":"{}","90":"{}","-1":"{}","1":42,"2":false}},
                "g":{"rows":1,"slots":{"1":55}}}
                """));
        assertEquals(Map.of(0,123,35,456),restored.get("r").inventorySlots());
        assertEquals(Map.of(0,"{}",89,"{}"),restored.get("r").icons());
        assertEquals(new SlotOverlay(1,Map.of(1,55)),restored.get("g"));
        assertEquals(Map.of(0,42),restored.get("r").slots());
        assertTrue(SlotOverlay.empty().withIcon(0,"x".repeat(65537)).icons().isEmpty());
    }
    @Test void liveSquaresAreAlwaysTranslucentAndPreserveRgb() {
        assertEquals(0x4D55D9FF,SlotOverlay.translucent(0x55D9FF,30));
        assertEquals(0xCC55D9FF,SlotOverlay.translucent(0x55D9FF,100));
        assertEquals(0x0D55D9FF,SlotOverlay.translucent(0x55D9FF,0));
        assertEquals(0x4D55D9FF,SlotOverlay.translucent(0x55D9FF,Double.NaN));
    }
}
