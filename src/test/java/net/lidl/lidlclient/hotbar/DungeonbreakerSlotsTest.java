package net.lidl.lidlclient.hotbar;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DungeonbreakerSlotsTest {
    @Test void findsEveryHotbarPosition() {
        for (int target = 0; target < 9; target++) {
            var names = new ArrayList<>(java.util.Collections.nCopies(9, ""));
            names.set(target, "§6Heroic Dungeonbreaker ✪✪✪");
            assertEquals(target, DungeonbreakerSlots.find(names, 0));
        }
    }

    @Test void matchesNamesWithoutCaseOrColorAndKeepsAnAlreadySelectedMatch() {
        assertTrue(DungeonbreakerSlots.matches("dungeonbreaker"));
        assertTrue(DungeonbreakerSlots.matches("Dungeon Breaker"));
        assertFalse(DungeonbreakerSlots.matches("NotDungeonbreaker"));
        assertEquals(1, DungeonbreakerSlots.find(List.of("Dungeonbreaker", "Dungeonbreaker"), 1));
    }

    @Test void ignoresMainInventoryAndHandlesMissingOrEmptyItems() {
        var names = new ArrayList<>(java.util.Collections.nCopies(10, ""));
        names.set(9, "Dungeonbreaker");
        assertEquals(-1, DungeonbreakerSlots.find(names, 0));
        assertEquals(-1, DungeonbreakerSlots.find(Arrays.asList(null, "", "Diamond Pickaxe"), 0));
        assertEquals(-1, DungeonbreakerSlots.find(List.of(), -1));
    }
}
