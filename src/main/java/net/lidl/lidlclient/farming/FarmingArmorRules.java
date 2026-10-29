package net.lidl.lidlclient.farming;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Pure rules: never infer armor identity from a renameable display name. */
final class FarmingArmorRules {
    private static final Set<String> SETS = Set.of("HELIANTHUS", "FERMENTO", "SQUASH", "CROPIE");
    private static final List<String> SLOTS = List.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");

    private FarmingArmorRules() { }

    static boolean allowedArmor(List<String> ids) {
        if (ids.size() != SLOTS.size()) return false;
        for (int i = 0; i < SLOTS.size(); i++) {
            String suffix = "_" + SLOTS.get(i);
            String id = ids.get(i);
            if (id == null || !id.endsWith(suffix)
                    || !SETS.contains(id.substring(0, id.length() - suffix.length()))) return false;
        }
        return true;
    }

    static String plain(String text) {
        return text == null ? "" : text.replaceAll("§.", "").strip();
    }

    static boolean hypixel(String address) {
        String host = address.toLowerCase(Locale.ROOT).split(":", 2)[0];
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }

    static boolean garden(String title, List<String> sidebar, List<String> tab) {
        if (!plain(title).toUpperCase(Locale.ROOT).startsWith("SKYBLOCK")) return false;
        // Area stays Garden while individual plots have their own location names.
        if (tab.stream().map(FarmingArmorRules::plain)
                .anyMatch(line -> line.matches("Area:\\s*(?:The )?Garden"))) return true;
        return sidebar.stream().map(FarmingArmorRules::plain)
                .anyMatch(line -> line.matches("[⏣]\\s*(?:The )?Garden"));
    }
}
