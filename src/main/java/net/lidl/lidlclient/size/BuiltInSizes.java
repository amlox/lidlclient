package net.lidl.lidlclient.size;

import java.util.Map;

/** Edit these permanent visual sizes, then rebuild the mod. */
public final class BuiltInSizes {
    private BuiltInSizes() { }

    // Lowercase real username, uniform X/Y/Z multiplier (1.0 = normal).
    // Add another Map.entry("username", 1.5) with a comma between entries.
    public static final Map<String, Double> PLAYERS = Map.ofEntries(
            Map.entry("syntocxleftfoot", 0.5),
            Map.entry("syntocxrightfoot", 2.0));
}
