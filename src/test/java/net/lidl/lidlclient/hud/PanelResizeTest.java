package net.lidl.lidlclient.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PanelResizeTest {
    private final PanelResize.Bounds start=new PanelResize.Bounds(100,80,260,280);
    @Test void cornerKeepsOppositeCornerAnchored() {
        assertEquals(new PanelResize.Bounds(50,40,310,320),
            PanelResize.resize(start,5,-50,-40,800,600,220,240));
    }
    @Test void minimumAndViewportLimitsApply() {
        assertEquals(new PanelResize.Bounds(140,120,220,240),
            PanelResize.resize(start,5,999,999,800,600,220,240));
        assertEquals(new PanelResize.Bounds(100,80,700,520),
            PanelResize.resize(start,10,999,999,800,600,220,240));
    }
    @Test void tinyViewportCanStillContainPanel() {
        var tiny=new PanelResize.Bounds(0,0,160,120);
        assertEquals(tiny,PanelResize.resize(tiny,10,-100,-100,160,120,220,240));
    }
    @Test void edgesAndInteriorAreDistinct() {
        assertEquals(5,PanelResize.edges(101,81,100,80,260,280));
        assertEquals(2,PanelResize.edges(359,200,100,80,260,280));
        assertEquals(0,PanelResize.edges(200,200,100,80,260,280));
    }
}
