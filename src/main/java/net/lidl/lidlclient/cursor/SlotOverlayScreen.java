package net.lidl.lidlclient.cursor;

import com.mojang.blaze3d.platform.InputConstants;
import net.lidl.lidlclient.gui.GuiColor;
import net.lidl.lidlclient.gui.GuiColorPickerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.Map;

/** Combined chest and player-inventory editor; both areas share one saved draft. */
public final class SlotOverlayScreen extends Screen {
    private final Screen parent;
    private final InputConstants.Key key;
    private SlotOverlay draft;
    private int brush = SlotOverlay.DEFAULT_COLOUR;
    private int left,top,pitch;
    private boolean iconMode;
    private final java.util.Map<String,net.minecraft.world.item.ItemStack> iconCache=new java.util.HashMap<>();
    public SlotOverlayScreen(Screen parent,InputConstants.Key key) {
        super(Component.literal("Slot overlay: ").append(key.getDisplayName()));
        this.parent = parent;
        this.key = key;
        draft = MenuCursor.overlay(key);
    }
    @Override protected void init() {
        pitch = Math.min(18,Math.max(4,(height-154)/(draft.rows()+4)));
        left = (width-9*pitch)/2;
        top = 104;
        int controls = width/2-115;
        addRenderableWidget(Button.builder(Component.literal(draft.rows()*9+" slots ("+draft.rows()+" rows)"),b -> {
            draft = draft.withRows(draft.rows()%6+1); rebuildWidgets();
        }).bounds(controls,34,120,20).build());
        addRenderableWidget(Button.builder(Component.literal("Brush "+GuiColor.hex(brush)),b ->
                Minecraft.getInstance().setScreen(new GuiColorPickerScreen(this,"New slot colour",0xFF000000|brush,
                        SlotOverlay.DEFAULT_COLOUR,colour -> brush=colour & 0xFFFFFF)))
                .bounds(controls+126,34,104,20).build());
        addRenderableWidget(Button.builder(Component.literal(iconMode?"Mode: Icons":"Mode: Colours"),b -> {
            iconMode=!iconMode; rebuildWidgets();
        }).bounds(width/2-52,58,104,20).build());
        int bottom = height-28;
        addRenderableWidget(Button.builder(Component.literal("Save"),b -> {
            MenuCursor.setOverlay(key,draft); onClose();
        }).bounds(width/2-120,bottom,76,20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"),b -> {
            draft = new SlotOverlay(draft.rows(),Map.of(),Map.of(),Map.of());
        }).bounds(width/2-38,bottom,76,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b -> onClose())
                .bounds(width/2+44,bottom,76,20).build());
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        if (event.button() == 0 || event.button() == 1) {
            for (int index=0;index<draft.rows()*9+36;index++) {
                boolean inventory=index>=draft.rows()*9;
                int slot=inventory?index-draft.rows()*9:index;
                int x=left+(slot%9)*pitch,y=slotY(inventory,slot);
                if (event.x()<x || event.x()>=x+pitch-2 || event.y()<y || event.y()>=y+pitch-2) continue;
                if (iconMode) {
                    final int chosen=inventory?slot+54:slot;
                    if (event.button()==1) draft=draft.withIcon(chosen,null);
                    else Minecraft.getInstance().setScreen(new SlotIconScreen(this,key,
                            icon -> draft=draft.withIcon(chosen,icon)));
                } else if (event.button()==0) draft=draft.withColour(inventory,slot,
                        (inventory?draft.inventorySlots():draft.slots()).containsKey(slot)?null:brush);
                else {
                    final int chosen=slot;
                    final boolean area=inventory;
                    Minecraft.getInstance().setScreen(new GuiColorPickerScreen(this,"Slot "+(slot+1)+" colour",
                            0xFF000000|(inventory?draft.inventorySlots():draft.slots()).getOrDefault(slot,brush),SlotOverlay.DEFAULT_COLOUR,
                            colour -> draft=draft.withColour(area,chosen,colour)));
                }
                return true;
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xEB0B101B);
        g.text(font,title,(width-font.width(title))/2,16,0xFFFFFFFF,false);
        var help=Component.literal(iconMode?"Left: choose icon   Right: remove icon":"Left: toggle slot   Right: slot colour");
        g.text(font,help,(width-font.width(help))/2,80,0xFFADBBD0,false);
        g.text(font,"Container",left,top-10,0xFFADBBD0,false);
        g.text(font,"Inventory",left,top+draft.rows()*pitch+4,0xFFADBBD0,false);
        for (int index=0;index<draft.rows()*9+36;index++) {
            boolean inventory=index>=draft.rows()*9;
            int slot=inventory?index-draft.rows()*9:index;
            int x=left+(slot%9)*pitch,y=slotY(inventory,slot),size=pitch-2;
            Integer colour=(inventory?draft.inventorySlots():draft.slots()).get(slot);
            String icon=draft.icons().get(inventory?slot+54:slot);
            g.fill(x-1,y-1,x+size+1,y+size+1,0xFF576176);
            g.fill(x,y,x+size,y+size,0xFF18202E);
            if (icon!=null) {
                var stack=iconCache.computeIfAbsent(icon,SlotIcons::decode);
                g.pose().pushMatrix(); g.pose().translate(x,y); g.pose().scale(size/16f,size/16f);
                g.item(stack,0,0); g.pose().popMatrix();
            }
            if (colour!=null) g.fill(x,y,x+size,y+size,SlotOverlay.translucent(colour,MenuCursor.overlayOpacity()));
            if (pitch>=16 && icon==null) {
                var number=Component.literal(Integer.toString(slot+1));
                g.text(font,number,x+(size-font.width(number))/2,y+(size-8)/2,0xFFFFFFFF,false);
            }
        }
        super.extractRenderState(g,mx,my,delta);
    }
    private int slotY(boolean inventory,int slot) {
        return top+(inventory?draft.rows()*pitch+16:0)+(slot/9)*pitch+(inventory&&slot>=27?4:0);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
