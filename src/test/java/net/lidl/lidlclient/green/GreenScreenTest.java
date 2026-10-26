package net.lidl.lidlclient.green;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GreenScreenTest {
    @Test void strengthChangesAlphaWithoutChangingGreenHue() {
        assertEquals(0x0000FF00,GreenScreen.tint(0));
        assertEquals(0x5900FF00,GreenScreen.tint(35));
        assertEquals(0x8000FF00,GreenScreen.tint(50));
        assertEquals(0xFF00FF00,GreenScreen.tint(100));
        assertEquals(0x0000FF00,GreenScreen.tint(-10));
        assertEquals(0xFF00FF00,GreenScreen.tint(110));
        assertEquals(0x5900FF00,GreenScreen.tint(Double.NaN));
        assertEquals(0x5900FF00,GreenScreen.tint(Double.POSITIVE_INFINITY));
    }
}
