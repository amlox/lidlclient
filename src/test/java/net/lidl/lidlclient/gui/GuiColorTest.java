package net.lidl.lidlclient.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiColorTest {
    @Test void hsvAndHexRoundTripsPreservePickerColours() {
        for (int rgb : new int[]{0xFF000000,0xFFFFFFFF,0xFFFF0000,0xFF00FF00,0xFF0000FF,0xFF51A7CE,0xFF05070D}) {
            double[] hsv = GuiColor.hsv(rgb);
            assertEquals(rgb,GuiColor.rgb(hsv[0],hsv[1],hsv[2]));
            assertEquals(rgb,GuiColor.parse(GuiColor.hex(rgb)));
        }
        assertEquals(0xFFAABBCC, GuiColor.parse(" aabbcc "));
        assertNull(GuiColor.parse("#123"));
        assertNull(GuiColor.parse("#GG0000"));
        assertNull(GuiColor.parse("#12345678"));
    }
}
