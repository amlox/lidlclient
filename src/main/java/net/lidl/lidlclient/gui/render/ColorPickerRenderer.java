package net.lidl.lidlclient.gui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.lidl.lidlclient.gui.GuiColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

/** One render state and eight quads, independent of picker resolution. */
public final class ColorPickerRenderer {
    private ColorPickerRenderer() { }
    public static void draw(GuiGraphicsExtractor g, int x, int y, int size, int hueY, double hue) {
        Matrix3x2f pose = new Matrix3x2f(g.pose());
        ScreenRectangle bounds = new ScreenRectangle(x,y,size,hueY+14-y).transformMaxBounds(pose);
        g.guiRenderState.addGuiElement(new Gradient(pose,bounds,x,y,size,hueY,hue));
    }
    private record Gradient(Matrix3x2f pose, ScreenRectangle bounds, int x, int y, int size,
                            int hueY, double hue) implements GuiElementRenderState {
        @Override public ScreenRectangle scissorArea() { return null; }
        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        private void quad(VertexConsumer c,float left,float top,float right,float bottom,
                          int tl,int tr,int br,int bl) {
            c.addVertexWith2DPose(pose,left,top).setColor(tl);
            c.addVertexWith2DPose(pose,left,bottom).setColor(bl);
            c.addVertexWith2DPose(pose,right,bottom).setColor(br);
            c.addVertexWith2DPose(pose,right,top).setColor(tr);
        }
        @Override public void buildVertices(VertexConsumer c) {
            int hueColor = GuiColor.rgb(hue,1,1);
            quad(c,x,y,x+size,y+size,0xFFFFFFFF,hueColor,hueColor,0xFFFFFFFF);
            quad(c,x,y,x+size,y+size,0,0,0xFF000000,0xFF000000);
            for (int i=0;i<6;i++) {
                int a=GuiColor.rgb(i/6.0,1,1), b=GuiColor.rgb((i+1)/6.0,1,1);
                quad(c,x+size*i/6f,hueY,x+size*(i+1)/6f,hueY+14,a,b,b,a);
            }
        }
    }
}
