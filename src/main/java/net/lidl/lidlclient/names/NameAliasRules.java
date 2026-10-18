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

    private static final Resolver BUILT_IN = new Resolver(null, "");

    private NameAliasRules() { }

    public static String cleanCustomName(String input) {
        if (input == null) return "";
        String clean = input.replaceAll("(?i)§[0-9A-FK-ORX]", "")
                .replaceAll("[\\p{Cc}\\p{Cf}]", "").strip();
        int count = clean.codePointCount(0, clean.length());
        return count <= 32 ? clean : clean.substring(0, clean.offsetByCodePoints(0, 32));
    }

    public static final class Resolver {
        private final Map<String, Alias> aliases = new LinkedHashMap<>();
        private final Pattern names;

        public Resolver(String selfUsername, String customName) {
            ALIASES.forEach(alias -> aliases.put(alias.username(), alias));
            String custom = cleanCustomName(customName);
            if (selfUsername != null && selfUsername.matches("[A-Za-z0-9_]{1,16}") && !custom.isEmpty()) {
                String real = selfUsername.toLowerCase(Locale.ROOT);
                Alias builtIn = aliases.get(real);
                aliases.put(real, new Alias(real, custom,
                        builtIn == null ? List.of(0xFFFFFF) : builtIn.colors()));
            }
            String alternatives = String.join("|", aliases.keySet().stream().map(Pattern::quote).toList());
            names = Pattern.compile("(?<![A-Za-z0-9_])(?:" + alternatives + ")(?![A-Za-z0-9_])",
                    Pattern.CASE_INSENSITIVE);
        }

        public List<Match> find(String text) {
            List<Match> matches = new ArrayList<>();
            Matcher matcher = names.matcher(text);
            while (matcher.find()) matches.add(new Match(matcher.start(), matcher.end(),
                    aliases.get(matcher.group().toLowerCase(Locale.ROOT))));
            return matches;
        }
    }

    public static List<Match> find(String text) { return BUILT_IN.find(text); }
}
