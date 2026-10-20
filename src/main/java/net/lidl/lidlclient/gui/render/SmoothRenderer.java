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
        panel(graphics, x, y, width, height, radius, fill, border, stroke, glow, .24f, clip);
    }

    /** Glow radius and brightness can be tuned independently for hover and enabled states. */
    public static void panel(GuiGraphicsExtractor graphics, float x, float y, float width,
                             float height, float radius, int fill, int border, float stroke,
                             float glow, float glowStrength, ScreenRectangle clip) {
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
        graphics.guiRenderState.addGuiElement(new MeshState(pose.translate(x, y), bounds, clip, width, height, radius, fill, border, stroke, glow, pixel, glowStrength));
    }

    public static void line(GuiGraphicsExtractor graphics, float x, float y, float height,
                            float thickness, int color, float glow, ScreenRectangle clip) {
        panel(graphics, x, y, thickness, height, thickness / 2,
                color, color, 0, glow, clip);
    }

    private static float glowAlpha(float t, float strength) {
        return strength * (float) Math.exp(-5 * t * t) * (1 - t);
    }

    private static int alpha(int color, float multiplier) {
        return (Math.clamp(Math.round((color >>> 24) * multiplier), 0, 255) << 24) | (color & 0xFFFFFF);
    }

    private record MeshState(Matrix3x2f pose, ScreenRectangle bounds, ScreenRectangle scissorArea,
                             float width, float height, float radius,
                             int fill, int border, float stroke, float glow, float pixel, float glowStrength)
            implements GuiElementRenderState {
        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public void buildVertices(VertexConsumer consumer) {
            var mesh = new RoundedGeometry.MeshBuilder((vx, vy, color) ->
                    consumer.addVertexWith2DPose(pose, vx, vy).setColor(color));
            int segments = Math.clamp((int) Math.ceil(Math.sqrt(radius / pixel) * 2), 4, 16);

            if (glow > 0) {
                // Non-overlapping gradient rings, rather than stacked opaque rectangles.
                for (int ring = 4; ring > 0; ring--) {
                    float far = glow * ring / 4;
                    float near = glow * (ring - 1) / 4;
                    mesh.ring(RoundedGeometry.cachedContour(width, height, radius, -far, segments),
                            RoundedGeometry.cachedContour(width, height, radius, -near, segments),
                            alpha(border, glowAlpha(far / glow, glowStrength)),
                            alpha(border, glowAlpha(near / glow, glowStrength)));
                }
            }

            float[] outside = RoundedGeometry.cachedContour(width, height, radius, -pixel / 2, segments);
            float[] inside = RoundedGeometry.cachedContour(width, height, radius, pixel / 2, segments);
            if ((fill >>> 24) != 0) {
                mesh.fan(inside, fill);
                mesh.ring(outside, inside, alpha(fill, 0), fill);
            }

            if ((border >>> 24) != 0 && stroke > 0) {
                float edge = Math.min(pixel / 2, stroke / 2);
                float[] outerSolid = RoundedGeometry.cachedContour(width, height, radius, edge, segments);
                float[] innerSolid = RoundedGeometry.cachedContour(width, height, radius, stroke - edge, segments);
                float[] innerFade = RoundedGeometry.cachedContour(width, height, radius, stroke + edge, segments);
                mesh.ring(outside, outerSolid, alpha(border, 0), border);
                mesh.ring(outerSolid, innerSolid, border, border);
                mesh.ring(innerSolid, innerFade, border, alpha(border, 0));
            }

        }
    }
}
