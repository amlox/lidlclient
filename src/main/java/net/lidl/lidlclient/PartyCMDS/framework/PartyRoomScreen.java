package net.lidl.lidlclient.PartyCMDS.framework;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Explicit room connection; no public server is contacted by default. */
public final class PartyRoomScreen extends Screen {
    private final Screen parent;
    private EditBox endpoint,room,token;
    private String error="";
    public PartyRoomScreen(Screen parent) { super(Component.literal("Shared party replies")); this.parent=parent; }
    @Override protected void init() {
        String address=endpoint==null?PartyRoomSync.endpoint():endpoint.getValue();
        String name=room==null?PartyRoomSync.room():room.getValue();
        String secret=token==null?PartyRoomSync.token():token.getValue();
        int left=(width-296)/2;
        endpoint=addRenderableWidget(new EditBox(font,left,46,296,20,Component.literal("WebSocket server")));
        endpoint.setMaxLength(512); endpoint.setValue(address);
        room=addRenderableWidget(new EditBox(font,left,88,296,20,Component.literal("Room name")));
        room.setMaxLength(48); room.setValue(name);
        token=addRenderableWidget(new EditBox(font,left,130,296,20,Component.literal("Room token")));
        token.setMaxLength(128); token.setValue(secret);
        token.addFormatter((value,offset) -> net.minecraft.util.FormattedCharSequence.forward("*".repeat(value.length()),net.minecraft.network.chat.Style.EMPTY));
        addRenderableWidget(Button.builder(Component.literal("Connect / retry"),b -> {
            try { PartyRoomSync.connect(endpoint.getValue(),room.getValue(),token.getValue()); error=""; }
            catch (RuntimeException exception) { error=exception.getMessage(); }
        }).bounds(left,160,144,20).build());
        addRenderableWidget(Button.builder(Component.literal("Disconnect"),b -> { PartyRoomSync.disconnect(); error=""; })
                .bounds(left+150,160,146,20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"),b -> onClose()).bounds(width/2-50,height-28,100,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        int left=(width-296)/2;
        g.fill(0,0,width,height,0xEB0B101B); g.centeredText(font,title,width/2,16,0xFFFFFFFF);
        g.text(font,"Server (ws:// for local tests; wss:// for hosting)",left,34,0xFFADBBD0);
        g.text(font,"Private room name",left,76,0xFFADBBD0);
        g.text(font,"Room token (12+ characters; saved on this device)",left,118,0xFFADBBD0);
        g.text(font,font.plainSubstrByWidth(error.isEmpty()?PartyRoomSync.status():error,width-24),12,height-47,0xFFFFFFFF);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
