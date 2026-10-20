package net.lidl.lidlclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.function.IntConsumer;

/** Drag saturation/brightness and hue; optional hex input stays in sync with the preview. */
public final class GuiColorPickerScreen extends Screen {
    private final Screen parent;
    private final IntConsumer accept;
    private final int original;
    private final int defaultColor;
    private double hue,saturation,value;
    private EditBox hex;
    private Button save;
    private int left,top,size,hueY,sideX,drag;
    private boolean updating;
    public GuiColorPickerScreen(Screen parent,String label,int color,int defaultColor,IntConsumer accept) {
        super(Component.literal(label));this.parent=parent;this.accept=accept;original=color;this.defaultColor=defaultColor;setColor(color);
    }
    private void setColor(int color) {var hsv=GuiColor.hsv(color);if(hsv[1]>0) hue=hsv[0];saturation=hsv[1];value=hsv[2];}
    private int color() {return GuiColor.rgb(hue,saturation,value);}
    @Override protected void init() {
        String draft=hex==null?GuiColor.hex(color()):hex.getValue();
        size=Math.max(40,Math.min(190,Math.min(height-120,width-170)));left=(width-size-135)/2;top=44;hueY=top+size+8;sideX=left+size+14;
        hex=addRenderableWidget(new EditBox(font,sideX,top+60,110,20,Component.literal("Hex colour")));
        hex.setMaxLength(9);hex.setValue(draft);hex.setResponder(text->{
            Integer rgb=GuiColor.parse(text);if(!updating&&rgb!=null) setColor(rgb);if(save!=null) save.active=rgb!=null;
        });
        int y=hueY+26,w=(size+135-12)/3;
        save=addRenderableWidget(Button.builder(Component.literal("Use colour"),b->save()).bounds(left,y,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Default"),b->{setColor(defaultColor);updateHex();}).bounds(left+w+6,y,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(left+2*(w+6),y,w,20).build());
        save.active=GuiColor.parse(draft)!=null;drag=0;
    }
    private void updateHex() {updating=true;hex.setValue(GuiColor.hex(color()));updating=false;save.active=true;}
    private boolean inside(double x,double y,int bx,int by,int w,int h) {return x>=bx&&x<bx+w&&y>=by&&y<by+h;}
    private void pick(double x,double y) {
        if(drag==1) {saturation=Math.clamp((x-left)/(size-1),0,1);value=1-Math.clamp((y-top)/(size-1),0,1);}
        else if(drag==2) hue=Math.clamp((x-left)/(size-1),0,1);updateHex();
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick) {
        if(e.button()==0) {
            if(inside(e.x(),e.y(),left,top,size,size)) drag=1;
            else if(inside(e.x(),e.y(),left,hueY,size,14)) drag=2;
            if(drag!=0) {pick(e.x(),e.y());return true;}
        }
        return super.mouseClicked(e,doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy) {if(e.button()==0&&drag!=0) {pick(e.x(),e.y());return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e) {boolean was=drag!=0;drag=0;return was||super.mouseReleased(e);}
    private void save() {Integer rgb=GuiColor.parse(hex.getValue());if(rgb!=null) {accept.accept(rgb);onClose();}}
    @Override public boolean keyPressed(KeyEvent e) {if(e.key()==GLFW.GLFW_KEY_ENTER||e.key()==GLFW.GLFW_KEY_KP_ENTER) {save();return true;}return super.keyPressed(e);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xEB0B101B);g.text(font,title,(width-font.width(title))/2,18,0xFFFFFFFF,true);
        net.lidl.lidlclient.gui.render.ColorPickerRenderer.draw(g,left,top,size,hueY,hue);
        int sx=left+(int)Math.round(saturation*(size-1)),sy=top+(int)Math.round((1-value)*(size-1));
        g.fill(sx-3,sy-1,sx+4,sy+2,0xFF000000);g.fill(sx-1,sy-3,sx+2,sy+4,0xFF000000);
        g.fill(sx-2,sy,sx+3,sy+1,0xFFFFFFFF);g.fill(sx,sy-2,sx+1,sy+3,0xFFFFFFFF);
        int hx=left+(int)Math.round(hue*(size-1));g.fill(hx-1,hueY-2,hx+2,hueY+16,0xFFFFFFFF);
        g.text(font,Component.literal("Before     After"),sideX,top,0xFFADBBD0,false);
        g.fill(sideX,top+13,sideX+50,top+47,original);g.fill(sideX+58,top+13,sideX+108,top+47,color());
        g.text(font,Component.literal("Hex (optional)"),sideX,top+49,0xFFFFFFFF,false);
        g.text(font,Component.literal(save.active?"Drag to choose":"Invalid hex code"),sideX,top+88,save.active?0xFFADBBD0:0xFFFF6666,false);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() {Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen() {return false;}
}
