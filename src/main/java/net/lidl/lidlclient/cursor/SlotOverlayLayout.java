package net.lidl.lidlclient.cursor;

/** Vanilla ContainerScreen/ChestMenu geometry in GUI units, not framebuffer pixels. */
public final class SlotOverlayLayout {
    public static final int SIZE = 16;
    public static final int PITCH = 18;
    private SlotOverlayLayout() { }
    public static int x(int guiWidth,int slot) {
        return (guiWidth-176)/2 + 8 + (slot%9)*PITCH;
    }
    public static int y(int guiHeight,int rows,int slot) {
        return (guiHeight-(114+rows*PITCH))/2 + 18 + (slot/9)*PITCH;
    }
}
