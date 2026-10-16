package net.lidl.lidlclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.lidl.lidlclient.slots.Slots;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ShizoStyleScreen extends Screen {

    // ==============================
    // COLORS
    // ==============================

    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFFBBBBBB;

    // Transparent backgrounds
    private static final int MAIN_BG = 0x6008080A;
    private static final int SIDEBAR_BG = 0x44080808;
    private static final int CARD_BG = 0x4008080A;
    private static final int ENABLED_BG = 0x60404048;
    // Tune panel opacity with the alpha byte of MAIN_BG.
    private static final long SLIDE_DURATION_NS = 500_000_000L;
    private final long openedAt = System.nanoTime();
    private final String[] categories = {"General", "Social"};
    private String selectedCategory = "General";

    private int guiX;
    private int guiY;
    private int guiWidth;
    private int guiHeight;
    private float guiScale;

    // ==============================
    // MODULES
    // ==============================

    private final List<GuiModule> modules = new ArrayList<>();

    // Right-click a module to expand its settings. Only one opens at a time.
    private int expandedModule = -1;

    public ShizoStyleScreen() {
        super(Component.literal("LidlClient"));

        modules.add(new GuiModule(
                "Slots",
                "Slots module",
                "General",
                () -> ModuleController.setSlotsEnabled(true),
                () -> ModuleController.setSlotsEnabled(false)
        ));

        modules.add(new GuiModule(
                "PartyCMDS",
                "Party commands",
                "Social",
                () -> ModuleController.setPartyCommandsEnabled(true),
                () -> ModuleController.setPartyCommandsEnabled(false)
        ));

        modules.add(new GuiModule(
                "Circle",
                "Circle module",
                "General",
                () -> ModuleController.setCircleEnabled(true),
                () -> ModuleController.setCircleEnabled(false)
        ));

        // Restore module states when GUI opens
        for (GuiModule module : modules) {
            switch (module.name) {
                case "Slots" ->
                        module.enabled =
                                ModuleController.isSlotsEnabled();

                case "PartyCMDS" ->
                        module.enabled =
                                ModuleController.isPartyCommandsEnabled();

                case "Circle" ->
                        module.enabled =
                                ModuleController.isCircleEnabled();
            }
        }
    }

    // ==============================
    // SCALING
    // ==============================

    private int scaled(float value) {
        return Math.max(1, Math.round(value * guiScale));
    }

    private void calculateGuiLayout() {

        guiWidth = Math.min(width, Math.max(200, Math.min(300, Math.round(width * 0.32f))));
        guiHeight = height;
        guiScale = guiWidth / 300.0f;
        guiY = 0;
        updateSlide();

    }

    // ==============================
    // ROUNDED RECTANGLES
    // ==============================

    private void roundedRect(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int rectWidth,
            int rectHeight,
            int radius,
            int color
    ) {
        if (rectWidth <= 0 || rectHeight <= 0) {
            return;
        }

        radius = Math.max(
                0,
                Math.min(
                        radius,
                        Math.min(rectWidth, rectHeight) / 2
                )
        );

        if (radius == 0) {
            graphics.fill(
                    x, y,
                    x + rectWidth,
                    y + rectHeight,
                    color
            );
            return;
        }

        for (int row = 0; row < rectHeight; row++) {
            int inset = 0;

            if (row < radius) {
                double dy = radius - row - 0.5;

                inset = radius - (int) Math.sqrt(
                        Math.max(0, radius * radius - dy * dy)
                );
            } else if (row >= rectHeight - radius) {
                double dy =
                        row - (rectHeight - radius) + 0.5;

                inset = radius - (int) Math.sqrt(
                        Math.max(0, radius * radius - dy * dy)
                );
            }

            graphics.fill(
                    x + inset,
                    y + row,
                    x + rectWidth - inset,
                    y + row + 1,
                    color
            );
        }
    }

    private void roundedBorder(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int rectWidth,
            int rectHeight,
            int radius,
            int thickness,
            int borderColor,
            int backgroundColor
    ) {
        if (rectWidth <= 0 || rectHeight <= 0) return;
        thickness = Math.max(1, thickness);
        radius = Math.max(0, Math.min(radius, Math.min(rectWidth, rectHeight) / 2));
        // Draw only the border pixels: a solid rectangle behind a translucent
        // interior would make the entire button opaque.
        roundedRect(graphics, x, y, rectWidth, rectHeight, radius, backgroundColor);
        for (int row = 0; row < rectHeight; row++) {
            int outer = cornerInset(row, rectHeight, radius);
            int left = x + outer;
            int right = x + rectWidth - outer;
            if (row < thickness || row >= rectHeight - thickness) {
                graphics.fill(left, y + row, right, y + row + 1, borderColor);
            } else {
                int inner = thickness + cornerInset(row - thickness,
                        rectHeight - 2 * thickness, Math.max(0, radius - thickness));
                graphics.fill(left, y + row, x + inner, y + row + 1, borderColor);
                graphics.fill(x + rectWidth - inner, y + row, right, y + row + 1, borderColor);
            }
        }
    }

    private int cornerInset(int row, int height, int radius) {
        if (radius == 0) return 0;
        double dy = row < radius ? radius - row - 0.5
                : (row >= height - radius ? row - (height - radius) + 0.5 : 0);
        return dy == 0 ? 0 : radius - (int) Math.sqrt(Math.max(0, radius * radius - dy * dy));
    }

    // ==============================
    // INITIALIZATION
    // ==============================

    @Override
    protected void init() {
        calculateGuiLayout();
    }

    private void updateSlide() {
        double progress = Math.min(1.0, Math.max(0.0,
                (System.nanoTime() - openedAt) / (double) SLIDE_DURATION_NS));
        double eased = 1.0 - Math.pow(1.0 - progress, 3);
        guiX = (int) Math.round(-(guiWidth + scaled(12)) * (1.0 - eased));
    }

    private boolean visible(int index) {
        return modules.get(index).tab.equals(selectedCategory);
    }

    private int moduleX() {
        return guiX + scaled(62);
    }

    private int dropdownHeight(int index) {
        return scaled(index == 0 ? 126 : 42);
    }

    private int moduleY(int index) {
        int row = 0;
        for (int i = 0; i < index; i++) {
            if (visible(i)) row++;
        }
        return guiY + scaled(38) + row * scaled(62)
                + (expandedModule >= 0 && visible(expandedModule) && expandedModule < index
                ? dropdownHeight(expandedModule) + scaled(7) : 0);
    }

    private int moduleWidth() {
        return guiWidth - scaled(90);
    }

    private int moduleHeight() {
        return scaled(48);
    }

    // Nested low-alpha strips create a soft glow without a shader dependency.
    private void luminousLine(GuiGraphicsExtractor graphics, int x, int y, int h) {
        for (int radius = scaled(9); radius >= 1; radius--) {
            int alpha = Math.max(2, 24 / radius);
            graphics.fill(x - radius, y, x + radius + 1, y + h,
                    (alpha << 24) | 0xFFFFFF);
        }
        graphics.fill(x, y, x + 1, y + h, WHITE);
        graphics.fill(x - scaled(4), y, x - scaled(3), y + h, 0x88777777);
        graphics.fill(x + scaled(3), y, x + scaled(4), y + h, 0xAAFFFFFF);
    }

    private boolean hovering(double mouseX, double mouseY,
                             int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w
                && mouseY >= y && mouseY < y + h;
    }

    // ==============================
    // RENDERING
    // ==============================

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        updateSlide();
        int x = guiX;
        int w = guiWidth;
        graphics.fill(x, 0, x + w, guiHeight, MAIN_BG);
        graphics.fill(x, 0, x + scaled(50), guiHeight, SIDEBAR_BG);
        graphics.fill(x + scaled(50), 0, x + scaled(50) + 1, guiHeight, 0xAAFFFFFF);
        luminousLine(graphics, x + w - scaled(5), 0, guiHeight);

        for (int category = 0; category < categories.length; category++) {
            int sideX = x + scaled(6);
            int sideY = scaled(38) + category * scaled(62);
            int size = scaled(36);
            boolean active = categories[category].equals(selectedCategory);
            boolean hovered = hovering(mouseX, mouseY, sideX, sideY, size, size);
            roundedBorder(graphics, sideX, sideY, size, size, scaled(6), 1,
                    active || hovered ? WHITE : 0xFF888888,
                    active ? ENABLED_BG : CARD_BG);
            String label = categories[category].substring(0, 1);
            graphics.text(font, label, sideX + (size - font.width(label)) / 2,
                    sideY + (size - font.lineHeight) / 2, WHITE, true);
            if (hovered) {
                graphics.text(font, categories[category], moduleX(), scaled(15), WHITE, true);
            }
        }

        // Category-filtered module buttons and their settings.
        for (int i = 0; i < modules.size(); i++) {

            if (!visible(i)) continue;
            GuiModule module = modules.get(i);
            int cardX = moduleX();
            int cardY = moduleY(i);
            int cardW = moduleWidth();
            int cardH = moduleHeight();

            boolean hovered = hovering(
                    mouseX, mouseY,
                    cardX, cardY,
                    cardW, cardH
            );

            int border = 1;
            boolean expanded = expandedModule == i;
            int fullHeight = cardH + (expanded ? dropdownHeight(i) : 0);
            if (module.enabled || hovered) {
                for (int spread = scaled(4); spread >= 1; spread--) {
                    roundedRect(graphics, cardX - spread, cardY - spread,
                            cardW + spread * 2, fullHeight + spread * 2,
                            scaled(8) + spread, 0x08FFFFFF);
                }
            }
            roundedBorder(graphics, cardX, cardY, cardW, fullHeight,
                    scaled(8), border, module.enabled || hovered ? WHITE : 0xFF999999,
                    module.enabled ? ENABLED_BG : CARD_BG);
            int labelX = cardX + scaled(10);
            graphics.text(font, module.name,
                    cardX + (cardW - font.width(module.name)) / 2,
                    cardY + (cardH - font.lineHeight) / 2,
                    module.enabled ? WHITE : MUTED, true);

            // Expanded settings share the module's background colour.
            if (expanded) {
                int dropY = cardY + cardH;
                int rowH = scaled(42);
                int rightX = cardX + cardW - scaled(10);

                if (i == 0) {
                    graphics.fill(cardX + scaled(15), dropY,
                            cardX + cardW - scaled(15), dropY + 1, 0x665B4355);
                    graphics.text(font, "Mute chat", labelX,
                            dropY + scaled(15), WHITE, true);
                    String muted = Slots.isMuteMessages() ? "ON" : "OFF";
                    graphics.text(font, muted, rightX - font.width(muted),
                            dropY + scaled(15), Slots.isMuteMessages() ? WHITE : MUTED, true);

                    dropY += rowH;
                    graphics.fill(cardX + scaled(15), dropY,
                            cardX + cardW - scaled(15), dropY + 1, 0x665B4355);
                    graphics.text(font, "Bet / balance", labelX,
                            dropY + scaled(15), WHITE, true);
                    String show = Slots.isShowUi() ? "ON" : "OFF";
                    graphics.text(font, show, rightX - font.width(show),
                            dropY + scaled(15), Slots.isShowUi() ? WHITE : MUTED, true);
                    dropY += rowH;
                }

                graphics.fill(cardX + scaled(15), dropY,
                        cardX + cardW - scaled(15), dropY + 1, 0x665B4355);
                graphics.text(font, "Keybind", labelX,
                        dropY + scaled(15), WHITE, true);
                String keybind = "Not Bound";
                graphics.text(font, keybind,
                        rightX - font.width(keybind),
                        dropY + scaled(15), MUTED, true);
            }
        }

        // Preserve the Screen render lifecycle.
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );
    }
    // Left-click: toggle. Right-click: open/close module dropdown.
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        updateSlide();
        for (int category = 0; category < categories.length; category++) {
            if (event.button() == 0 && hovering(event.x(), event.y(),
                    guiX + scaled(6), scaled(38) + category * scaled(62),
                    scaled(36), scaled(36))) {
                selectedCategory = categories[category];
                expandedModule = -1;
                return true;
            }
        }
        for (int i = 0; i < modules.size(); i++) {
            if (!visible(i)) continue;
            if (hovering(event.x(), event.y(),
                    moduleX(), moduleY(i), moduleWidth(), moduleHeight())) {
                if (event.button() == 0) {
                    modules.get(i).toggle();
                    return true;
                }
                if (event.button() == 1) {
                    expandedModule = (expandedModule == i) ? -1 : i;
                    return true;
                }
            }

            if (expandedModule == i && hovering(event.x(), event.y(),
                    moduleX(), moduleY(i) + moduleHeight(),
                    moduleWidth(), dropdownHeight(i))) {
                if (event.button() == 0 && i == 0) {
                    int relativeY = (int) event.y() - moduleY(i) - moduleHeight();
                    if (relativeY < scaled(42)) {
                        Slots.setMuteMessages(!Slots.isMuteMessages());
                    } else if (relativeY < scaled(84)) {
                        Slots.setShowUi(!Slots.isShowUi());
                    }
                }
                // Keybind remains display-only.
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
