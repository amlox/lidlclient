package net.lidl.lidlclient.farming;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Match the visible armor names, including reforges, formatting and upgrade suffixes. */
final class FarmingArmorRules {
    private static final Set<String> SETS = Set.of("helianthus", "fermento", "squash", "cropie");

    private FarmingArmorRules() { }

    static boolean allowedArmor(List<String> names) {
        return names.size() == 4 && names.stream().allMatch(name -> {
            String normalized = plain(name).toLowerCase(Locale.ROOT);
            return SETS.stream().anyMatch(normalized::contains);
        });
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
