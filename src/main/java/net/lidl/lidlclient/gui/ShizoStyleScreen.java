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

    private static final int PINK = 0xFFFF43A5;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFFBDA8B5;

    // Transparent backgrounds
    private static final int MAIN_BG = 0x80100D12;
    private static final int SIDEBAR_BG = 0x44080808;
    private static final int CARD_BG = 0x66151015;
    private static final int ENABLED_BG = 0x8042162E;
    private static final int OUTER_GLOW = 0x223E142E;

    // ==============================
    // GUI SIZE
    // ==============================

    private static final int DESIGN_WIDTH = 1000;
    private static final int DESIGN_HEIGHT = 570;

    private static final float WIDTH_RATIO = 0.60f;
    private static final float HEIGHT_RATIO = 0.64f;

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
                "General",
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

        float targetWidth = width * WIDTH_RATIO;
        float targetHeight = height * HEIGHT_RATIO;

        guiScale = Math.min(
                targetWidth / DESIGN_WIDTH,
                targetHeight / DESIGN_HEIGHT
        );

        guiWidth = Math.round(DESIGN_WIDTH * guiScale);
        guiHeight = Math.round(DESIGN_HEIGHT * guiScale);

        guiX = (width - guiWidth) / 2;
        guiY = (height - guiHeight) / 2;
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
        thickness = Math.max(1, thickness);

        roundedRect(
                graphics,
                x, y,
                rectWidth, rectHeight,
                radius,
                borderColor
        );

        roundedRect(
                graphics,
                x + thickness,
                y + thickness,
                rectWidth - thickness * 2,
                rectHeight - thickness * 2,
                Math.max(0, radius - thickness),
                backgroundColor
        );
    }

    // ==============================
    // INITIALIZATION
    // ==============================

    @Override
    protected void init() {
        calculateGuiLayout();
    }

    // The GUI draws its own buttons instead of Minecraft's gray widgets.
    // Keep the original module card coordinates for rendering and clicks.
    private int moduleX() {
        return guiX + scaled(210);
    }

    private int dropdownHeight(int index) {
        // Slots has two settings plus the keybind row.
        return scaled(index == 0 ? 126 : 42);
    }

    private int moduleY(int index) {
        return guiY + scaled(110) + index * scaled(62)
                + (expandedModule >= 0 && expandedModule < index ? dropdownHeight(expandedModule) + scaled(7) : 0);
    }

    private int moduleWidth() {
        return scaled(765);
    }

    private int moduleHeight() {
        return scaled(48);
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
        int x = guiX;
        int y = guiY;
        int w = guiWidth;
        int h = guiHeight;

        int mainRadius = scaled(20);


        // Pink outer glow
        roundedRect(
                graphics,
                x - scaled(5),
                y - scaled(5),
                w + scaled(10),
                h + scaled(10),
                mainRadius + scaled(5),
                OUTER_GLOW
        );

        // Transparent rounded main window
        roundedBorder(
                graphics,
                x, y,
                w, h,
                mainRadius,
                Math.max(1, scaled(2)),
                0xFF9E286A,
                MAIN_BG
        );

        // Transparent sidebar
        roundedRect(
                graphics,
                x + scaled(2),
                y + scaled(2),
                scaled(190),
                h - scaled(4),
                Math.max(1, mainRadius - scaled(2)),
                SIDEBAR_BG
        );

        // Sidebar separator
        graphics.fill(
                x + scaled(190),
                y + scaled(20),
                x + scaled(191),
                y + h - scaled(20),
                0x99472039
        );

        // LidlClient title
        graphics.text(
                font,
                "LidlClient",
                x + scaled(20),
                y + scaled(24),
                PINK,
                true
        );

        // General heading
        graphics.text(
                font,
                "General",
                x + scaled(210),
                y + scaled(65),
                WHITE,
                true
        );

        // Divider under General
        graphics.fill(
                x + scaled(210),
                y + scaled(95),
                x + w - scaled(25),
                y + scaled(97),
                0xAA6A4659
        );

        // Custom General sidebar button, replacing the vanilla gray one.
        int sideX = x + scaled(12);
        int sideY = y + scaled(55);
        int sideW = scaled(170);
        int sideH = scaled(28);

        roundedRect(
                graphics,
                sideX, sideY,
                sideW, sideH,
                scaled(7),
                0x8042162E
        );

        graphics.fill(
                sideX + scaled(3),
                sideY + scaled(5),
                sideX + scaled(5),
                sideY + sideH - scaled(5),
                PINK
        );

        graphics.text(
                font,
                "General",
                sideX + scaled(17),
                sideY + (sideH - font.lineHeight) / 2,
                WHITE,
                true
        );

        // Original card positions, now with custom labels and toggle switches.
        for (int i = 0; i < modules.size(); i++) {

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

            // Draw the module and its dropdown as ONE continuous square panel.
            // Only the main GUI background has rounded corners.
            int border = Math.max(1, scaled(1));
            boolean expanded = expandedModule == i;
            int fullHeight = cardH + (expanded ? dropdownHeight(i) : 0);
            int panelBorder = module.enabled ? PINK
                    : (hovered ? 0xCCB35A8A : 0x8862515D);
            int panelBackground = module.enabled ? ENABLED_BG
                    : (hovered ? 0x99502A41 : CARD_BG);

            graphics.fill(cardX, cardY,
                    cardX + cardW, cardY + fullHeight, panelBorder);
            graphics.fill(cardX + border, cardY + border,
                    cardX + cardW - border, cardY + fullHeight - border,
                    panelBackground);

            int labelX = cardX + scaled(20);

            graphics.text(
                    font,
                    module.name,
                    labelX,
                    cardY + scaled(10),
                    WHITE,
                    true
            );

            graphics.text(
                    font,
                    module.description,
                    labelX,
                    cardY + scaled(27),
                    MUTED,
                    true
            );

            // Square ON/OFF switch matching the module cards.
            int switchW = scaled(90);
            int switchH = scaled(28);
            int switchX = cardX + cardW - switchW - scaled(14);
            int switchY = cardY + (cardH - switchH) / 2;

            graphics.fill(switchX, switchY,
                    switchX + switchW, switchY + switchH,
                    module.enabled ? 0xFFB92F79 : 0xAA302832);

            graphics.fill(switchX + scaled(2), switchY + scaled(2),
                    switchX + switchW - scaled(2), switchY + switchH - scaled(2),
                    module.enabled ? 0xFF8C245F : 0xAA201C25);

            String state = module.enabled ? "ON" : "OFF";

            graphics.text(
                    font,
                    state,
                    switchX + (switchW - font.width(state)) / 2,
                    switchY + (switchH - font.lineHeight) / 2,
                    module.enabled ? WHITE : MUTED,
                    true
            );

            // Expanded settings share the module's background colour.
            if (expanded) {
                int dropY = cardY + cardH;
                int rowH = scaled(42);
                int rightX = cardX + cardW - scaled(18);

                if (i == 0) {
                    graphics.fill(cardX + scaled(15), dropY,
                            cardX + cardW - scaled(15), dropY + 1, 0x665B4355);
                    graphics.text(font, "Mute chat messages", labelX,
                            dropY + scaled(15), WHITE, true);
                    String muted = Slots.isMuteMessages() ? "ON" : "OFF";
                    graphics.text(font, muted, rightX - font.width(muted),
                            dropY + scaled(15), Slots.isMuteMessages() ? PINK : MUTED, true);

                    dropY += rowH;
                    graphics.fill(cardX + scaled(15), dropY,
                            cardX + cardW - scaled(15), dropY + 1, 0x665B4355);
                    graphics.text(font, "Show bet / balance text", labelX,
                            dropY + scaled(15), WHITE, true);
                    String show = Slots.isShowUi() ? "ON" : "OFF";
                    graphics.text(font, show, rightX - font.width(show),
                            dropY + scaled(15), Slots.isShowUi() ? PINK : MUTED, true);
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
        for (int i = 0; i < modules.size(); i++) {
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
