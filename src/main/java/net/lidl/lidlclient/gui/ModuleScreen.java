package net.lidl.lidlclient.gui;

import net.lidl.lidlclient.gui.render.SmoothRenderer;
import net.lidl.lidlclient.gui.render.ConstellationRenderer;
import net.lidl.lidlclient.gui.render.UiText;
import net.lidl.lidlclient.gui.settings.BooleanSetting;
import net.lidl.lidlclient.gui.settings.ModuleSetting;
import net.lidl.lidlclient.gui.settings.NumberSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Layout and widgets depend only on module declarations, never feature names or indices. */
public class ModuleScreen extends Screen {
    private static final long SLIDE_DURATION_NS = 500_000_000L;
    private static final int PANEL = 0x9805070D;
    private static final int WHITE = 0xFFF4F4FA;
    private static final int MUTED = 0xFFABAAB5;
    private static final int HEADER = 38;
    private static final int SETTING = 34;
    private static final int GAP = 10;
    private static final int TOP = 40;
    private static final int BOTTOM = 14;
    private static final int RAIL = 48;
    private static final int CATEGORY_PITCH = 46;

    private final List<GuiModule> modules;
    private final List<String> categories;
    private final long openedAt = System.nanoTime();
    private String selectedCategory;
    private String expandedId;
    private float panelX;
    private int panelWidth;
    private double scroll;
    private double categoryScroll;
    private NumberSetting draggingNumber;
    private boolean draggingScrollbar;
    private double scrollbarGrab;
    private double starOffset;
    private long lastFrame = System.nanoTime();
    private String layoutCategory;
    private String layoutExpandedId;
    private List<ModuleLayout.Row> layoutRows = List.of();

    public ModuleScreen() { this(ModuleRegistry.modules()); }

    /** Also useful for custom registries and stress-testing large menus. */
    public ModuleScreen(List<GuiModule> modules) {
        super(Component.literal("LidlClient"));
        this.modules = ModuleRegistry.validate(modules);
        this.categories = ModuleLayout.categories(this.modules);
        this.selectedCategory = categories.isEmpty() ? "" : categories.getFirst();
    }

    @Override protected void init() {
        draggingNumber = null;
        draggingScrollbar = false;
        panelWidth = Math.min(width, Math.max(220, Math.min(300, Math.round(width * 0.32f))));
        clampScroll();
        updateSlide();
    }

    private void updateSlide() {
        double t = Math.clamp((System.nanoTime() - openedAt) / (double) SLIDE_DURATION_NS, 0, 1);
        panelX = (float) (-(panelWidth + 12) * Math.pow(1 - t, 3));
    }

    private List<ModuleLayout.Row> rows() {
        if (!java.util.Objects.equals(layoutCategory, selectedCategory)
                || !java.util.Objects.equals(layoutExpandedId, expandedId)) {
            layoutRows = ModuleLayout.rows(modules, selectedCategory, expandedId, HEADER, SETTING, GAP);
            layoutCategory = selectedCategory;
            layoutExpandedId = expandedId;
        }
        return layoutRows;
    }

    private int viewportHeight() { return Math.max(0, height - TOP - BOTTOM); }
    private int cardX() { return Math.round(panelX) + RAIL + 10; }
    private int cardWidth() { return Math.max(1, panelWidth - RAIL - 28); }
    private int categoryHeight() { return Math.max(0, categories.size() * CATEGORY_PITCH - 10); }

    private void clampScroll() {
        scroll = ModuleLayout.clampScroll(scroll, ModuleLayout.contentHeight(rows()), viewportHeight());
        categoryScroll = ModuleLayout.clampScroll(categoryScroll, categoryHeight(), viewportHeight());
    }

    private ScreenRectangle contentClip() {
        return new ScreenRectangle(Math.max(0, Math.round(panelX) + RAIL + 3), TOP,
                Math.max(0, Math.min(width, Math.round(panelX) + panelWidth - 8)
                        - Math.max(0, Math.round(panelX) + RAIL + 3)), viewportHeight());
    }

    private ScreenRectangle railClip() {
        int left = Math.max(0, Math.round(panelX));
        return new ScreenRectangle(left, TOP,
                Math.max(0, Math.min(width, Math.round(panelX) + RAIL) - left), viewportHeight());
    }

    private boolean contains(double x, double y, double left, double top, double w, double h) {
        return x >= left && x < left + w && y >= top && y < top + h;
    }

    private void text(GuiGraphicsExtractor graphics, String value, int x, int y, int maxWidth, int color) {
        UiText.draw(graphics, font, value, x, y, maxWidth, color);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        updateSlide();
        clampScroll();
        long now = System.nanoTime();
        double elapsed = Math.min(.1, (now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
        starOffset += (scroll * .14 - starOffset) * (1 - Math.exp(-elapsed * 12));
        graphics.fill(Math.round(panelX), 0, Math.round(panelX) + panelWidth, height, PANEL);
        int backgroundLeft = Math.max(0, Math.round(panelX));
        int backgroundRight = Math.max(backgroundLeft, Math.min(width, Math.round(panelX) + panelWidth - 9));
        ConstellationRenderer.background(graphics, panelX, panelWidth - 9, height, starOffset,
                new ScreenRectangle(backgroundLeft, 0, backgroundRight - backgroundLeft, height));
        graphics.fill(Math.round(panelX), 0, Math.round(panelX) + RAIL, height, 0x30000000);
        SmoothRenderer.line(graphics, panelX + RAIL, 0, height, 0.65f, 0x66FFFFFF, 0, null);
        SmoothRenderer.line(graphics, panelX + panelWidth - 5, 0, height, 0.9f, WHITE, 6, null);
        SmoothRenderer.line(graphics, panelX + panelWidth - 8, 0, height, 0.5f, 0x80FFFFFF, 0, null);
        text(graphics, selectedCategory.isEmpty() ? "LidlClient" : selectedCategory,
                cardX(), 16, cardWidth(), WHITE);

        ScreenRectangle rail = railClip();
        graphics.enableScissor(rail.left(), TOP, rail.right(), TOP + viewportHeight());
        for (int i = 0; i < categories.size(); i++) {
            float y = TOP + i * CATEGORY_PITCH - (float) categoryScroll;
            if (y + 36 < TOP || y > height - BOTTOM) continue;
            String category = categories.get(i);
            boolean active = category.equals(selectedCategory);
            boolean hovered = contains(mouseX, mouseY, panelX + 7, y, 34, 36);
            SmoothRenderer.panel(graphics, panelX + 7, y, 34, 36, 7,
                    active ? 0x405D5D70 : 0x200C0C12,
                    active || hovered ? WHITE : 0x668D8D9A, 0.8f, active || hovered ? 3 : 0, rail);
            String letter = category.substring(0, category.offsetByCodePoints(0, 1));
            text(graphics, letter, Math.round(panelX + 24 - UiText.width(font, letter) / 2f),
                    Math.round(y + (36 - UiText.HEIGHT) / 2f), 24, active ? WHITE : MUTED);
        }
        graphics.disableScissor();

        ScreenRectangle clip = contentClip();
        graphics.enableScissor(clip.left(), TOP, clip.right(), TOP + viewportHeight());
        for (ModuleLayout.Row row : rows()) {
            float y = TOP + row.top() - (float) scroll;
            if (y + row.height() < TOP || y > height - BOTTOM) continue;
            GuiModule module = row.module();
            boolean enabled = module.isEnabled();
            boolean hovered = contains(mouseX, mouseY, cardX(), y, cardWidth(), HEADER);
            SmoothRenderer.panel(graphics, cardX(), y, cardWidth(), row.height(), 8,
                    enabled ? 0x304F4F60 : 0x2007070D,
                    enabled || hovered ? WHITE : 0x667F7F8C, 0.8f, enabled || hovered ? 4 : 0, clip);
            text(graphics, module.name(), cardX() + 12, Math.round(y + (HEADER - UiText.HEIGHT) / 2f),
                    cardWidth() - 34, enabled ? WHITE : MUTED);
            // State marker stays visible independently of hover or the text colour.
            SmoothRenderer.panel(graphics, cardX() + cardWidth() - 17, y + HEADER / 2f - 2,
                    4, 4, 2, enabled ? WHITE : 0x407F7F8C, 0, 0, 0, clip);
            if (!row.expanded()) continue;
            for (int index = 0; index < module.settings().size(); index++) {
                float sy = y + HEADER + index * SETTING;
                if (sy + SETTING < TOP || sy > height - BOTTOM) continue;
                renderSetting(graphics, module.settings().get(index), sy, clip);
            }
        }
        graphics.disableScissor();

        renderScrollbars(graphics);
        if (modules.isEmpty()) text(graphics, "No modules registered", cardX(), TOP + 8, cardWidth(), MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderSetting(GuiGraphicsExtractor graphics, ModuleSetting setting, float y, ScreenRectangle clip) {
        SmoothRenderer.panel(graphics, cardX() + 10, y, cardWidth() - 20, 0.6f,
                0, 0x207F7F8C, 0, 0, 0, clip);
        String value = setting.valueText();
        int valueWidth = Math.min(UiText.width(font, value), Math.max(24, cardWidth() / 3));
        int right = cardX() + cardWidth() - 12;
        text(graphics, setting.label(), cardX() + 12, Math.round(y + 9),
                cardWidth() - valueWidth - 32, WHITE);
        text(graphics, value, right - valueWidth, Math.round(y + 9), valueWidth,
                setting instanceof BooleanSetting bool && bool.value() ? WHITE : MUTED);
        if (setting instanceof NumberSetting number) {
            int trackWidth = cardWidth() - 24;
            SmoothRenderer.panel(graphics, cardX() + 12, y + 26, trackWidth, 2, 1,
                    0x407F7F8C, 0, 0, 0, clip);
            SmoothRenderer.panel(graphics, cardX() + 12, y + 26,
                    Math.max(1, trackWidth * (float) number.fraction()), 2, 1,
                    WHITE, 0, 0, 0, clip);
        }
    }

    private int contentHeight() { return ModuleLayout.contentHeight(rows()); }
    private int thumbHeight(int contentHeight) {
        return Math.min(viewportHeight(), Math.max(16, viewportHeight() * viewportHeight() / Math.max(1, contentHeight)));
    }
    private float thumbY(int contentHeight, double offset) {
        return TOP + (float) (offset / Math.max(1, contentHeight - viewportHeight())
                * (viewportHeight() - thumbHeight(contentHeight)));
    }

    private void renderScrollbars(GuiGraphicsExtractor graphics) {
        ConstellationRenderer.star(graphics, panelX + panelWidth - 5, indicatorY(),
                9, contentHeight() > viewportHeight() ? 1 : .68f, null);
        if (categoryHeight() > viewportHeight()) {
            SmoothRenderer.panel(graphics, panelX + RAIL - 4, thumbY(categoryHeight(), categoryScroll),
                    1, thumbHeight(categoryHeight()), 0.5f, 0x66FFFFFF, 0, 0, 0, null);
        }
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        updateSlide();
        clampScroll();
        // The star straddles the edge, so its complete hit area is tested first.
        if (event.button() == 0 && contentHeight() > viewportHeight()
                && contains(event.x(), event.y(), panelX + panelWidth - 16, TOP, 23, viewportHeight())) {
            draggingScrollbar = true;
            scrollbarGrab = Math.abs(event.y() - indicatorY()) <= 10 ? event.y() - indicatorY() : 0;
            dragScrollbar(event.y());
            return true;
        }
        if (!contains(event.x(), event.y(), panelX, TOP, panelWidth, viewportHeight())) {
            return super.mouseClicked(event, doubleClick);
        }
        if (event.button() == 0 && event.x() < panelX + RAIL) {
            for (int i = 0; i < categories.size(); i++) {
                if (contains(event.x(), event.y(), panelX + 7,
                        TOP + i * CATEGORY_PITCH - categoryScroll, 34, 36)) {
                    selectedCategory = categories.get(i);
                    expandedId = null;
                    scroll = 0;
                    draggingNumber = null;
                    return true;
                }
            }
            return true;
        }
        for (ModuleLayout.Row row : rows()) {
            double y = TOP + row.top() - scroll;
            if (contains(event.x(), event.y(), cardX(), y, cardWidth(), HEADER)) {
                if (event.button() == 0) row.module().toggle();
                else if (event.button() == 1 && !row.module().settings().isEmpty()) {
                    expandedId = row.module().id().equals(expandedId) ? null : row.module().id();
                    clampScroll();
                }
                return true;
            }
            if (row.expanded() && contains(event.x(), event.y(), cardX(), y + HEADER,
                    cardWidth(), row.height() - HEADER)) {
                int index = (int) ((event.y() - y - HEADER) / SETTING);
                ModuleSetting setting = row.module().settings().get(index);
                if (setting instanceof NumberSetting number && event.button() == 0) {
                    if (event.y() - y - HEADER - index * SETTING >= 21) {
                        draggingNumber = number;
                        dragNumber(event.x());
                    } else number.change(1);
                } else if (event.button() == 0 || event.button() == 1) {
                    setting.change(event.button() == 0 ? 1 : -1);
                }
                return true;
            }
        }
        return true;
    }

    private void dragNumber(double mouseX) {
        draggingNumber.setFraction((mouseX - cardX() - 12) / Math.max(1, cardWidth() - 24));
    }

    private double indicatorTop() { return TOP + Math.min(10, viewportHeight() / 2.0); }
    private double indicatorBottom() { return Math.max(indicatorTop(), height - BOTTOM - 10); }
    private float indicatorY() {
        return (float) ScrollIndicator.position(scroll, contentHeight() - viewportHeight(),
                indicatorTop(), indicatorBottom());
    }
    private void dragScrollbar(double mouseY) {
        scroll = ScrollIndicator.scroll(mouseY - scrollbarGrab, contentHeight() - viewportHeight(),
                indicatorTop(), indicatorBottom());
        clampScroll();
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() == 0 && draggingNumber != null) { dragNumber(event.x()); return true; }
        if (event.button() == 0 && draggingScrollbar) { dragScrollbar(event.y()); return true; }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        boolean dragging = draggingNumber != null || draggingScrollbar;
        draggingNumber = null;
        draggingScrollbar = false;
        return dragging || super.mouseReleased(event);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        updateSlide();
        if (!contains(mouseX, mouseY, panelX, TOP, panelWidth + 7, viewportHeight())) return false;
        if (mouseX < panelX + RAIL) categoryScroll -= vertical * CATEGORY_PITCH;
        else scroll -= vertical * 30;
        draggingNumber = null;
        clampScroll();
        return true;
    }
}
