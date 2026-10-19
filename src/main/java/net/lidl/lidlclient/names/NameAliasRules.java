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

    private static final Pattern CONTROLS=Pattern.compile("[\\p{Cc}\\p{Cf}]");

    public static final List<Alias> ALIASES = BuiltInAliases.PLAYERS;

    private static final Resolver BUILT_IN = new Resolver(null, "");

    private NameAliasRules() { }

    private static final Pattern COLOR_CODE = Pattern.compile("(?i)[&§](?:#[0-9a-f]{6}|[0-9a-fr])");
    private static final int[] LEGACY_COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };

    public static String cleanCustomName(String input) {
        if (input == null) return "";
        String clean = CONTROLS.matcher(input).replaceAll("").strip();
        StringBuilder result = new StringBuilder();
        int glyphs = 0;
        for (int offset = 0; offset < clean.length() && result.length() < 4096;) {
            Matcher code = COLOR_CODE.matcher(clean).region(offset, clean.length());
            if (code.lookingAt()) {
                result.append(code.group());
                offset = code.end();
            } else {
                if (glyphs++ == 200) break;
                int cp = clean.codePointAt(offset);
                result.appendCodePoint(cp);
                offset += Character.charCount(cp);
            }
        }
        return result.toString();
    }

    private static Alias customAlias(String username, String input, List<Integer> defaults) {
        StringBuilder display = new StringBuilder();
        List<Integer> colors = new ArrayList<>();
        Integer selected = null;
        for (int offset = 0; offset < input.length();) {
            Matcher code = COLOR_CODE.matcher(input).region(offset, input.length());
            if (code.lookingAt()) {
                String value = code.group().substring(1).toLowerCase(Locale.ROOT);
                selected = value.equals("r") ? null : value.startsWith("#")
                        ? Integer.parseInt(value.substring(1), 16)
                        : LEGACY_COLORS[Character.digit(value.charAt(0), 16)];
                offset = code.end();
            } else {
                int cp = input.codePointAt(offset);
                display.appendCodePoint(cp);
                colors.add(selected == null ? defaults.get(Math.min(colors.size(), defaults.size() - 1)) : selected);
                offset += Character.charCount(cp);
            }
        }
        return new Alias(username, display.toString(), colors);
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
                Alias alias = customAlias(real, custom,
                        builtIn == null ? List.of(0xFFFFFF) : builtIn.colors());
                if (!alias.display().isBlank()) aliases.put(real, alias);
            }
            String alternatives = String.join("|", aliases.keySet().stream().map(Pattern::quote).toList());
            names = Pattern.compile("(?<![A-Za-z0-9_])(?:" + alternatives + ")(?![A-Za-z0-9_])",
                    Pattern.CASE_INSENSITIVE);
        }

        public List<Match> find(String text) {
            Matcher matcher = names.matcher(text);
            if(!matcher.find()) return List.of();
            List<Match> matches = new ArrayList<>();
            do {matches.add(new Match(matcher.start(), matcher.end(),
                    aliases.get(matcher.group().toLowerCase(Locale.ROOT))));} while(matcher.find());
            return matches;
        }
    }

    public static List<Match> find(String text) { return BUILT_IN.find(text); }
}
