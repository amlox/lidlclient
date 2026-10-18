package net.lidl.lidlclient.names;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A real text input, with explicit save/cancel and a blank-name reset. */
public final class SelfNameScreen extends Screen {
    private final Screen parent;
    private EditBox input;
    private int top;

    public SelfNameScreen(Screen parent) {
        super(Component.literal("Your display name"));
        this.parent = parent;
    }

    @Override protected void init() {
        NameChanger.setEditing(true);
        String draft = input == null ? NameChanger.customName() : input.getValue();
        int boxWidth = Math.max(1, Math.min(280, width - 24));
        top = Math.max(12, height / 2 - 55);
        input = new EditBox(font, (width - boxWidth) / 2, top + 30, boxWidth, 20,
                Component.literal("Custom display name"));
        input.setMaxLength(64);
        input.setValue(draft);
        addRenderableWidget(input);
        setInitialFocus(input);
        int buttonWidth = Math.max(1, (boxWidth - 12) / 3);
        int left = (width - boxWidth) / 2;
        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                .bounds(left, top + 62, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            NameChanger.setEditing(false);
            NameChanger.setCustomName("");
            onClose();
        }).bounds(left + buttonWidth + 6, top + 62, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(left + 2 * (buttonWidth + 6), top + 62, buttonWidth, 20).build());
    }

    private void save() {
        NameChanger.setEditing(false);
        NameChanger.setCustomName(input.getValue());
        onClose();
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xEA080D17);
        graphics.text(font, title, (width - font.width(title)) / 2, top, 0xFFFFFFFF, true);
        String hint = "Blank resets to default. Up to 32 characters.";
        graphics.text(font, Component.literal(hint), (width - font.width(hint)) / 2, top + 16, 0xFFAAAAAA, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override public void removed() { NameChanger.setEditing(false); }

    @Override public void onClose() {
        NameChanger.setEditing(false);
        Minecraft.getInstance().setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
}
