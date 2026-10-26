package net.lidl.lidlclient.cursor;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A paged, persistent key list; no fixed number of bindings. */
public final class MenuCursorKeysScreen extends Screen {
    private final Screen parent;
    private int page;
    private int perPage;
    private boolean capturing;
    public MenuCursorKeysScreen(Screen parent) {
        super(Component.literal("Menu cursor keybinds"));
        this.parent = parent;
    }
    @Override protected void init() {
        perPage = Math.max(1,(height-145)/24);
        var keys = MenuCursor.keys();
        page = Math.clamp(page,0,Math.max(0,(keys.size()-1)/perPage));
        int left = width/2-135;
        for (int row = 0; row < perPage && page*perPage+row < keys.size(); row++) {
            int index = page*perPage+row;
            int y = 66+row*24;
            var key=keys.get(index);
            addRenderableWidget(Button.builder(key.getDisplayName().copy().append(" · Slots"),b ->
                    Minecraft.getInstance().setScreen(new SlotOverlayScreen(this,key)))
                    .bounds(left,y,120,20).build()).active = !capturing;
            addRenderableWidget(Button.builder(Component.literal(MenuCursor.command(key).isEmpty() ? "Command" : "Edit cmd"),b ->
                    Minecraft.getInstance().setScreen(new MenuCommandScreen(this,key)))
                    .bounds(left+124,y,68,20).build()).active = !capturing;
            addRenderableWidget(Button.builder(Component.literal("Remove"),b -> {
                MenuCursor.removeKey(index); rebuildWidgets();
            }).bounds(left+196,y,74,20).build()).active = !capturing;
        }
        int bottom = height-66;
        addRenderableWidget(Button.builder(Component.literal("Previous"),b -> { page--; rebuildWidgets(); })
                .bounds(left,bottom,80,20).build()).active = !capturing && page>0;
        addRenderableWidget(Button.builder(Component.literal(capturing ? "Press a key..." : "Add keybind"),b -> {
            capturing = true; rebuildWidgets();
        }).bounds(left+86,bottom,98,20).build()).active = !capturing;
        addRenderableWidget(Button.builder(Component.literal("Next"),b -> { page++; rebuildWidgets(); })
                .bounds(left+190,bottom,80,20).build()).active = !capturing && (page+1)*perPage<keys.size();
        addRenderableWidget(Button.builder(Component.literal("Done"),b -> onClose()).bounds(width/2-50,height-40,100,20).build());
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (!capturing) return super.keyPressed(event);
        capturing = false;
        if (!event.isEscape()) {
            MenuCursor.addKey(event.key() == GLFW.GLFW_KEY_UNKNOWN
                    ? InputConstants.Type.SCANCODE.getOrCreate(event.scancode())
                    : InputConstants.Type.KEYSYM.getOrCreate(event.key()));
            page = Math.max(0,(MenuCursor.keys().size()-1)/perPage);
        }
        rebuildWidgets();
        return true;
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick) {
        if (!capturing) return super.mouseClicked(event,doubleClick);
        MenuCursor.addKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
        capturing=false;
        page=Math.max(0,(MenuCursor.keys().size()-1)/perPage);
        rebuildWidgets();
        return true;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xEB0B101B);
        g.text(font,title,(width-font.width(title))/2,16,0xFFFFFFFF,false);
        var line = Component.literal(capturing ? "Press a key or mouse button; Esc cancels." : "Bind a key or mouse button; optionally add a command.");
        g.text(font,line,(width-font.width(line))/2,34,0xFFADBBD0,false);
        var hint = Component.literal("Esc cancels a wait. Existing hotkeys still work.");
        g.text(font,hint,Math.max(4,(width-font.width(hint))/2),height-83,0xFFADBBD0,false);
        if (MenuCursor.keys().isEmpty()) g.text(font,Component.literal("No keys added yet."),width/2-55,72,0xFFADBBD0,false);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
