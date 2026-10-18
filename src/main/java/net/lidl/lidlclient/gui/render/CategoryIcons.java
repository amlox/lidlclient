package net.lidl.lidlclient.gui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/** Small vector icons: independent of font coverage and GUI scale. */
public final class CategoryIcons {
    private CategoryIcons() { }

    public static void draw(GuiGraphicsExtractor g, String category, float x, float y,
                            int color, ScreenRectangle clip) {
        switch (category) {
            case "General settings" -> {
                circle(g, x, y, 6, color, clip);
                circle(g, x, y, 2, color, clip);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    line(g, x + (float) Math.cos(a) * 6, y + (float) Math.sin(a) * 6,
                            x + (float) Math.cos(a) * 9, y + (float) Math.sin(a) * 9, color, clip);
                }
            }
            case "Gambling" -> {
                circle(g, x + 3, y - 3, 5, color, clip);
                circle(g, x - 3, y + 3, 5, color, clip);
                line(g, x - 3, y + 1, x - 3, y + 5, color, clip);
                line(g, x + 3, y - 5, x + 3, y - 1, color, clip);
            }
            case "QoL features" -> {
                // Open wrench jaw and a rounded handle.
                line(g, x - 6, y + 7, x + 3, y - 2, color, clip);
                line(g, x - 3, y + 8, x + 5, y, color, clip);
                line(g, x - 6, y + 7, x - 3, y + 8, color, clip);
                line(g, x + 3, y - 2, x, y - 5, color, clip);
                line(g, x, y - 5, x + 2, y - 8, color, clip);
                line(g, x + 2, y - 8, x + 4, y - 4, color, clip);
                line(g, x + 4, y - 4, x + 8, y - 3, color, clip);
                line(g, x + 8, y - 3, x + 5, y, color, clip);
            }
            case "Fun features" -> {
                sparkle(g, x - 2, y + 1, 7, color, clip);
                sparkle(g, x + 6, y - 6, 3, color, clip);
            }
            case "Cheats" -> {
                float[][] points = {{3,-9},{-6,2},{0,2},{-3,9},{6,-2},{0,-2},{3,-9}};
                for (int i = 1; i < points.length; i++)
                    line(g, x + points[i-1][0], y + points[i-1][1],
                            x + points[i][0], y + points[i][1], color, clip);
            }
            default -> { // Custom registries retain a neutral icon.
                for (int i = -1; i <= 1; i++)
                    line(g, x - 6, y + i * 5, x + 6, y + i * 5, color, clip);
            }
        }
    }

    private static void circle(GuiGraphicsExtractor g, float x, float y, float r,
                               int color, ScreenRectangle clip) {
        SmoothRenderer.panel(g, x-r, y-r, r*2, r*2, r, 0, color, 1.3f, 0, clip);
    }

    private static void sparkle(GuiGraphicsExtractor g, float x, float y, float r,
                                int color, ScreenRectangle clip) {
        float[] points = ConstellationRenderer.points(x, y, r);
        for (int i = 0; i < points.length; i += 2) {
            int next = (i + 2) % points.length;
            line(g, points[i], points[i+1], points[next], points[next+1], color, clip);
        }
    }

    private static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1,
                             int color, ScreenRectangle clip) {
        g.pose().pushMatrix();
        g.pose().translate(x0, y0);
        g.pose().rotate((float) Math.atan2(y1-y0, x1-x0));
        SmoothRenderer.panel(g, 0, -.65f, (float) Math.hypot(x1-x0, y1-y0), 1.3f,
                .65f, color, 0, 0, 0, clip);
        g.pose().popMatrix();
    }
}
