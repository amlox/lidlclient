package net.lidl.lidlclient.gui;

import java.util.List;

/** Shared by rendering and hit testing; never stores module indices as identities. */
public final class ModuleLayout {
    private ModuleLayout() { }

    public record Row(GuiModule module, int top, int height, int headerHeight, int settingHeight) {
        public int bottom() { return top + height; }
        public boolean expanded() { return height > headerHeight; }
    }

    public static List<String> categories(List<GuiModule> modules) {
        return modules.stream().map(GuiModule::category).distinct().toList();
    }

    public static List<Row> rows(List<GuiModule> modules, String category, String expandedId,
                                 int headerHeight, int settingHeight, int gap) {
        if (headerHeight <= 0 || settingHeight <= 0 || gap < 0) {
            throw new IllegalArgumentException("Invalid layout dimensions");
        }
        var rows = new java.util.ArrayList<Row>();
        int top = 0;
        for (GuiModule module : modules) {
            if (!module.category().equals(category)) continue;
            int height = headerHeight + (module.id().equals(expandedId)
                    ? module.settings().size() * settingHeight : 0);
            rows.add(new Row(module, top, height, headerHeight, settingHeight));
            top += height + gap;
        }
        return List.copyOf(rows);
    }

    public static int contentHeight(List<Row> rows) {
        return rows.isEmpty() ? 0 : rows.getLast().bottom();
    }

    public static double clampScroll(double scroll, int contentHeight, int viewportHeight) {
        if (!Double.isFinite(scroll)) return 0;
        return Math.clamp(scroll, 0, Math.max(0, contentHeight - Math.max(0, viewportHeight)));
    }
}
