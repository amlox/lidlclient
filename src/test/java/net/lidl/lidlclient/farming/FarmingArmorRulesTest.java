package net.lidl.lidlclient.farming;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FarmingArmorRulesTest {
    @Test void allowsEveryApprovedSetAndMixedPieces() {
        for (String set : List.of("HELIANTHUS", "FERMENTO", "SQUASH", "CROPIE")) {
            assertTrue(FarmingArmorRules.allowedArmor(List.of(set + "_HELMET", set + "_CHESTPLATE",
                    set + "_LEGGINGS", set + "_BOOTS")));
        }
        assertTrue(FarmingArmorRules.allowedArmor(List.of("HELIANTHUS_HELMET", "FERMENTO_CHESTPLATE",
                "SQUASH_LEGGINGS", "CROPIE_BOOTS")));
    }

    @Test void rejectsMissingWrongAndLookalikePieces() {
        for (String head : List.of("", "RANCHERS_BOOTS", "FERMENTO", "FAKE_FERMENTO_HELMET")) {
            assertFalse(FarmingArmorRules.allowedArmor(List.of(head, "FERMENTO_CHESTPLATE",
                    "FERMENTO_LEGGINGS", "FERMENTO_BOOTS")));
        }
        assertFalse(FarmingArmorRules.allowedArmor(List.of()));
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
