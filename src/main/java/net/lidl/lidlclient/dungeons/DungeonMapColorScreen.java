package net.lidl.lidlclient.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Palette overview. All edits stay in a draft until Save, including after resizing. */
public final class DungeonMapColorScreen extends Screen {
    private final Screen parent;
    private final Map<DungeonMapPalette.Key,Integer> draft=new EnumMap<>(DungeonMapPalette.Key.class);
    private final Map<DungeonMapPalette.Key,Button> buttons=new EnumMap<>(DungeonMapPalette.Key.class);
    private int left,top,rowHeight,columnWidth,rows;
    public DungeonMapColorScreen(Screen parent) {
        super(Component.literal("Dungeon map colours"));this.parent=parent;
        for(var key:DungeonMapPalette.Key.values()) draft.put(key,DungeonMapColors.palette().get(key));
    }
    @Override protected void init() {
        buttons.clear();int panelWidth=Math.min(520,width-24);left=(width-panelWidth)/2;top=43;
        columnWidth=panelWidth/2;rows=(DungeonMapPalette.Key.values().length+1)/2;
        rowHeight=Math.max(20,Math.min(30,(height-110)/rows));int i=0;
        for(var key:DungeonMapPalette.Key.values()) {
            int x=left+(i/rows)*columnWidth,y=top+(i%rows)*rowHeight;
            buttons.put(key,addRenderableWidget(Button.builder(Component.literal(key.label),b->Minecraft.getInstance().setScreen(
                    new DungeonColorPickerScreen(this,key,draft.get(key),color->draft.put(key,color))))
                    .bounds(x+20,y,columnWidth-24,20).build()));i++;
        }
        int y=top+rows*rowHeight+9,w=(panelWidth-12)/3;
        addRenderableWidget(Button.builder(Component.literal("Save"),b->{DungeonMapColors.save(draft);onClose();}).bounds(left,y,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Defaults"),b->{for(var key:DungeonMapPalette.Key.values()) draft.put(key,0xFF000000|key.rgb);}).bounds(left+w+6,y,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(left+2*(w+6),y,w,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xEB0B101B);g.text(font,title,(width-font.width(title))/2,15,0xFFFFFFFF,true);
        String hint="Click a colour to pick visually, or paste a hex code.";
        g.text(font,Component.literal(hint),(width-font.width(hint))/2,29,0xFFADBBD0,false);
        for(var entry:buttons.entrySet()) {var b=entry.getValue();g.fill(b.getX()-18,b.getY()+2,b.getX()-2,b.getY()+18,0xFFCCCCCC);g.fill(b.getX()-17,b.getY()+3,b.getX()-3,b.getY()+17,draft.get(entry.getKey()));}
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() {Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen() {return false;}
}
