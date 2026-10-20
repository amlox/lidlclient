package net.lidl.lidlclient.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Only this screen owns drag input. Gameplay panels never intercept clicks. */
public final class PanelEditorScreen extends Screen {
    private final Screen parent;
    private PanelHost.Entry dragging;
    private double offsetX, offsetY, startMouseX, startMouseY;
    private int resizeEdges;
    private PanelResize.Bounds startBounds;
    private static final Component HELP = Component.literal("Drag inside: move • Edges: resize • Middle-click: settings");
    public PanelEditorScreen(Screen parent) {
        super(Component.literal("Lidl GUI editor"));
        this.parent = parent;
    }
    @Override protected void init() {
        finishDrag();
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(4, height-24,56,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0x24000000);
        PanelHost.render(g,true);
        for (var entry:PanelHost.entries()) {
            int w=entry.width(width,height),h=entry.height(width,height);
            int x=entry.placement.x(width,w),y=entry.placement.y(height,h);
            // Corner handles are editor-only, and stay clear of panel contents.
            for (int i=0;i<4;i++) {
                int cx=(i&1)==0?x:x+w-5,cy=(i&2)==0?y:y+h-5;
                g.fill(cx,cy,cx+5,cy+5,0xFFE8EDF7);
            }
        }
        g.text(font,HELP,Math.max(2,(width-font.width(HELP))/2),8,0xFFF3F5FC,true);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta) {
        // Vanilla Screen adds a full-screen blur here. Keep the world visible and cheap.
        Minecraft.getInstance().gui.extractDeferredSubtitles();
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (super.mouseClicked(event,twice)) return true;
        if (event.button()!=0 && event.button()!=2) return false;
        var entries = PanelHost.entries();
        for (int i=entries.size()-1; i>=0; i--) {
            var entry=entries.get(i);
            int w=entry.width(width,height), h=entry.height(width,height);
            int x=entry.placement.x(width,w), y=entry.placement.y(height,h);
            if (event.x()>=x && event.x()<x+w && event.y()>=y && event.y()<y+h) {
                if (event.button()==2) {
                    Minecraft.getInstance().setScreen(new net.lidl.lidlclient.gui.ModuleScreen(entry.panel.id(),this));
                    return true;
                }
                resizeEdges=PanelResize.edges(event.x(),event.y(),x,y,w,h);
                if (resizeEdges==0 && entry.panel.click(event.x()-x,event.y()-y,w,h)) return true;
                dragging=entry; offsetX=event.x()-x; offsetY=event.y()-y;
                startMouseX=event.x(); startMouseY=event.y();
                startBounds=new PanelResize.Bounds(x,y,w,h);
                return true;
            }
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if (dragging==null || event.button()!=0) return super.mouseDragged(event,dx,dy);
        if (resizeEdges!=0) {
            dragging.resize(PanelResize.resize(startBounds,resizeEdges,event.x()-startMouseX,event.y()-startMouseY,
                    width,height,dragging.panel.minimumWidth(),dragging.panel.minimumHeight()),width,height);
            return true;
        }
        dragging.placement.move(event.x()-offsetX,event.y()-offsetY,width,height,
                dragging.width(width,height),dragging.height(width,height));
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button()==0 && dragging!=null) { finishDrag(); return true; }
        return super.mouseReleased(event);
    }
    private void finishDrag() { if (dragging!=null) { dragging.save(); dragging=null; } }
    @Override public void removed() { finishDrag(); super.removed(); }
    @Override public void onClose() { finishDrag(); Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
