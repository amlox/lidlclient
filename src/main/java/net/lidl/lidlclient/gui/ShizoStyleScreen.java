
package net.lidl.lidlclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
        rebuildGui();
    }

    private void rebuildGui() {
        clearWidgets();

        // General sidebar
        addRenderableWidget(
                Button.builder(
                        Component.literal("General"),
                        button -> {}
                ).bounds(
                        guiX + scaled(12),
                        guiY + scaled(55),
                        scaled(170),
                        scaled(28)
                ).build()
        );

        // Module positions
        int moduleX = guiX + scaled(210);
        int moduleY = guiY + scaled(110);

        int moduleWidth = scaled(765);
        int moduleHeight = scaled(48);
        int moduleSpacing = scaled(62);

        // Create module buttons
        for (int i = 0; i < modules.size(); i++) {

            GuiModule module = modules.get(i);

            int cardY = moduleY + i * moduleSpacing;

            addRenderableWidget(
                    Button.builder(
                            Component.literal(
                                    (module.enabled ? "ON  " : "OFF  ")
                                            + module.name
                            ),
                            button -> {
                                module.toggle();
                                rebuildGui();
                            }
                    ).bounds(
                            moduleX,
                            cardY,
                            moduleWidth,
                            moduleHeight
                    ).build()
            );
        }
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
        int cardRadius = scaled(10);

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

        // Rounded module backgrounds
        int moduleX = x + scaled(210);
        int moduleY = y + scaled(110);
        int moduleWidth = scaled(765);
        int moduleHeight = scaled(48);

        for (int i = 0; i < modules.size(); i++) {

            GuiModule module = modules.get(i);

            int cardY = moduleY + i * scaled(62);

            roundedBorder(
                    graphics,
                    moduleX,
                    cardY,
                    moduleWidth,
                    moduleHeight,
                    cardRadius,
                    Math.max(1, scaled(1)),
                    module.enabled
                            ? PINK
                            : 0xAA62515D,
                    module.enabled
                            ? ENABLED_BG
                            : CARD_BG
            );
        }

        // Render interactive buttons
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );
    }
}
