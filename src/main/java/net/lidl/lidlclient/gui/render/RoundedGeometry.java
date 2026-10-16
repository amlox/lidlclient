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

}
