package net.lidl.lidlclient.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DungeonRelayStatusScreen extends Screen {
    private final Screen parent;
    public DungeonRelayStatusScreen(Screen parent) {super(Component.literal("Lidl secret sync"));this.parent=parent;}
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Done"),button->onClose()).bounds(width/2-45,height-35,90,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xED0B101B);g.text(font,title,(width-font.width(title))/2,30,0xFFFFFFFF,true);
        String[] lines={"LidlClient: "+DungeonMap.scanner().sync().status(),"Transport: Odin WebSocket relay", "Receives LidlClient-tagged party counts only.","No secret reports are sent to Hypixel chat."};
        for(int i=0;i<lines.length;i++) g.text(font,Component.literal(lines[i]),20,65+i*22,0xFFE1E9FA,true);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() {Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen() {return false;}
}
