package net.lidl.lidlclient.hud;

/** Position is a fraction of available travel, so resizing cannot strand a panel. */
public final class PanelPlacement {
    private double x, y;
    public PanelPlacement(double x, double y) { this.x = fraction(x); this.y = fraction(y); }
    private static double fraction(double value) {
        return Double.isFinite(value) ? Math.clamp(value, 0, 1) : .5;
    }
    public double xFraction() { return x; }
    public double yFraction() { return y; }
    public int x(int viewport, int panel) { return (int)Math.round(x * Math.max(0, viewport-panel)); }
    public int y(int viewport, int panel) { return (int)Math.round(y * Math.max(0, viewport-panel)); }
    public void move(double left, double top, int width, int height, int panelWidth, int panelHeight) {
        // Preserve placement on an axis that currently has no room to move.
        if (width > panelWidth) x = fraction(left / (width-panelWidth));
        if (height > panelHeight) y = fraction(top / (height-panelHeight));
    }
}
