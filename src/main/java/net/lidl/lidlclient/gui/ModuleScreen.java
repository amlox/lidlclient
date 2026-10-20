package net.lidl.lidlclient.gui;

import net.lidl.lidlclient.gui.render.SmoothRenderer;
import net.lidl.lidlclient.gui.render.CategoryIcons;
import net.lidl.lidlclient.gui.render.ConstellationRenderer;
import net.lidl.lidlclient.gui.render.UiText;
import net.lidl.lidlclient.gui.settings.BooleanSetting;
import net.lidl.lidlclient.gui.settings.KeybindSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import net.lidl.lidlclient.gui.settings.ActionSetting;
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
    private long categoryOpenedAt = openedAt;
    private float backgroundX;
    private double ambientOffset;
    private String selectedCategory;
    private String expandedId;
    private float panelX;
    private int panelWidth;
    private double scroll;
    private double categoryScroll;
    private NumberSetting draggingNumber;
    private KeybindSetting bindingKey;
    private boolean draggingScrollbar;
    private double scrollbarGrab;
    private double starOffset;
    private long lastFrame = System.nanoTime();
    private String layoutCategory;
    private String layoutExpandedId;
    private List<ModuleLayout.Row> layoutRows = List.of();

    public ModuleScreen() { this(ModuleRegistry.modules(), ModuleRegistry.CATEGORIES); }

    /** Also useful for custom registries and stress-testing large menus. */
    public ModuleScreen(List<GuiModule> modules) {
        this(modules, ModuleLayout.categories(modules));
    }

    private ModuleScreen(List<GuiModule> modules, List<String> categories) {
        super(Component.literal("LidlClient"));
        this.modules = ModuleRegistry.validate(modules);
        this.categories = List.copyOf(categories);
        this.selectedCategory = categories.isEmpty() ? "" : categories.getFirst();
    }

    @Override protected void init() {
        bindingKey = null;
        draggingNumber = null;
        draggingScrollbar = false;
        panelWidth = Math.min(width, Math.max(220, Math.min(300, Math.round(width * 0.32f))));
        clampScroll();
        updateSlide();
    }

    private void updateSlide() {
        double t = Math.clamp((System.nanoTime() - openedAt) / (double) SLIDE_DURATION_NS, 0, 1);
        panelX = (float) (-(panelWidth + 12) * Math.pow(1 - t, 3));
        double backgroundT = Math.clamp((System.nanoTime() - openedAt - 35_000_000L)
                / (double) SLIDE_DURATION_NS, 0, 1);
        backgroundX = (float) (-(panelWidth + 12) * Math.pow(1 - backgroundT, 3));
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
    private int animatedCardX(int index) {
        // Cubic ease-out is about 9% complete when the next card starts (15 / 480 ms).
        double t = Math.clamp((System.nanoTime() - categoryOpenedAt - index * 15_000_000L)
                / 480_000_000.0, 0, 1);
        return cardX() - (int) Math.round((cardWidth() + 16) * Math.pow(1 - t, 3));
    }

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

    private ScreenRectangle glowClip(ScreenRectangle clip, float itemTop) {
        int top = GlowClip.top(clip.top(), itemTop, 12);
        return new ScreenRectangle(clip.left(), top, clip.width(), clip.bottom() - top);
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
        ambientOffset += elapsed * 2.5; // Gentle downward drift, independent of scroll parallax.
        graphics.fill(Math.round(backgroundX), 0, Math.round(backgroundX) + panelWidth, height, GuiAppearance.background());
        int backgroundLeft = Math.max(0, Math.round(backgroundX));
        int backgroundRight = Math.max(backgroundLeft, Math.min(width, Math.round(backgroundX) + panelWidth - 9));
        if (GuiAppearance.stars()) ConstellationRenderer.background(graphics, backgroundX, panelWidth - 9, height,
                starOffset - ambientOffset, (now - openedAt) / 1_000_000_000.0,
                new ScreenRectangle(backgroundLeft, 0, backgroundRight - backgroundLeft, height));
        graphics.fill(Math.round(panelX), 0, Math.round(panelX) + RAIL, height, 0x30000000);
        SmoothRenderer.line(graphics, panelX + RAIL, 0, height, 0.65f, 0x66FFFFFF, 0, null);
        SmoothRenderer.line(graphics, panelX + panelWidth - 5, 0, height, 0.9f, GuiAppearance.accent(), 6, null);
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
                    GuiAppearance.tint(active ? 0xBC344055 : (hovered ? 0xA0283243 : 0x8A10151F)),
                    active || hovered ? GuiAppearance.accent() : GuiAppearance.tint(0xA5798598), 1,
                    active ? 7 : (hovered ? 5 : 1), active ? .42f : .24f, glowClip(rail, y));
            CategoryIcons.draw(graphics, category, panelX + 24, y + 18,
                    active ? WHITE : MUTED, rail);
        }
        graphics.disableScissor();

        ScreenRectangle clip = contentClip();
        graphics.enableScissor(clip.left(), TOP, clip.right(), TOP + viewportHeight());
        for (int rowIndex = 0; rowIndex < rows().size(); rowIndex++) {
            ModuleLayout.Row row = rows().get(rowIndex);
            int x = animatedCardX(rowIndex);
            float y = TOP + row.top() - (float) scroll;
            if (y + row.height() < TOP || y > height - BOTTOM) continue;
            GuiModule module = row.module();
            boolean enabled = module.isEnabled();
            boolean hovered = contains(mouseX, mouseY, x, y, cardWidth(), HEADER);
            SmoothRenderer.panel(graphics, x, y, cardWidth(), row.height(), 8,
                    GuiAppearance.tint(enabled ? 0xC0364258 : (hovered ? 0xB4263042 : 0x94101520)),
                    enabled ? GuiAppearance.accent() : GuiAppearance.tint(hovered ? 0xFFE0E8F7 : 0xA5798598),
                    enabled ? 1.3f : .9f, enabled ? 10 : (hovered ? 6 : 1.5f),
                    enabled ? .55f : (hovered ? .32f : .14f), glowClip(clip, y));
            if (enabled) {
                SmoothRenderer.panel(graphics, x + 3, y + 10, 2, HEADER - 20, 1,
                        GuiAppearance.accent(), GuiAppearance.accent(), 0, 3, .38f, clip);
            }
            text(graphics, module.name(), x + 12, Math.round(y + (HEADER - UiText.HEIGHT) / 2f + 1),
                    cardWidth() - 24, enabled ? WHITE : 0xFFD6DAE4);
            if (!row.expanded()) continue;
            for (int index = 0; index < module.settings().size(); index++) {
                float sy = y + HEADER + index * SETTING;
                if (sy + SETTING < TOP || sy > height - BOTTOM) continue;
                renderSetting(graphics, module.settings().get(index), sy, mouseX, mouseY, clip, x);
            }
        }
        graphics.disableScissor();

        renderScrollbars(graphics);
        if (modules.isEmpty()) text(graphics, "No modules registered", cardX(), TOP + 8, cardWidth(), MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderSetting(GuiGraphicsExtractor graphics, ModuleSetting setting, float y,
                               int mouseX, int mouseY, ScreenRectangle clip, int x) {
        if (setting instanceof ActionSetting action) {
            boolean hovered = contains(mouseX, mouseY, x + 6, y + 3, cardWidth() - 12, SETTING - 6);
            SmoothRenderer.panel(graphics, x + 6, y + 3, cardWidth() - 12, SETTING - 6, 5,
                    GuiAppearance.tint(hovered ? 0xB4425573 : 0xA02B374B), hovered ? GuiAppearance.accent() : GuiAppearance.tint(0x907F90AB),
                    .9f, hovered ? 3 : 1, .24f, clip);
            int available = cardWidth() - 24;
            int labelWidth = Math.min(available, UiText.width(font, action.label()));
            text(graphics, action.label(), x + (cardWidth() - labelWidth) / 2,
                    Math.round(y + (SETTING - UiText.HEIGHT) / 2f), available, WHITE);
            return;
        }
        boolean on = setting instanceof BooleanSetting bool && bool.value();
        boolean hovered = contains(mouseX, mouseY, x, y, cardWidth(), SETTING);
        SmoothRenderer.panel(graphics, x + 6, y + 3, cardWidth() - 12, SETTING - 6, 5,
                GuiAppearance.tint(on ? 0x883E4D67 : (hovered ? 0x782B374B : 0x50080D17)),
                GuiAppearance.tint(on ? 0xAACFDDF5 : (hovered ? 0x907F90AB : 0x303F4D66)),
                .7f, on ? 3 : (hovered ? 2 : 0), on ? .3f : .18f, clip);
        String value = setting == bindingKey ? "Press key..." : setting.valueText();
        int valueWidth = Math.min(UiText.width(font, value), Math.max(24, cardWidth() / 3));
        int right = x + cardWidth() - 12;
        text(graphics, setting.label(), x + 12, Math.round(y + 9),
                cardWidth() - valueWidth - 32, WHITE);
        text(graphics, value, right - valueWidth, Math.round(y + 9), valueWidth,
                on ? WHITE : MUTED);
        if (setting instanceof NumberSetting number) {
            int trackWidth = cardWidth() - 24;
            SmoothRenderer.panel(graphics, x + 12, y + 26, trackWidth, 2, 1,
                    0x407F7F8C, 0, 0, 0, clip);
            SmoothRenderer.panel(graphics, x + 12, y + 26,
                    Math.max(1, trackWidth * (float) number.fraction()), 2, 1,
                    GuiAppearance.accent(), 0, 0, 0, clip);
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
        if (bindingKey != null) {
            bindingKey.bind(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            bindingKey = null;
            return true;
        }
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
                    if (selectedCategory.equals(categories.get(i))) return true;
                    selectedCategory = categories.get(i);
                    categoryOpenedAt = System.nanoTime();
                    draggingScrollbar = false;
                    expandedId = null;
                    scroll = 0;
                    draggingNumber = null;
                    return true;
                }
            }
            return true;
        }
        if (event.x() < contentClip().left() || event.x() >= contentClip().right()) return true;
        for (int rowIndex = 0; rowIndex < rows().size(); rowIndex++) {
            ModuleLayout.Row row = rows().get(rowIndex);
            int x = animatedCardX(rowIndex);
            double y = TOP + row.top() - scroll;
            if (contains(event.x(), event.y(), x, y, cardWidth(), HEADER)) {
                if (event.button() == 0) row.module().toggle();
                else if (event.button() == 1 && !row.module().settings().isEmpty()) {
                    expandedId = row.module().id().equals(expandedId) ? null : row.module().id();
                    clampScroll();
                }
                return true;
            }
            if (row.expanded() && contains(event.x(), event.y(), x, y + HEADER,
                    cardWidth(), row.height() - HEADER)) {
                int index = (int) ((event.y() - y - HEADER) / SETTING);
                ModuleSetting setting = row.module().settings().get(index);
                if (setting instanceof KeybindSetting keybind) {
                    draggingNumber = null;
                    if (event.button() == 0) bindingKey = keybind;
                    else if (event.button() == 1) keybind.bind(InputConstants.UNKNOWN);
                }
                if (setting instanceof ActionSetting action) {
                    if (event.button() == 0 && contains(event.x(), event.y(),
                            x + 6, y + HEADER + index * SETTING + 3,
                            cardWidth() - 12, SETTING - 6)) {
                        draggingNumber = null;
                        action.activate();
                    }
                } else if (setting instanceof NumberSetting number && event.button() == 0) {
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
        for (int i = 0; i < rows().size(); i++) {
            if (rows().get(i).expanded()) {
                draggingNumber.setFraction((mouseX - animatedCardX(i) - 12) / Math.max(1, cardWidth() - 24));
                return;
            }
        }
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

    @Override public boolean keyPressed(KeyEvent event) {
        if (bindingKey == null) return super.keyPressed(event);
        if (event.isEscape()) {
            bindingKey = null;
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE || event.key() == GLFW.GLFW_KEY_DELETE) {
            bindingKey.bind(InputConstants.UNKNOWN);
        } else if (event.key() != GLFW.GLFW_KEY_UNKNOWN) {
            bindingKey.bind(InputConstants.Type.KEYSYM.getOrCreate(event.key()));
        }
        bindingKey = null;
        return true;
    }
}
