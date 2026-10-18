package net.lidl.lidlclient.size;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerSizeRulesTest {
    @Test void builtInNamesHaveTheirRequestedSizesRegardlessOfCase() {
        assertEquals(0.5f, PlayerSizeRules.scaleFor("SyntocxLeftFoot", false, null, true));
        assertEquals(2f, PlayerSizeRules.scaleFor("syntocxrightfoot", false, null, true));
        assertEquals(1f, PlayerSizeRules.scaleFor("AnotherPlayer", false, null, true));
    }

    @Test void localSliderNeverOverridesOtherPlayers() {
        assertEquals(0.5f, PlayerSizeRules.scaleFor("SyntocxLeftFoot", false, 2.0, true));
        assertEquals(2f, PlayerSizeRules.scaleFor("SyntocxRightFoot", false, 2.0, true));
        assertEquals(1f, PlayerSizeRules.scaleFor("AnotherPlayer", false, 2.0, true));
        assertEquals(2f, PlayerSizeRules.scaleFor("SyntocxLeftFoot", true, 2.0, true));
    }

    @Test void resetUsesBuiltInDefaultAndDisabledAlwaysUsesNormalSize() {
        assertEquals(0.5f, PlayerSizeRules.scaleFor("SyntocxLeftFoot", true, null, true));
        assertEquals(2f, PlayerSizeRules.scaleFor("SyntocxRightFoot", true, null, true));
        assertEquals(1f, PlayerSizeRules.scaleFor("SyntocxRightFoot", false, null, false));
        assertEquals(1f, PlayerSizeRules.scaleFor("AnotherPlayer", true, 2.0, false));
    }

    @Test void invalidOverridesFallBackToDefaults() {
        assertEquals(0.5f, PlayerSizeRules.scaleFor("SyntocxLeftFoot", true, Double.NaN, true));
        assertEquals(1f, PlayerSizeRules.scaleFor(null, true, 9.0, true));
    }
}
