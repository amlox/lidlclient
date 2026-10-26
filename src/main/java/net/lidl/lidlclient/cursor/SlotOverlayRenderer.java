package net.lidl.lidlclient.cursor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;

/** Renders the vanilla ContainerScreen texture and geometry without taking gameplay focus. */
public final class SlotOverlayRenderer {
    private static final Identifier TEXTURE=Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private Map<Integer,String> source;
    private Object level;
    private final Map<Integer,ItemStack> icons=new HashMap<>();
    public void clear() { source=null; level=null; icons.clear(); }
    public void draw(GuiGraphicsExtractor g,SlotOverlay overlay,boolean gui,boolean highlights,boolean showIcons) {
        var client=Minecraft.getInstance();
        int rows=overlay.rows(),left=(g.guiWidth()-176)/2,top=(g.guiHeight()-(114+rows*18))/2;
        if (gui) {
            // Same two texture slices as vanilla ContainerScreen, for all six chest sizes.
            g.blit(RenderPipelines.GUI_TEXTURED,TEXTURE,left,top,0,0,176,rows*18+17,256,256);
            g.blit(RenderPipelines.GUI_TEXTURED,TEXTURE,left,top+rows*18+17,0,126,176,96,256,256);
            g.text(client.font,"Loading…",left+8,top+6,0xFF404040,false);
            g.text(client.font,client.player.getInventory().getDisplayName(),left+8,top+rows*18+20,0xFF404040,false);
        }
        if (showIcons && (source!=overlay.icons() || level!=client.level)) {
            source=overlay.icons(); level=client.level; icons.clear();
            source.forEach((slot,value) -> icons.put(slot,SlotIcons.decode(value)));
        }
        for (int slot=0;slot<rows*9;slot++) {
            int x=SlotOverlayLayout.x(g.guiWidth(),slot),y=SlotOverlayLayout.y(g.guiHeight(),rows,slot);
            if (highlights) highlight(g,overlay.slots().get(slot),x,y);
            if (showIcons) item(g,icons.getOrDefault(slot,ItemStack.EMPTY),x,y);
        }
        for (int slot=0;slot<36;slot++) {
            int x=SlotOverlayLayout.x(g.guiWidth(),slot),y=SlotOverlayLayout.inventoryY(g.guiHeight(),rows,slot);
            ItemStack stack=showIcons ? icons.get(slot+54) : null;
            if (stack==null && gui) stack=client.player.getInventory().getItem(slot<27 ? slot+9 : slot-27);
            if (highlights) highlight(g,overlay.inventorySlots().get(slot),x,y);
            if (stack!=null) item(g,stack,x,y);
        }
    }
    private static void item(GuiGraphicsExtractor g,ItemStack stack,int x,int y) {
        if (!stack.isEmpty()) { g.item(stack,x,y); g.itemDecorations(Minecraft.getInstance().font,stack,x,y); }
    }
    private static void highlight(GuiGraphicsExtractor g,Integer colour,int x,int y) {
        if (colour!=null) g.fill(x,y,x+16,y+16,SlotOverlay.translucent(colour,MenuCursor.overlayOpacity()));
    }
}
