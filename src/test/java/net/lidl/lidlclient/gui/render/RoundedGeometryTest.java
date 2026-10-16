package net.lidl.lidlclient.gui.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoundedGeometryTest {
    @Test void emittedFanAndRingMatchVanillaGuiQuadWinding() {
        var vertices = new java.util.ArrayList<float[]>();
        var mesh = new RoundedGeometry.MeshBuilder((x, y, color) -> vertices.add(new float[]{x, y}));
        float[] inner = RoundedGeometry.contour(0, 0, 120, 38, 8, 0.25f, 12);
        float[] outer = RoundedGeometry.contour(0, 0, 120, 38, 8, -0.25f, 12);
        mesh.fan(inner, 0xFFFFFFFF);
        mesh.ring(outer, inner, 0x00FFFFFF, 0xFFFFFFFF);
        assertEquals(0, vertices.size() % 4);
        boolean hasArea = false;
        for (int i = 0; i < vertices.size(); i += 4) {
            float[] a = vertices.get(i), b = vertices.get(i + 1), c = vertices.get(i + 2);
            double area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
            // Minecraft's GUI quads run top-left, bottom-left, bottom-right, top-right.
            assertTrue(area <= 0.001, "A reversed triangle can disappear with GUI face culling");
            hasArea |= area < -0.001;
        }
        assertTrue(hasArea);
    }

    @Test void outlinePreservesFractionalCoordinatesAndMatchesFramebufferFringe() {
        for (int guiScale : new int[]{1, 2, 3, 4, 6}) {
            float pixel = 1f / guiScale;
            float[] outside = RoundedGeometry.contour(10.25f, 20.25f, 160, 38, 8, -pixel / 2, 12);
            float[] inside = RoundedGeometry.contour(10.25f, 20.25f, 160, 38, 8, pixel / 2, 12);
            float minOuter = Float.POSITIVE_INFINITY;
            float minInner = Float.POSITIVE_INFINITY;
            for (int i = 0; i < outside.length; i += 2) {
                minOuter = Math.min(minOuter, outside[i]);
                minInner = Math.min(minInner, inside[i]);
            }
            assertEquals(10.25 - pixel / 2, minOuter, 1e-5);
            assertEquals(pixel, minInner - minOuter, 1e-5);
            assertEquals(1, (minInner - minOuter) * guiScale, 1e-4);
        }
    }

    @Test void oversizedCornerRadiiRemainFiniteAndInsideBounds() {
        float[] points = RoundedGeometry.contour(0, 0, 4, 4, 30, 0, 12);
        for (int i = 0; i < points.length; i++) {
            assertTrue(Float.isFinite(points[i]));
            assertTrue(points[i] >= -0.00001 && points[i] <= 4.00001);
        }
    }

    @Test void antialiasAndBorderContoursHaveMatchingVertexCountsAndWinding() {
        for (float inset : new float[]{-4, -0.25f, 0.25f, 0.55f, 1.05f}) {
            float[] points = RoundedGeometry.contour(0, 0, 120, 38, 8, inset, 12);
            assertEquals(104, points.length);
            double area = 0;
            for (int i = 0; i < points.length; i += 2) {
                int next = (i + 2) % points.length;
                area += points[i] * points[next + 1] - points[next] * points[i + 1];
            }
            assertTrue(area > 0, "Contour winding must stay consistent for fan/ring quads");
        }
    }
}
