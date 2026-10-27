package net.lidl.lidlclient.cursor;

/** Shared picker/editor grid: menu first, fixed inventory slots, then a separated hotbar. */
public record SlotEditorLayout(int left, int top, int pitch, int rows) {
    public static SlotEditorLayout forScreen(int width,int height,int rows) {
        int pitch=Math.min(18,Math.max(4,(height-154)/(rows+4)));
        return new SlotEditorLayout((width-9*pitch)/2,104,pitch,rows);
    }
    public int size() { return pitch-2; }
    public int x(int slot) { return left+(slot%9)*pitch; }
    public int y(boolean inventory,int slot) {
        return top+(inventory?rows*pitch+16:0)+(slot/9)*pitch+(inventory&&slot>=27?4:0);
    }
    /** Return the saved icon position (54–89 for inventory), or -1 for gaps/outside. */
    public int iconSlotAt(double x,double y) {
        for (int i=0;i<rows*9+36;i++) {
            boolean inventory=i>=rows*9;
            int slot=inventory?i-rows*9:i;
            int sx=x(slot),sy=y(inventory,slot);
            if (x>=sx && x<sx+size() && y>=sy && y<sy+size()) return inventory?54+slot:slot;
        }
        return -1;
    }
    /** Screen inventory is main inventory first and hotbar last; Minecraft stores hotbar first. */
    public static int inventoryIndex(int displaySlot) {
        if (displaySlot<0 || displaySlot>=36) throw new IllegalArgumentException("Inventory slot must be 0–35");
        return displaySlot<27?displaySlot+9:displaySlot-27;
    }
}
