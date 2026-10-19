package net.lidl.lidlclient.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DungeonRelayStatusScreen extends Screen {
    private final Screen parent;
    public DungeonRelayStatusScreen(Screen parent) {super(Component.literal("Dungeon secret sync"));this.parent=parent;}
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Done"),button->onClose()).bounds(width/2-45,height-35,90,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xED0B101B);g.text(font,title,(width-font.width(title))/2,30,0xFFFFFFFF,true);
        int y=65;for(var backend:DungeonRelayProtocol.Backend.values()) {
            String line=backend+": "+DungeonMap.scanner().sync().status(backend);
            g.text(font,Component.literal(line),20,y,0xFFE1E9FA,true);y+=22;
        }
        String hint="Uses WebSockets. No party-chat messages or chat fallback.";
        g.text(font,Component.literal(hint),20,y+15,0xFFADBBD0,false);super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() {Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen() {return false;}
}
