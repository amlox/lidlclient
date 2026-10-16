package net.lidl.lidlclient.gui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;


/**
 * Vector-style rounded geometry with a one-framebuffer-pixel coverage fringe.
 * Unlike integer GUI fills, curves and alpha gradients retain subpixel coordinates
 * through the GPU pipeline, even at large Minecraft GUI scales. No external native
 * renderer, texture per button, or custom shader is needed.
 */
public final class SmoothRenderer {
    private SmoothRenderer() { }

    public static void panel(GuiGraphicsExtractor graphics, float x, float y, float width,
                             float height, float radius, int fill, int border, float stroke,
                             float glow, ScreenRectangle clip) {
        if (width <= 0 || height <= 0) return;
        float pixel = (float) (1.0 / Minecraft.getInstance().getWindow().getGuiScale());
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        int left = (int) Math.floor(x - glow - pixel);
        int top = (int) Math.floor(y - glow - pixel);
        int right = (int) Math.ceil(x + width + glow + pixel);
        int bottom = (int) Math.ceil(y + height + glow + pixel);
        ScreenRectangle bounds = new ScreenRectangle(left, top, right - left, bottom - top)
                .transformMaxBounds(pose);
        if (clip != null) bounds = clip.intersection(bounds);
        if (bounds == null) return;
        graphics.guiRenderState.addGuiElement(new MeshState(pose, bounds, clip, x, y, width, height, radius, fill, border, stroke, glow, pixel));
    }

    public static void line(GuiGraphicsExtractor graphics, float x, float y, float height,
                            float thickness, int color, float glow, ScreenRectangle clip) {
        panel(graphics, x, y, thickness, height, thickness / 2,
                color, color, thickness / 2, glow, clip);
    }

    private static float glowAlpha(float t) {
        return 0.16f * (float) Math.exp(-5 * t * t) * (1 - t);
    }

    private static int alpha(int color, float multiplier) {
        return (Math.clamp(Math.round((color >>> 24) * multiplier), 0, 255) << 24) | (color & 0xFFFFFF);
    }

    private static float[] contour(float x, float y, float width, float height,
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

    private static final class MeshBuilder {
        private final VertexConsumer consumer;
        private final Matrix3x2f pose;

        MeshBuilder(VertexConsumer consumer, Matrix3x2f pose) {
            this.consumer = consumer;
            this.pose = pose;
        }

        private void vertex(float x, float y, int color) {
            consumer.addVertexWith2DPose(pose, x, y).setColor(color);
        }

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
                vertex(points[2 * i], points[2 * i + 1], color);
                vertex(points[2 * next], points[2 * next + 1], color);
                vertex(points[2 * next], points[2 * next + 1], color);
            }
        }

        void ring(float[] outer, float[] inner, int outerColor, int innerColor) {
            int count = outer.length / 2;
            for (int i = 0; i < count; i++) {
                int next = (i + 1) % count;
                vertex(outer[2 * i], outer[2 * i + 1], outerColor);
                vertex(outer[2 * next], outer[2 * next + 1], outerColor);
                vertex(inner[2 * next], inner[2 * next + 1], innerColor);
                vertex(inner[2 * i], inner[2 * i + 1], innerColor);
            }
        }
    }

    private record MeshState(Matrix3x2f pose, ScreenRectangle bounds, ScreenRectangle scissorArea,
                             float x, float y, float width, float height, float radius,
                             int fill, int border, float stroke, float glow, float pixel)
            implements GuiElementRenderState {
        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public void buildVertices(VertexConsumer consumer) {
            var mesh = new MeshBuilder(consumer, pose);
            int segments = Math.clamp((int) Math.ceil(Math.sqrt(radius / pixel) * 3), 6, 24);
    
            if (glow > 0) {
                // Non-overlapping gradient rings, rather than stacked opaque rectangles.
                for (int ring = 8; ring > 0; ring--) {
                    float far = glow * ring / 8;
                    float near = glow * (ring - 1) / 8;
                    mesh.ring(contour(x, y, width, height, radius, -far, segments),
                            contour(x, y, width, height, radius, -near, segments),
                            alpha(border, glowAlpha(far / glow)),
                            alpha(border, glowAlpha(near / glow)));
                }
            }
    
            float[] outside = contour(x, y, width, height, radius, -pixel / 2, segments);
            float[] inside = contour(x, y, width, height, radius, pixel / 2, segments);
            mesh.fan(inside, fill);
            mesh.ring(outside, inside, alpha(fill, 0), fill);
    
            if ((border >>> 24) != 0 && stroke > 0) {
                float edge = Math.min(pixel / 2, stroke / 2);
                float[] outerSolid = contour(x, y, width, height, radius, edge, segments);
                float[] innerSolid = contour(x, y, width, height, radius, stroke - edge, segments);
                float[] innerFade = contour(x, y, width, height, radius, stroke + edge, segments);
                mesh.ring(outside, outerSolid, alpha(border, 0), border);
                mesh.ring(outerSolid, innerSolid, border, border);
                mesh.ring(innerSolid, innerFade, border, alpha(border, 0));
            }
    
        }
    }
}
