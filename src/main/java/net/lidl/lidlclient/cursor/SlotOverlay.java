package net.lidl.lidlclient.cursor;

import java.util.LinkedHashMap;
import java.util.Map;

/** Per-hotkey chest layout and RGB colours, with immutable slot selections. */
public record SlotOverlay(int rows, Map<Integer,Integer> slots) {
    public static final int DEFAULT_COLOUR = 0x55D9FF;
    public SlotOverlay {
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("Chest rows must be 1–6");
        Map<Integer,Integer> clean = new LinkedHashMap<>();
        slots.forEach((slot,colour) -> {
            if (slot != null && colour != null && slot >= 0 && slot < 54) clean.put(slot,colour & 0xFFFFFF);
        });
        slots = Map.copyOf(clean);
    }
    public static SlotOverlay empty() { return new SlotOverlay(6,Map.of()); }
    public SlotOverlay withRows(int next) { return new SlotOverlay(next,slots); }
    public SlotOverlay withSlot(int slot, Integer colour) {
        Map<Integer,Integer> next = new LinkedHashMap<>(slots);
        if (colour == null) next.remove(slot); else next.put(slot,colour);
        return new SlotOverlay(rows,next);
    }
    public static int translucent(int rgb, double opacityPercent) {
        double opacity = Double.isFinite(opacityPercent) ? Math.clamp(opacityPercent,5,80) : 30;
        return ((int)Math.round(opacity * 255 / 100) << 24) | (rgb & 0xFFFFFF);
    }
}
