package net.lidl.lidlclient.pets;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PetsMenuRulesTest {
    @Test void detectsOnlyNormalPetSelectionClicks() {
        assertTrue(PetsMenuRules.isPetSelection("Pets", true, true, true, List.of("§eClick to summon!")));
        assertTrue(PetsMenuRules.isPetSelection("Pets (2/4)", true, true, true, List.of("Click to despawn!")));
        assertFalse(PetsMenuRules.isPetSelection("Pets", true, true, true, List.of("Next Page")));
        assertFalse(PetsMenuRules.isPetSelection("Pets", false, true, true, List.of("Click to summon!")));
        assertFalse(PetsMenuRules.isPetSelection("Pets", true, true, false, List.of("Click to summon!")));
        assertFalse(PetsMenuRules.isPetSelection("Pets", true, false, true, List.of("Click to summon!")));
        assertFalse(PetsMenuRules.isPetSelection("Pet Item", true, true, true, List.of("Click to summon!")));
    }

    @Test void suppressesOnlyPetsAndOnlyWithinTheBoundedWindow() {
        var guard = new PetsMenuRules();
        assertFalse(guard.shouldSuppress("Pets", 0));
        guard.selected(100);
        assertTrue(guard.shouldSuppress("Pets", 101));
        assertTrue(guard.shouldSuppress("Pets (1/3)", 102));
        assertFalse(guard.shouldSuppress("SkyBlock Menu", 103));
        assertFalse(guard.shouldSuppress("Pets", 100 + PetsMenuRules.SUPPRESSION_NS));
    }

    @Test void explicitReopenAndClockResetCancelSuppression() {
        var guard = new PetsMenuRules();
        guard.selected(100);
        guard.reset();
        assertFalse(guard.shouldSuppress("Pets", 101));
        guard.selected(100);
        assertFalse(guard.shouldSuppress("Pets", 99));
    }
}
