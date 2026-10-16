package net.lidl.lidlclient.gui.render;

/** Pure geometry shared by the renderer and its tests. */
final class RoundedGeometry {
    private RoundedGeometry() { }

    static float[] contour(float x, float y, float width, float height,
                                   float radius, float inset, int segments) {
        float w = Math.max(0.001f, width - 2 * inset);
        float h = Math.max(0.001f, height - 2 * inset);
        float r = Math.clamp(radius - inset, 0, Math.min(w, h) / 2);
        float left = x + inset, top = y + inset;
        float[] points = new float[4 * (segments + 1) * 2];
        for (int corner = 0; corner < 4; corner++) {
            float cx = (corner == 0 || corner == 3) ? left + r : left + w - r;
            float cy = corner < 2 ? top + r : top + h - r;
            for (int step = 0; step <= segments; step++) {
                double angle = Math.toRadians(180 + 90 * corner + 90.0 * step / segments);
                int i = (corner * (segments + 1) + step) * 2;
                points[i] = cx + (float) Math.cos(angle) * r;
                points[i + 1] = cy + (float) Math.sin(angle) * r;
            }
        }
        return points;
    }

    @FunctionalInterface
    interface VertexSink { void vertex(float x, float y, int color); }

    static final class MeshBuilder {
        private final VertexSink sink;

        MeshBuilder(VertexSink sink) { this.sink = sink; }

        private void vertex(float x, float y, int color) { sink.vertex(x, y, color); }

        void fan(float[] points, int color) {
            float cx = 0, cy = 0;
            int count = points.length / 2;
            for (int i = 0; i < points.length; i += 2) { cx += points[i]; cy += points[i + 1]; }
            cx /= count;
            cy /= count;
            for (int i = 0; i < count; i++) {
                int next = (i + 1) % count;
                // A degenerate quad is a triangle in the standard batched GUI pipeline.
                vertex(cx, cy, color);
                vertex(points[2 * next], points[2 * next + 1], color);
                vertex(points[2 * i], points[2 * i + 1], color);
                vertex(points[2 * i], points[2 * i + 1], color);
            }
        }

        void ring(float[] outer, float[] inner, int outerColor, int innerColor) {
            int count = outer.length / 2;
            for (int i = 0; i < count; i++) {
                int next = (i + 1) % count;
                vertex(outer[2 * i], outer[2 * i + 1], outerColor);
                vertex(inner[2 * i], inner[2 * i + 1], innerColor);
                vertex(inner[2 * next], inner[2 * next + 1], innerColor);
                vertex(outer[2 * next], outer[2 * next + 1], outerColor);
            }
        }
    }

}
