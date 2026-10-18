package net.lidl.lidlclient.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Eight separate inputs with live swatches, validation, defaults and atomic Save. */
public final class DungeonMapColorScreen extends Screen {
    private final Screen parent;
    private final Map<DungeonMapPalette.Key,EditBox> inputs=new EnumMap<>(DungeonMapPalette.Key.class);
    private Button save;
    private int left,top,columnWidth,rowHeight;
    public DungeonMapColorScreen(Screen parent) {super(Component.literal("Dungeon map colours"));this.parent=parent;}
    @Override protected void init() {
        Map<DungeonMapPalette.Key,String> drafts=new EnumMap<>(DungeonMapPalette.Key.class);
        inputs.forEach((key,input)->drafts.put(key,input.getValue()));inputs.clear();
        int panelWidth=Math.max(120,Math.min(420,width-20));
        columnWidth=panelWidth/2;left=(width-panelWidth)/2;
        rowHeight=Math.max(30,Math.min(42,(height-90)/4));top=Math.max(28,(height-(4*rowHeight+60))/2);
        int i=0;
        for(var key:DungeonMapPalette.Key.values()) {
            int x=left+(i/4)*columnWidth,y=top+(i%4)*rowHeight;
            EditBox input=new EditBox(font,x+20,y+12,Math.max(40,columnWidth-32),18,Component.literal(key.label));
            input.setMaxLength(9);input.setValue(drafts.getOrDefault(key,DungeonMapPalette.hex(DungeonMapColors.palette().get(key))));
            inputs.put(key,input);addRenderableWidget(input);input.setResponder(value->validate());i++;
        }
        int buttonY=top+4*rowHeight+5,buttonWidth=Math.max(30,(panelWidth-12)/3);
        save=addRenderableWidget(Button.builder(Component.literal("Save"),b->save()).bounds(left,buttonY,buttonWidth,20).build());
        addRenderableWidget(Button.builder(Component.literal("Defaults"),b->{
            inputs.forEach((key,input)->input.setValue(DungeonMapPalette.hex(key.rgb)));validate();
        }).bounds(left+buttonWidth+6,buttonY,buttonWidth,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose())
                .bounds(left+2*(buttonWidth+6),buttonY,buttonWidth,20).build());
        validate();
    }
    private boolean valid() {return inputs.values().stream().allMatch(input->DungeonMapPalette.parse(input.getValue())!=null);}
    private void validate() {if(save!=null) save.active=valid();}
    private void save() {
        if(!valid()) return;
        Map<DungeonMapPalette.Key,Integer> colors=new EnumMap<>(DungeonMapPalette.Key.class);
        inputs.forEach((key,input)->colors.put(key,DungeonMapPalette.parse(input.getValue())));
        DungeonMapColors.save(colors);onClose();
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(event.key()==GLFW.GLFW_KEY_ENTER || event.key()==GLFW.GLFW_KEY_KP_ENTER) {save();return true;}
        return super.keyPressed(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        g.fill(0,0,width,height,0xEB0B101B);
        g.text(font,title,(width-font.width(title))/2,top-25,0xFFFFFFFF,true);
        String hint=valid()?"Use #RRGGBB, &#RRGGBB or &c. Save applies all colours.":"Invalid colour: use 6 hex digits, or a Minecraft code (&0 to &f).";
        g.text(font,Component.literal(hint),(width-font.width(hint))/2,top+4*rowHeight+32,valid()?0xFFADBBD0:0xFFFF6666,false);
        int i=0;
        for(var key:DungeonMapPalette.Key.values()) {
            int x=left+(i/4)*columnWidth,y=top+(i%4)*rowHeight;
            Integer color=DungeonMapPalette.parse(inputs.get(key).getValue());
            g.text(font,Component.literal(key.label),x+2,y,0xFFFFFFFF,true);
            g.fill(x+2,y+13,x+17,y+29,0xFFAAAAAA);g.fill(x+3,y+14,x+16,y+28,color==null?0xFFFF3333:color);i++;
        }
        super.extractRenderState(g,mouseX,mouseY,delta);
    }
    @Override public void onClose() {Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen() {return false;}
}
