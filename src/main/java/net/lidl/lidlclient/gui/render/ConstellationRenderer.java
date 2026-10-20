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

/** Layered original starfield arrangements inspired by the supplied references. */
public final class ConstellationRenderer {
    private static final float TILE = 240;
    private static final float[][] POINTS = {
            {.12f, 36}, {.31f, 57}, {.44f, 30}, {.63f, 70}, {.82f, 49},
            {.23f, 147}, {.43f, 123}, {.69f, 153}, {.59f, 196}, {.84f, 211},
            {.08f, 93}, {.22f, 111}, {.37f, 92}
    };
    private static final int[][] EDGES = {{0,1},{1,2},{2,3},{3,4},{5,6},{6,7},{7,8},{8,5},{8,9},{10,11},{11,12}};
    private static final float[][] SPARKLES = {
            {.13f, 81, 6}, {.84f, 118, 5}, {.16f, 222, 6}, {.76f, 230, 4}, {.52f, 101, 3.5f}
    };
    private static final float[][] DUST = createDust();

    // Fixed seed: stars never flicker or change arrangement between frames.
    private static float[][] createDust() {
        var random = new java.util.Random(0x51A7C0DEL);
        float[][] dust = new float[48][4];
        for (float[] point : dust) {
            point[0] = .03f + random.nextFloat() * .94f;
            point[1] = random.nextFloat() * TILE;
            point[2] = .45f + random.nextFloat() * .65f;
            point[3] = .28f + random.nextFloat() * .45f;
        }
        return dust;
    }
    private ConstellationRenderer() { }

    public static void background(GuiGraphicsExtractor graphics, float x, float width,
                                   int height, double offset, double seconds, ScreenRectangle clip) {
        if (clip.width() <= 0 || clip.height() <= 0) return;
        float pixel = (float) (1.0 / Minecraft.getInstance().getWindow().getGuiScale());
        graphics.guiRenderState.addGuiElement(new BackgroundState(new Matrix3x2f(graphics.pose()),
                clip, clip, x, width, height, offset, seconds, pixel));
    }

    private record BackgroundState(Matrix3x2f pose, ScreenRectangle bounds, ScreenRectangle scissorArea,
                                   float x, float width, int height, double offset, double seconds, float pixel)
            implements GuiElementRenderState {
        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public void buildVertices(VertexConsumer consumer) {
            var mesh = new RoundedGeometry.MeshBuilder((vx, vy, rgba) ->
                    consumer.addVertexWith2DPose(pose, vx, vy).setColor(rgba));
            int first = (int) Math.floor((offset - 18) / TILE);
            int last = (int) Math.floor((offset + height + 18) / TILE);
            for (int tile = first; tile <= last; tile++) {
                float originY = tile * TILE - (float) offset;
                boolean mirror = Math.floorMod(tile, 2) == 1;
                for (int[] edge : EDGES) {
                    float[] a = POINTS[edge[0]], b = POINTS[edge[1]];
                    float y0 = originY + a[1], y1 = originY + b[1];
                    if (Math.max(y0, y1) < 0 || Math.min(y0, y1) > height) continue;
                    float x0 = x + px(a[0], width, mirror), x1 = x + px(b[0], width, mirror);
                    float length = (float) Math.hypot(x1-x0,y1-y0);
                    float dx = (y1-y0) / length * .4f, dy = (x0-x1) / length * .4f;
                    mesh.quad(new float[]{x0-dx,y0-dy,x0+dx,y0+dy,x1+dx,y1+dy,x1-dx,y1-dy}, 0x60FFFFFF);
                }
                for (int i = 0; i < POINTS.length; i++) {
                    float sx = x + px(POINTS[i][0],width,mirror), sy = originY + POINTS[i][1];
                    if (sy < -16 || sy > height+16) continue;
                    float shine = twinkle(seconds,tile,i);
                    if (i == 1 || i == 4 || i == 8) drawStar(mesh,sx,sy,i == 8 ? 7 : 4.5f,shine,pixel);
                    else dot(mesh,sx,sy,1.2f,shine);
                }
                for (int i = 0; i < SPARKLES.length; i++) {
                    float[] point = SPARKLES[i];
                    float sy = originY + point[1];
                    if (sy < -18 || sy > height+18) continue;
                    drawStar(mesh,x+px(point[0],width,mirror),sy,point[2],twinkle(seconds,tile,13+i),pixel);
                }
                for (int i = 0; i < DUST.length; i++) {
                    float[] point = DUST[i];
                    float sy = originY + point[1];
                    if (sy < 0 || sy > height) continue;
                    dot(mesh,x+px(point[0],width,mirror),sy,point[2],point[3]*twinkle(seconds,tile,18+i));
                }
            }
        }
    }

    private static void dot(RoundedGeometry.MeshBuilder mesh, float x, float y, float size, float opacity) {
        mesh.quad(new float[]{x-size,y,x,y-size,x+size,y,x,y+size},color(0xFFFFFF,opacity));
    }

    static float twinkle(double seconds, int tile, int star) {
        // Stable identity per tile/star; no per-frame randomness or synchronized pulsing.
        long seed = (tile * 73856093L) ^ (star * 19349663L);
        double phase = Math.floorMod(seed, 6283) / 1000.0;
        double speed = .65 + Math.floorMod(seed * 31, 1100) / 1000.0;
        return (float) (.20 + .80 * Math.pow(.5 + .5 * Math.sin(seconds * speed + phase), 2));
    }

    private static float px(float fraction, float width, boolean mirror) {
        return 12 + (mirror ? 1 - fraction : fraction) * Math.max(1, width - 24);
    }

    public static void star(GuiGraphicsExtractor graphics, float x, float y, float radius,
                            float opacity, ScreenRectangle clip) {
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        int pad = (int) Math.ceil(radius * 2.3f);
        ScreenRectangle bounds = new ScreenRectangle((int) Math.floor(x) - pad,
                (int) Math.floor(y) - pad, pad * 2 + 2, pad * 2 + 2).transformMaxBounds(pose);
        if (clip != null) bounds = bounds.intersection(clip);
        if (bounds == null) return;
        float pixel = (float) (1.0 / Minecraft.getInstance().getWindow().getGuiScale());
        graphics.guiRenderState.addGuiElement(new StarState(pose, bounds, clip,
                x, y, radius, opacity, pixel));
    }

    static float[] points(float x, float y, float radius) {
        return new float[]{x, y-radius, x+radius*.22f, y-radius*.22f,
                x+radius, y, x+radius*.22f, y+radius*.22f,
                x, y+radius, x-radius*.22f, y+radius*.22f,
                x-radius, y, x-radius*.22f, y-radius*.22f};
    }

    private static int color(int rgb, float alpha) {
        return (Math.clamp(Math.round(alpha * 255), 0, 255) << 24) | rgb;
    }

    private record StarState(Matrix3x2f pose, ScreenRectangle bounds, ScreenRectangle scissorArea,
                             float x, float y, float radius, float opacity, float pixel)
            implements GuiElementRenderState {
        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public void buildVertices(VertexConsumer consumer) {
            var mesh = new RoundedGeometry.MeshBuilder((vx, vy, rgba) ->
                    consumer.addVertexWith2DPose(pose, vx, vy).setColor(rgba));
            drawStar(mesh,x,y,radius,opacity,pixel);
        }
    }

    private static void drawStar(RoundedGeometry.MeshBuilder mesh, float x, float y,
                                 float radius, float opacity, float pixel) {
        // Three broad gradient rings: more visible sparkle, fewer vertices.
        for (int i = 3; i > 0; i--) {
            float outer = 1 + i * .45f, inner = 1 + (i-1) * .45f;
            mesh.ring(points(x,y,radius*outer),points(x,y,radius*inner),
                    color(0xFFFFFF,opacity * .8f * (1-i/3f)*(1-i/3f)),
                    color(0xFFFFFF,opacity * .8f * (1-(i-1)/3f)*(1-(i-1)/3f)));
        }
        float aa = Math.min(pixel*.6f,radius*.12f);
        mesh.fan(points(x,y,radius-aa),color(0x03040A,opacity));
        mesh.ring(points(x,y,radius+aa),points(x,y,radius-aa),color(0x03040A,0),color(0x03040A,opacity));
        float core = radius * .82f;
        mesh.fan(points(x,y,core-aa),color(0xFFFFFF,opacity));
        mesh.ring(points(x,y,core+aa),points(x,y,core-aa),color(0xFFFFFF,0),color(0xFFFFFF,opacity));
    }
}
