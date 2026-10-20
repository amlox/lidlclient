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

/** Draft editor: left-click selects slots, right-click edits one slot's colour. */
public final class SlotOverlayScreen extends Screen {
    private final Screen parent;
    private final InputConstants.Key key;
    private SlotOverlay draft;
    private int brush = SlotOverlay.DEFAULT_COLOUR;
    private int left,top,pitch;
    public SlotOverlayScreen(Screen parent,InputConstants.Key key) {
        super(Component.literal("Slot overlay: ").append(key.getDisplayName()));
        this.parent = parent;
        this.key = key;
        draft = MenuCursor.overlay(key);
    }
    @Override protected void init() {
        pitch = Math.min(18,Math.max(8,(height-132)/draft.rows()));
        left = (width-9*pitch)/2;
        top = 74;
        int controls = width/2-115;
        addRenderableWidget(Button.builder(Component.literal(draft.rows()*9+" slots ("+draft.rows()+" rows)"),b -> {
            draft = draft.withRows(draft.rows()%6+1); rebuildWidgets();
        }).bounds(controls,34,120,20).build());
        addRenderableWidget(Button.builder(Component.literal("Brush "+GuiColor.hex(brush)),b ->
                Minecraft.getInstance().setScreen(new GuiColorPickerScreen(this,"New slot colour",0xFF000000|brush,
                        SlotOverlay.DEFAULT_COLOUR,colour -> brush=colour & 0xFFFFFF)))
                .bounds(controls+126,34,104,20).build());
        int bottom = height-28;
        addRenderableWidget(Button.builder(Component.literal("Save"),b -> {
            MenuCursor.setOverlay(key,draft); onClose();
        }).bounds(width/2-120,bottom,76,20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"),b -> {
            draft = new SlotOverlay(draft.rows(),Map.of());
        }).bounds(width/2-38,bottom,76,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b -> onClose())
                .bounds(width/2+44,bottom,76,20).build());
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        if (event.button() == 0 || event.button() == 1) {
            for (int slot=0;slot<draft.rows()*9;slot++) {
                int x=left+(slot%9)*pitch,y=top+(slot/9)*pitch;
                if (event.x()<x || event.x()>=x+pitch-2 || event.y()<y || event.y()>=y+pitch-2) continue;
                if (event.button()==0) draft=draft.withSlot(slot,draft.slots().containsKey(slot)?null:brush);
                else {
                    final int chosen=slot;
                    Minecraft.getInstance().setScreen(new GuiColorPickerScreen(this,"Slot "+(slot+1)+" colour",
                            0xFF000000|draft.slots().getOrDefault(slot,brush),SlotOverlay.DEFAULT_COLOUR,
                            colour -> draft=draft.withSlot(chosen,colour)));
                }
                return true;
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xEB0B101B);
        g.text(font,title,(width-font.width(title))/2,16,0xFFFFFFFF,false);
        var help=Component.literal("Left: toggle slot   Right: slot colour");
        g.text(font,help,(width-font.width(help))/2,60,0xFFADBBD0,false);
        for (int slot=0;slot<draft.rows()*9;slot++) {
            int x=left+(slot%9)*pitch,y=top+(slot/9)*pitch,size=pitch-2;
            Integer colour=draft.slots().get(slot);
            g.fill(x-1,y-1,x+size+1,y+size+1,0xFF576176);
            g.fill(x,y,x+size,y+size,0xFF18202E);
            if (colour!=null) g.fill(x,y,x+size,y+size,SlotOverlay.translucent(colour,MenuCursor.overlayOpacity()));
            if (pitch>=16) {
                var number=Component.literal(Integer.toString(slot+1));
                g.text(font,number,x+(size-font.width(number))/2,y+(size-8)/2,0xFFFFFFFF,false);
            }
        }
        var hint=Component.literal("Colours show while the menu is loading.");
        g.text(font,hint,(width-font.width(hint))/2,height-46,0xFFADBBD0,false);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
