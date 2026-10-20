package net.lidl.lidlclient.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Rendering and feature state belong to the panel; placement and input belong to the host. */
public interface HudPanel {
    String id();
    boolean enabled();
    int preferredWidth();
    int preferredHeight();
    void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean editing);
}
