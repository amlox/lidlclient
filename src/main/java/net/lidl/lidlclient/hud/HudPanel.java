package net.lidl.lidlclient.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Rendering and feature state belong to the panel; placement and input belong to the host. */
public interface HudPanel {
    String id();
    boolean enabled();
    int preferredWidth();
    int preferredHeight();
    default int minimumWidth() { return 180; }
    default int minimumHeight() { return 180; }
    default boolean click(double x,double y,int width,int height) { return false; }
    void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean editing);
}
