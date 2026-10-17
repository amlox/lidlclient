package net.lidl.lidlclient.hotbar;

import java.util.List;
import java.util.regex.Pattern;

/** Name matching and hotbar search; never searches or moves main-inventory items. */
public final class DungeonbreakerSlots {
    private static final Pattern NAME = Pattern.compile("\\bdungeon\\s*breaker\\b", Pattern.CASE_INSENSITIVE);

    private DungeonbreakerSlots() { }

    public static boolean matches(String name) {
        return name != null && NAME.matcher(name.replaceAll("(?i)§[0-9A-FK-ORX]", "")).find();
    }

    public static int find(List<String> names, int selected) {
        int slots = Math.min(9, names.size());
        if (selected >= 0 && selected < slots && matches(names.get(selected))) return selected;
        for (int i = 0; i < slots; i++) {
            if (matches(names.get(i))) return i;
        }
        return -1;
    }
}
