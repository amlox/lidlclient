package net.lidl.lidlclient.names;

import java.util.List;
import java.util.Locale;

/** Edit this list to add players or change their permanent display names/colors. */
public final class BuiltInAliases {
    private BuiltInAliases() { }

    // Format: alias("RealUsername", "Display name", 0xRRGGBB)
    // Multiple colors apply one per letter; the last color repeats.
    public static final List<NameAliasRules.Alias> PLAYERS = List.of(
            alias("SyntocxLeftFoot", "Tesco", 0x000000),
            alias("SyntocxRightFoot", "Lidl", 0xFF5555),
            alias("SyntocxFeetPics", "petal", 0xFF75ED, 0xFC83EB, 0xFC90EB, 0xFFA1ED, 0xFFADEE),
            alias("__beacon", "Syntocx", 0xFF55FF),
            alias("Fawnelle", "Kontsus", 0x5555FF),
            alias("quak07", "quak", 0xFFFF55));

    private static NameAliasRules.Alias alias(String username, String display, Integer... colors) {
        return new NameAliasRules.Alias(username.toLowerCase(Locale.ROOT), display, List.of(colors));
    }
}
