package net.lidl.lidlclient.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlowClipTest {
    @Test void firstVisibleButtonHasRoomForItsCompleteTopGlow() {
        int clipTop = GlowClip.top(40, 40, 12);
        assertTrue(clipTop <= 40 - 10 - 1); // 10-unit halo plus antialias fringe.
        assertEquals(28, clipTop);
        assertEquals(28, GlowClip.top(40, 88, 12));
    }
    @Test void partiallyScrolledButtonsRemainClippedBelowTheHeading() {
        assertEquals(40, GlowClip.top(40, 39.9, 12));
        assertEquals(40, GlowClip.top(40, -100, 12));
        assertEquals(0, GlowClip.top(4, 4, 12));
    }
}
