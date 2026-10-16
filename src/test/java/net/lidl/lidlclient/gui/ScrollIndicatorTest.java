package net.lidl.lidlclient.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScrollIndicatorTest {
    @Test void starReachesBothEndsAndDraggingRoundTrips() {
        for (double scroll : new double[]{0, 1, 250, 799, 800}) {
            double y = ScrollIndicator.position(scroll, 800, 50, 470);
            assertEquals(scroll, ScrollIndicator.scroll(y, 800, 50, 470), 1e-9);
        }
        assertEquals(50, ScrollIndicator.position(0, 800, 50, 470));
        assertEquals(470, ScrollIndicator.position(800, 800, 50, 470));
    }

    @Test void outsideDragsClampAndShortContentDoesNotScroll() {
        assertEquals(0, ScrollIndicator.scroll(-100, 800, 50, 470));
        assertEquals(800, ScrollIndicator.scroll(900, 800, 50, 470));
        assertEquals(0, ScrollIndicator.scroll(300, 0, 50, 470));
        assertEquals(50, ScrollIndicator.position(0, -100, 50, 470));
        assertEquals(0, ScrollIndicator.scroll(300, 800, 50, 50));
    }
}
