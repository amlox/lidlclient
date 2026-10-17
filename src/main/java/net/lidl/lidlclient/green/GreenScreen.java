package net.lidl.lidlclient.green;

import net.lidl.lidlclient.config.ClientConfig;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.lidl.lidlclient.LidlClient;

/** A purely visual, full-screen translucent green tint. */
public final class GreenScreen {
    private static boolean enabled;
    // ARGB: 35% opacity, pure green.
    private static final int TINT = 0x5900FF00;

    private GreenScreen() { }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        ClientConfig.set("green_screen", "enabled", value);
    }

    public static void initialize() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                LidlClient.id("green_screen"),
                (graphics, deltaTracker) -> {
                    if (enabled) {
                        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), TINT);
                    }
                });
    }
}
