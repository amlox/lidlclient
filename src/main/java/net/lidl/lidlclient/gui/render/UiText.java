package net.lidl.lidlclient.gui.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/** The same custom font is used for drawing, measuring, centering and truncation. */
public final class UiText {
    private static final Style STYLE = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("lidlclient", "ui")));
    public static final int HEIGHT = 10;
    private UiText() { }
    private static final java.util.Map<String, Component> COMPONENTS = new java.util.LinkedHashMap<>(256, .75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, Component> entry) {
            return size() > 256;
        }
    };

    public static Component component(String text) { return COMPONENTS.computeIfAbsent(text, value -> Component.literal(value).withStyle(STYLE)); }
    public static int width(Font font, String text) { return font.width(component(text)); }

    public static void draw(GuiGraphicsExtractor graphics, Font font, String text,
                            int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) return;
        String clipped = text;
        if (width(font, text) > maxWidth) {
            if (width(font, "…") > maxWidth) return;
            int[] codepoints = text.codePoints().toArray();
            int low = 0, high = codepoints.length;
            while (low < high) {
                int mid = (low + high + 1) / 2;
                if (width(font, new String(codepoints, 0, mid) + "…") <= maxWidth) low = mid;
                else high = mid - 1;
            }
            clipped = new String(codepoints, 0, low) + "…";
        }
        graphics.text(font, component(clipped), x, y, color, false);
    }
}
