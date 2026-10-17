package net.lidl.lidlclient.size;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerSizeAxesTest {
    @Test void localAxesAreIndependentAndDoNotAffectOtherPlayers() {
        assertEquals(new PlayerSizeRules.Axes(0.5f, 2f, 3f),
                PlayerSizeRules.axesFor("Me", true, 0.5, 2.0, 3.0, true));
        assertEquals(new PlayerSizeRules.Axes(1, 1, 1),
                PlayerSizeRules.axesFor("Other", false, 0.5, 2.0, 3.0, true));
    }

    @Test void builtInPlayersRemainUniformAndResetAxesUseTheirDefaults() {
        assertEquals(new PlayerSizeRules.Axes(0.5f, 0.5f, 0.5f),
                PlayerSizeRules.axesFor("SyntocxLeftFoot", false, 1.0, 2.0, 3.0, true));
        assertEquals(new PlayerSizeRules.Axes(3, 3, 3),
                PlayerSizeRules.axesFor("SyntocxRightFoot", true, null, null, null, true));
        assertEquals(new PlayerSizeRules.Axes(3, 2, 3),
                PlayerSizeRules.axesFor("SyntocxRightFoot", true, null, 2.0, null, true));
    }

    @Test void disablingResetsAllVisualAxes() {
        assertEquals(new PlayerSizeRules.Axes(1, 1, 1),
                PlayerSizeRules.axesFor("SyntocxRightFoot", true, 0.5, 2.0, 3.0, false));
    }

    @Test void migrationKeepsOldUniformSizeAndPrefersSavedAxes() {
        assertEquals(0.5, PlayerSizeRules.migrateAxis(null, 0.5));
        assertEquals(2.0, PlayerSizeRules.migrateAxis(2.0, 0.5));
        assertNull(PlayerSizeRules.migrateAxis(Double.NaN, null));
        assertNull(PlayerSizeRules.migrateAxis(null, Double.POSITIVE_INFINITY));
    }
}
