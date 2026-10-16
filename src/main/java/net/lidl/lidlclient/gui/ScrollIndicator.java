package net.lidl.lidlclient.gui;

/** Shared scroll-to-star mapping for rendering, rail clicks and dragging. */
public final class ScrollIndicator {
    private ScrollIndicator() { }
    public static double position(double scroll, double maxScroll, double top, double bottom) {
        double fraction = maxScroll <= 0 ? 0 : Math.clamp(scroll / maxScroll, 0, 1);
        return top + Math.max(0, bottom - top) * fraction;
    }
    public static double scroll(double y, double maxScroll, double top, double bottom) {
        if (bottom <= top || maxScroll <= 0) return 0;
        return Math.clamp((y - top) / (bottom - top), 0, 1) * maxScroll;
    }
}
