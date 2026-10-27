package net.lidl.lidlclient.cursor;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class SlotIconEditorTest {
    @Test void menuImportPreservesPositionsGapsAndColoursAndReplacesOnlyMenuIcons() {
        var draft=SlotOverlay.empty().withSlot(2,123).withColour(true,4,456)
                .withIcon(0,"stale").withIcon(53,"hidden stale").withIcon(54,"inventory");
        var menu=new MenuIconSnapshot(3,Map.of(1,"first",20,"last"));
        var imported=draft.withRows(menu.rows()).importIcons(false,menu.icons());
        assertEquals(3,imported.rows());
        assertEquals(Map.of(1,"first",20,"last",54,"inventory"),imported.icons());
        assertEquals(draft.slots(),imported.slots());
        assertEquals(draft.inventorySlots(),imported.inventorySlots());
        assertEquals("stale",draft.icons().get(0)); // Editing remains a draft until Save.
    }
    @Test void inventoryImportMapsEverySlotAndClearsOldIconsInEmptyPositions() {
        Map<Integer,String> inventory=IntStream.range(0,36).boxed()
                .collect(Collectors.toMap(i -> i,i -> "item "+i));
        var draft=SlotOverlay.empty().withIcon(3,"menu").withColour(true,35,123);
        var imported=draft.importIcons(true,inventory);
        for (int slot=0;slot<36;slot++) assertEquals("item "+slot,imported.icons().get(54+slot));
        assertEquals("menu",imported.icons().get(3));
        assertEquals(draft.inventorySlots(),imported.inventorySlots());
        assertEquals(Map.of(3,"menu",89,"hotbar"),imported.importIcons(true,Map.of(35,"hotbar")).icons());
        assertEquals(Map.of(3,"menu"),imported.importIcons(true,Map.of()).icons());
    }
    @Test void emptyMenusKeepTheirRowCountAndClearStaleMenuIcons() {
        var menu=new MenuIconSnapshot(2,Map.of());
        var imported=SlotOverlay.empty().withIcon(1,"old").withIcon(80,"inventory")
                .withRows(menu.rows()).importIcons(false,menu.icons());
        assertEquals(2,imported.rows());
        assertEquals(Map.of(80,"inventory"),imported.icons());
        Map<Integer,String> source=new HashMap<>(Map.of(0,"icon"));
        var snapshot=new MenuIconSnapshot(6,source);
        source.clear();
        assertEquals(Map.of(0,"icon"),snapshot.icons());
        assertThrows(IllegalArgumentException.class,() -> new MenuIconSnapshot(1,Map.of(9,"outside")));
    }
    @Test void inventoryOrderIsMainRowsThenHotbarWithoutCompactingEmptySlots() {
        for (int slot=0;slot<27;slot++) assertEquals(slot+9,SlotEditorLayout.inventoryIndex(slot));
        for (int slot=27;slot<36;slot++) assertEquals(slot-27,SlotEditorLayout.inventoryIndex(slot));
        assertEquals(36,IntStream.range(0,36).map(SlotEditorLayout::inventoryIndex).distinct().count());
    }
    @Test void pickerAndEditorShareSlotSizeAndHitTargetsAtEveryMenuSize() {
        for (int height:new int[]{240,270,360,720}) for (int rows=1;rows<=6;rows++) {
            var layout=SlotEditorLayout.forScreen(640,height,rows);
            assertTrue(layout.pitch()<=18);
            for (int slot=0;slot<rows*9;slot++) {
                assertEquals(slot,layout.iconSlotAt(layout.x(slot)+.5,layout.y(false,slot)+.5));
                assertTrue(layout.y(false,slot)<layout.y(true,0));
            }
            for (int slot=0;slot<36;slot++)
                assertEquals(54+slot,layout.iconSlotAt(layout.x(slot)+.5,layout.y(true,slot)+.5));
            assertEquals(-1,layout.iconSlotAt(layout.x(0)+layout.size(),layout.top()));
            assertEquals(-1,layout.iconSlotAt(layout.x(0),layout.y(true,27)-1));
            assertTrue(layout.y(true,35)+layout.size()<height-28);
        }
    }
}
