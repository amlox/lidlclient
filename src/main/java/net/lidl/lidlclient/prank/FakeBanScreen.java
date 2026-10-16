package net.lidl.lidlclient.prank;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Cosmetic only: keeps the server connection open, and Esc returns to play. */
public final class FakeBanScreen extends Screen {
    public FakeBanScreen() {
        super(Component.literal("Disconnected"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xFF171717);
        int top = Math.max(12, height / 2 - 55);
        line(graphics, "You are permanently banned from this server!", top, 0xFFFF5555);
        line(graphics, "Reason: Failed a dungeon puzzle.", top + 25, 0xFFFFFFFF);
        line(graphics, "Ban ID: #PUZZLEFAIL", top + 45, 0xFFAAAAAA);
        line(graphics, "LidlClient prank - press Esc to return", top + 80, 0xFF55FF55);
    }

    private void line(GuiGraphicsExtractor graphics, String text, int y, int color) {
        graphics.text(font, Component.literal(text), (width - font.width(text)) / 2, y, color, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
