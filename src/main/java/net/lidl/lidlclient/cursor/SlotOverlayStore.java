package net.lidl.lidlclient.cursor;

import com.google.gson.JsonElement;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tolerant decoding keeps a bad slot entry from breaking keybind loading. */
public final class SlotOverlayStore {
    private SlotOverlayStore() { }
    public static Map<String,SlotOverlay> read(JsonElement source) {
        Map<String,SlotOverlay> result = new LinkedHashMap<>();
        if (source == null || !source.isJsonObject()) return result;
        source.getAsJsonObject().entrySet().forEach(entry -> {
            if (!entry.getValue().isJsonObject()) return;
            var value = entry.getValue().getAsJsonObject();
            int rows = 6;
            Integer savedRows = integer(value.get("rows"),1,6);
            if (savedRows != null) rows = savedRows;
            Map<Integer,Integer> slots = new LinkedHashMap<>();
            var savedSlots = value.get("slots");
            if (savedSlots != null && savedSlots.isJsonObject()) {
                savedSlots.getAsJsonObject().entrySet().forEach(slot -> {
                    try {
                        int index = Integer.parseInt(slot.getKey());
                        Integer colour = integer(slot.getValue(),0,0xFFFFFF);
                        if (index >= 0 && index < 54 && colour != null) slots.put(index,colour);
                    } catch (NumberFormatException ignored) { }
                });
            }
            result.put(entry.getKey(),new SlotOverlay(rows,slots));
        });
        return result;
    }
    private static Integer integer(JsonElement value,int min,int max) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return null;
        double n = value.getAsDouble();
        return Double.isFinite(n) && n == Math.rint(n) && n >= min && n <= max ? (int)n : null;
    }
}
