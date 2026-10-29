package net.lidl.lidlclient.farming;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FarmingArmorRulesTest {
    @Test void allowsEveryApprovedSetAndMixedPieces() {
        for (String set : List.of("Helianthus", "Fermento", "Squash", "Cropie")) {
            assertTrue(FarmingArmorRules.allowedArmor(List.of(set + " Helmet", set + " Chestplate",
                    set + " Leggings", set + " Boots")));
        }
        assertTrue(FarmingArmorRules.allowedArmor(List.of("§6Mossy Helianthus Helmet ✦",
                "§aBustling FERMENTO Chestplate", "Ancient squash Leggings ★★★★★", "cropie Boots")));
    }

    @Test void rejectsMissingAndWrongPiecesInAnySlot() {
        var names = List.of("Helianthus Helmet", "Fermento Chestplate", "Squash Leggings", "Cropie Boots");
        for (int slot = 0; slot < 4; slot++) {
            for (String wrong : java.util.Arrays.asList("", null, "Rancher's Boots", "Diamond Helmet")) {
                var equipped = new java.util.ArrayList<>(names);
                equipped.set(slot, wrong);
                assertFalse(FarmingArmorRules.allowedArmor(equipped));
            }
        }
        assertFalse(FarmingArmorRules.allowedArmor(List.of()));
        assertFalse(FarmingArmorRules.allowedArmor(names.subList(0, 3)));
    }

    @Test void gardenRequiresSkyblockAndAnActualAreaOrLocation() {
        assertTrue(FarmingArmorRules.garden("§e§lSKYBLOCK", List.of("§a⏣ The Garden"), List.of()));
        assertTrue(FarmingArmorRules.garden("SKYBLOCK", List.of("§a The Garden"), List.of()));
        assertTrue(FarmingArmorRules.garden("SKYBLOCK CO-OP", List.of("⏣ Custom Plot"),
                List.of("§bArea: §aGarden")));
        assertFalse(FarmingArmorRules.garden("BED WARS", List.of("⏣ Garden"), List.of("Area: Garden")));
        assertFalse(FarmingArmorRules.garden("SKYBLOCK", List.of("⏣ Private Island", "Garden Level 15"),
                List.of("Visit the Garden", "Area: Hub")));
        assertFalse(FarmingArmorRules.garden("SKYBLOCK", List.of(), List.of()));
    }

    @Test void restrictsServerHostToHypixel() {
        assertTrue(FarmingArmorRules.hypixel("mc.hypixel.net:25565"));
        assertTrue(FarmingArmorRules.hypixel("HYPIXEL.NET"));
        assertFalse(FarmingArmorRules.hypixel("fakehypixel.net"));
        assertFalse(FarmingArmorRules.hypixel("hypixel.net.example.com"));
    }
}
