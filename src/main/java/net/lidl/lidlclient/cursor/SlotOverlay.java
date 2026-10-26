package net.lidl.lidlclient.cursor;

import java.util.LinkedHashMap;
import java.util.Map;

/** Inventory positions are independent of chest size: 0–26 main inventory, 27–35 hotbar. */
public record SlotOverlay(int rows, Map<Integer,Integer> slots, Map<Integer,Integer> inventorySlots,
                          Map<Integer,String> icons) {
    public static final int DEFAULT_COLOUR = 0x55D9FF;
    public SlotOverlay(int rows,Map<Integer,Integer> slots) { this(rows,slots,Map.of(),Map.of()); }
    public SlotOverlay {
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("Chest rows must be 1–6");
        slots = colours(slots,54);
        inventorySlots = colours(inventorySlots,36);
        Map<Integer,String> clean = new LinkedHashMap<>();
        icons.forEach((slot,icon) -> {
            if (slot != null && slot >= 0 && slot < 90 && icon != null && icon.length() <= 65536)
                clean.put(slot,icon);
        });
        icons = Map.copyOf(clean);
    }
    private static Map<Integer,Integer> colours(Map<Integer,Integer> source,int limit) {
        Map<Integer,Integer> clean = new LinkedHashMap<>();
        source.forEach((slot,colour) -> {
            if (slot != null && colour != null && slot >= 0 && slot < limit) clean.put(slot,colour & 0xFFFFFF);
        });
        return Map.copyOf(clean);
    }
    public static SlotOverlay empty() { return new SlotOverlay(6,Map.of()); }
    public SlotOverlay withRows(int next) { return new SlotOverlay(next,slots,inventorySlots,icons); }
    public SlotOverlay withSlot(int slot,Integer colour) { return withColour(false,slot,colour); }
    public SlotOverlay withColour(boolean inventory,int slot,Integer colour) {
        Map<Integer,Integer> next = new LinkedHashMap<>(inventory ? inventorySlots : slots);
        if (colour == null) next.remove(slot); else next.put(slot,colour);
        return new SlotOverlay(rows,inventory ? slots : next,inventory ? next : inventorySlots,icons);
    }
    public SlotOverlay withIcon(int slot,String icon) {
        Map<Integer,String> next = new LinkedHashMap<>(icons);
        if (icon == null) next.remove(slot); else next.put(slot,icon);
        return new SlotOverlay(rows,slots,inventorySlots,next);
    }
    public SlotOverlay withIcons(Map<Integer,String> next) { return new SlotOverlay(rows,slots,inventorySlots,next); }
    public static int translucent(int rgb,double opacityPercent) {
        double opacity = Double.isFinite(opacityPercent) ? Math.clamp(opacityPercent,5,80) : 30;
        return ((int)Math.round(opacity * 255 / 100) << 24) | (rgb & 0xFFFFFF);
    }
}
