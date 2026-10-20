package net.lidl.lidlclient.gui;

import net.lidl.lidlclient.config.ClientConfig;
import net.minecraft.client.Minecraft;

/** Saved independently of the module's category or whether the menu is open. */
public final class GuiAppearance {
    public static final int DEFAULT_ACCENT = 0xFFF4F4FA;
    public static final int DEFAULT_BACKGROUND = 0xFF05070D;
    private static int accent = load("Accent colour", DEFAULT_ACCENT);
    private static int background = load("Background colour", DEFAULT_BACKGROUND);
    private static boolean stars = ClientConfig.bool("gui_appearance", "Animated stars", true);
    private GuiAppearance() { }
    private static int load(String key, int fallback) {
        String text = ClientConfig.string("gui_appearance",key);
        Integer parsed = text == null ? null : GuiColor.parse(text);
        return parsed == null ? fallback : parsed;
    }
    public static int accent() { return accent; }
    public static int background() { return 0x98000000 | (background & 0xFFFFFF); }
    public static boolean stars() { return stars; }
    public static void setStars(boolean value) {
        stars = value;
        ClientConfig.set("gui_appearance","Animated stars",value);
    }
    public static int tint(int color) {
        // Keep the existing opacity and shading while applying the selected accent hue.
        int r = (color >> 16 & 255) * (accent >> 16 & 255) / 244;
        int g = (color >> 8 & 255) * (accent >> 8 & 255) / 244;
        int b = (color & 255) * (accent & 255) / 250;
        return (color & 0xFF000000) | (Math.min(255,r) << 16) | (Math.min(255,g) << 8) | Math.min(255,b);
    }
    public static void editAccent() { edit(true); }
    public static void editBackground() { edit(false); }
    private static void edit(boolean isAccent) {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new GuiColorPickerScreen(client.screen,
                isAccent ? "GUI accent colour" : "GUI background colour",
                isAccent ? accent : background,
                isAccent ? DEFAULT_ACCENT : DEFAULT_BACKGROUND,
                color -> {
                    if (isAccent) accent = color; else background = color;
                    ClientConfig.set("gui_appearance",isAccent ? "Accent colour" : "Background colour",GuiColor.hex(color));
                }));
    }
    public static void reset() {
        accent = DEFAULT_ACCENT;
        background = DEFAULT_BACKGROUND;
        ClientConfig.set("gui_appearance","Accent colour",null);
        ClientConfig.set("gui_appearance","Background colour",null);
    }
}
