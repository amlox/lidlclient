package net.lidl.lidlclient.cursor;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Select server-delivered stacks in their original menu/inventory positions, including gaps. */
public final class SlotIconScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> accept;
    private final Map<Integer,String> icons=new HashMap<>();
    private final Map<Integer,ItemStack> items=new HashMap<>();
    private final int rows;
    private final boolean menuAvailable;
    private SlotEditorLayout layout;
    public SlotIconScreen(Screen parent,InputConstants.Key key,int fallbackRows,Consumer<String> accept) {
        super(Component.literal("Choose an item icon"));
        this.parent=parent; this.accept=accept;
        var menu=MenuCursor.lastMenu(key);
        menuAvailable=menu!=null;
        rows=menuAvailable?menu.rows():fallbackRows;
        if (menuAvailable) icons.putAll(menu.icons());
        MenuCursor.inventoryIcons().forEach((slot,icon) -> icons.put(54+slot,icon));
        icons.forEach((slot,icon) -> items.put(slot,SlotIcons.decode(icon)));
    }
    @Override protected void init() {
        layout=SlotEditorLayout.forScreen(width,height,rows);
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b -> onClose())
                .bounds(width/2-50,height-28,100,20).build());
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        int slot=layout.iconSlotAt(event.x(),event.y());
        if (event.button()==0 && slot>=0 && !items.getOrDefault(slot,ItemStack.EMPTY).isEmpty()) {
            accept.accept(icons.get(slot)); onClose();
            return true;
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xEB0B101B);
        g.centeredText(font,title,width/2,16,0xFFFFFFFF);
        g.centeredText(font,"Menu above, inventory below — click an item to copy its icon",width/2,34,0xFFADBBD0);
        if (!menuAvailable) g.centeredText(font,"Open this key's server menu once to collect its icons.",width/2,74,0xFFADBBD0);
        g.text(font,"Container",layout.left(),layout.top()-10,0xFFADBBD0,false);
        g.text(font,"Inventory",layout.left(),layout.top()+rows*layout.pitch()+4,0xFFADBBD0,false);
        for (int i=0;i<rows*9+36;i++) {
            boolean inventory=i>=rows*9;
            int slot=inventory?i-rows*9:i;
            int x=layout.x(slot),y=layout.y(inventory,slot),size=layout.size();
            var stack=items.getOrDefault(inventory?54+slot:slot,ItemStack.EMPTY);
            g.fill(x-1,y-1,x+size+1,y+size+1,0xFF576176);
            g.fill(x,y,x+size,y+size,0xFF18202E);
            if (!stack.isEmpty()) {
                g.pose().pushMatrix(); g.pose().translate(x,y); g.pose().scale(size/16f,size/16f);
                g.item(stack,0,0); g.itemDecorations(font,stack,0,0); g.pose().popMatrix();
                if (mx>=x && mx<x+size && my>=y && my<y+size) g.setTooltipForNextFrame(font,stack,mx,my);
            }
        }
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
