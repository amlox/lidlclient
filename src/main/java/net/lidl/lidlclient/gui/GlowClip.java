package net.lidl.lidlclient.gui;

/** Leave space for a fully visible button's halo without exposing scrolled content. */
public final class GlowClip {
    private GlowClip() { }
    public static int top(int viewportTop, double itemTop, int margin) {
        return itemTop >= viewportTop ? Math.max(0, viewportTop - Math.max(0, margin)) : viewportTop;
    }
}
