package net.lidl.lidlclient.cursor;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MenuCommandScreen extends Screen {
    private final Screen parent;
    private final InputConstants.Key key;
    private String draft;
    private String error="";
    public MenuCommandScreen(Screen parent,InputConstants.Key key) {
        super(Component.literal("Menu command"));
        this.parent=parent; this.key=key; draft=MenuCursor.command(key);
    }
    @Override protected void init() {
        int left=width/2-150;
        var input=new EditBox(font,left,66,300,20,Component.literal("Command"));
        input.setMaxLength(257);
        input.setValue(draft);
        input.setResponder(value -> { draft=value; error=""; });
        addRenderableWidget(input);
        setInitialFocus(input);
        addRenderableWidget(Button.builder(Component.literal("Save"),b -> {
            try { MenuCursor.setCommand(key,draft); onClose(); }
            catch (IllegalArgumentException invalid) { error=invalid.getMessage(); }
        }).bounds(left,118,146,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b -> onClose())
                .bounds(left+154,118,146,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xEB0B101B);
        g.centeredText(font,title,width/2,16,0xFFFFFFFF);
        g.centeredText(font,"Server command, e.g. /pets or /storage",width/2,36,0xFFADBBD0);
        g.centeredText(font,"Blank: only watch another mod's hotkey.",width/2,50,0xFFADBBD0);
        if (!error.isEmpty()) g.centeredText(font,error,width/2,94,0xFFFF7777);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
