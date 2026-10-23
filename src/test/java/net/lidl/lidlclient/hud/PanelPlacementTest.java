package net.lidl.lidlclient.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PanelPlacementTest {
    @Test void savedRelativePlacementAdaptsToResolutionAndGuiScale() {
        var position=new PanelPlacement(.75,.5);
        assertEquals(555,position.x(1000,260));
        assertEquals(160,position.y(600,280));
        assertEquals(180,position.x(500,260));
        assertEquals(10,position.y(300,280));
    }
    @Test void draggingClampsEdgesAndRoundTrips() {
        var position=new PanelPlacement(.5,.5);
        position.move(120,60,500,400,260,280);
        var restored=new PanelPlacement(position.xFraction(),position.yFraction());
        assertEquals(120,restored.x(500,260));
        assertEquals(60,restored.y(400,280));
        position.move(-100,900,500,400,260,280);
        assertEquals(0,position.x(500,260));
        assertEquals(120,position.y(400,280));
    }
    @Test void tinyViewportsDoNotLoseSavedPlacement() {
        var position=new PanelPlacement(.75,.25);
        position.move(100,100,100,100,260,280);
        assertEquals(0,position.x(100,260));
        assertEquals(0,position.y(100,280));
        assertEquals(555,position.x(1000,260));
        assertEquals(80,position.y(600,280));
    }
    @Test void corruptCoordinatesStayOnScreen() {
        var position=new PanelPlacement(Double.NaN,Double.POSITIVE_INFINITY);
        assertEquals(120,position.x(500,260));
        assertEquals(60,position.y(400,280));
        var outside=new PanelPlacement(-3,8);
        assertEquals(0,outside.x(500,260));
        assertEquals(120,outside.y(400,280));
    }
}
