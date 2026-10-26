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
            Map<Integer,Integer> inventory = new LinkedHashMap<>();
            var savedInventory=value.get("inventorySlots");
            if (savedInventory != null && savedInventory.isJsonObject()) {
                savedInventory.getAsJsonObject().entrySet().forEach(slot -> {
                    try {
                        int index=Integer.parseInt(slot.getKey());
                        Integer colour=integer(slot.getValue(),0,0xFFFFFF);
                        if (index>=0 && index<36 && colour!=null) inventory.put(index,colour);
                    } catch (NumberFormatException ignored) { }
                });
            }
            Map<Integer,String> icons = new LinkedHashMap<>();
            var savedIcons=value.get("icons");
            if (savedIcons != null && savedIcons.isJsonObject()) {
                savedIcons.getAsJsonObject().entrySet().forEach(slot -> {
                    try {
                        int index=Integer.parseInt(slot.getKey());
                        var icon=slot.getValue();
                        if (index>=0 && index<90 && icon.isJsonPrimitive()
                                && icon.getAsJsonPrimitive().isString() && icon.getAsString().length()<=65536)
                            icons.put(index,icon.getAsString());
                    } catch (NumberFormatException ignored) { }
                });
            }
            result.put(entry.getKey(),new SlotOverlay(rows,slots,inventory,icons));
        });
        return result;
    }
    private static Integer integer(JsonElement value,int min,int max) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return null;
        double n = value.getAsDouble();
        return Double.isFinite(n) && n == Math.rint(n) && n >= min && n <= max ? (int)n : null;
    }
}
