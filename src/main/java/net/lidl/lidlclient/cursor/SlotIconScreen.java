package net.lidl.lidlclient.cursor;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Select real server-delivered stacks; no external item catalog or texture bundle. */
public final class SlotIconScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> accept;
    private final List<ItemStack> items=new ArrayList<>();
    private int left,top,pitch;
    public SlotIconScreen(Screen parent,InputConstants.Key key,Consumer<String> accept) {
        super(Component.literal("Choose an item icon"));
        this.parent=parent; this.accept=accept;
        var player=Minecraft.getInstance().player;
        if (player!=null) for (int i=0;i<36;i++) {
            var stack=player.getInventory().getItem(i);
            if (!stack.isEmpty()) items.add(stack.copy());
        }
        MenuCursor.lastIcons(key).entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> SlotIcons.decode(entry.getValue())).filter(stack -> !stack.isEmpty()).forEach(items::add);
    }
    @Override protected void init() {
        int rows=Math.max(1,(items.size()+8)/9);
        pitch=Math.min(20,Math.max(8,(height-100)/rows)); left=(width-9*pitch)/2; top=56;
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b -> onClose())
                .bounds(width/2-50,height-28,100,20).build());
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        if (event.button()==0) for (int i=0;i<items.size();i++) {
            int x=left+i%9*pitch,y=top+i/9*pitch;
            if (event.x()>=x && event.x()<x+pitch-2 && event.y()>=y && event.y()<y+pitch-2) {
                String icon=SlotIcons.encode(items.get(i));
                if (icon!=null) { accept.accept(icon); onClose(); }
                return true;
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xEB0B101B);
        g.centeredText(font,title,width/2,16,0xFFFFFFFF);
        g.centeredText(font,"Your inventory + the last menu opened with this key",width/2,34,0xFFADBBD0);
        if (items.isEmpty()) g.centeredText(font,"Open the server menu once to collect its icons.",width/2,70,0xFFFFFFFF);
        for (int i=0;i<items.size();i++) {
            int x=left+i%9*pitch,y=top+i/9*pitch,size=pitch-2;
            g.fill(x-1,y-1,x+size+1,y+size+1,0xFF576176);
            g.fill(x,y,x+size,y+size,0xFF18202E);
            g.pose().pushMatrix(); g.pose().translate(x,y); g.pose().scale(size/16f,size/16f);
            g.item(items.get(i),0,0); g.pose().popMatrix();
            if (mx>=x && mx<x+size && my>=y && my<y+size) g.setTooltipForNextFrame(font,items.get(i),mx,my);
        }
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
