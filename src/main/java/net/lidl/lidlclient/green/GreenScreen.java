package net.lidl.lidlclient.green;

import net.lidl.lidlclient.config.ClientConfig;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.lidl.lidlclient.LidlClient;

/** A purely visual, full-screen translucent green tint. */
public final class GreenScreen {
    private static boolean enabled;
    private static double strength=35;
    public static double strength() { return strength; }
    public static void setStrength(double value) {
        if (!Double.isFinite(value)) return;
        strength=Math.clamp(value,0,100);
        ClientConfig.set("green_screen","Green strength (%)",strength);
    }
    public static int tint(double percent) {
        double opacity=Double.isFinite(percent)?Math.clamp(percent,0,100):35;
        return ((int)Math.round(opacity*255/100)<<24)|0x00FF00;
    }

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
                        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), tint(strength));
                    }
                });
    }
}
