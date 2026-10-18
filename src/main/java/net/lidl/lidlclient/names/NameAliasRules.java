package net.lidl.lidlclient.names;

import java.util.*;
import java.util.regex.*;

/** Built-in visual aliases. Account names and command arguments never change. */
public final class NameAliasRules {
    public record Alias(String username, String display, List<Integer> colors) {
        public Alias { colors = List.copyOf(colors); }
        public int color(int glyph) { return colors.get(Math.min(glyph, colors.size() - 1)); }
    }
    public record Match(int start, int end, Alias alias) { }

    public static final List<Alias> ALIASES = List.of(
            new Alias("syntocxleftfoot", "Tesco", List.of(0x000000)),
            new Alias("syntocxrightfoot", "Lidl", List.of(0xFF5555)),
            new Alias("syntocxfeetpics", "petal", List.of(0xFF75ED, 0xFC83EB, 0xFC90EB, 0xFFA1ED, 0xFFADEE)),
            new Alias("__beacon", "Syntocx", List.of(0xFF55FF)),
            new Alias("fawnelle", "Konstsus", List.of(0x5555FF)),
            new Alias("quak07", "quak", List.of(0xFFFF55)));
    private static final Map<String, Alias> BY_NAME = new HashMap<>();
    private static final Pattern NAMES;
    static {
        ALIASES.forEach(alias -> BY_NAME.put(alias.username(), alias));
        String alternatives = String.join("|", ALIASES.stream().map(alias -> Pattern.quote(alias.username())).toList());
        NAMES = Pattern.compile("(?<![A-Za-z0-9_])(?:" + alternatives + ")(?![A-Za-z0-9_])",
                Pattern.CASE_INSENSITIVE);
    }

    private NameAliasRules() { }

    public static List<Match> find(String text) {
        List<Match> matches = new ArrayList<>();
        Matcher matcher = NAMES.matcher(text);
        while (matcher.find()) matches.add(new Match(matcher.start(), matcher.end(),
                BY_NAME.get(matcher.group().toLowerCase(Locale.ROOT))));
        return matches;
    }
}
