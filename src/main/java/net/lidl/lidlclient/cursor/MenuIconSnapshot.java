package net.lidl.lidlclient.cursor;

import java.util.Map;

/** Keep menu size even when its last rows (or every slot) are empty. */
public record MenuIconSnapshot(int rows, Map<Integer,String> icons) {
    public MenuIconSnapshot {
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("Chest rows must be 1–6");
        icons = Map.copyOf(icons);
        if (icons.keySet().stream().anyMatch(slot -> slot < 0 || slot >= rows * 9))
            throw new IllegalArgumentException("Icon outside menu bounds");
    }
}
