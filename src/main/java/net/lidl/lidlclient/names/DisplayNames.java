package net.lidl.lidlclient.names;

import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import java.util.*;

/** Builds styled render copies, preserving rank colors, hover/click actions and original text. */
public final class DisplayNames {
    private static final java.util.regex.Pattern FORMAT=java.util.regex.Pattern.compile("(?i)§[0-9A-FK-ORX]");
    private record Glyph(int codepoint, Style style) { }
    private record AliasSequence(List<Glyph> glyphs) implements FormattedCharSequence {
        @Override public boolean accept(FormattedCharSink sink) {
            int offset = 0;
            for (Glyph glyph : glyphs) {
                if (!sink.accept(offset, glyph.style(), glyph.codepoint())) return false;
                offset += Character.charCount(glyph.codepoint());
            }
            return true;
        }
    }
    private DisplayNames() { }

    public static FormattedCharSequence fromString(String text) {
        return replace(sink -> StringDecomposer.iterateFormatted(text, Style.EMPTY, sink));
    }

    public static FormattedCharSequence replace(FormattedCharSequence source) {
        if (!NameChanger.isRenderingEnabled() || source instanceof AliasSequence) return source;
        StringBuilder text = new StringBuilder();
        source.accept((index, style, codepoint) -> { text.appendCodePoint(codepoint); return true; });
        List<NameAliasRules.Match> matches = NameChanger.findMatches(text.toString());
        if (matches.isEmpty()) return source;
        List<Style> styles = new ArrayList<>();
        source.accept((index, style, codepoint) -> {
            for (int i = 0; i < Character.charCount(codepoint); i++) styles.add(style);
            return true;
        });
        return new AliasSequence(List.copyOf(replaceGlyphs(text.toString(), styles, matches)));
    }

    public static FormattedText replace(FormattedText source) {
        if (!NameChanger.isRenderingEnabled()) return source;
        if (NameChanger.findMatches(FORMAT.matcher(source.getString()).replaceAll("")).isEmpty()) return source;
        // Visit only render inputs, never Component.getString() or chat-event payloads.
        StringBuilder text = new StringBuilder();
        List<Style> styles = new ArrayList<>();
        source.visit((style, part) -> {
            StringDecomposer.iterateFormatted(part, style, (index, glyphStyle, codepoint) -> {
                text.appendCodePoint(codepoint);
                for (int i = 0; i < Character.charCount(codepoint); i++) styles.add(glyphStyle);
                return true;
            });
            return Optional.empty();
        }, Style.EMPTY);
        List<NameAliasRules.Match> matches = NameChanger.findMatches(text.toString());
        if (matches.isEmpty()) return source;
        MutableComponent result = Component.empty();
        for (Glyph glyph : replaceGlyphs(text.toString(), styles, matches)) {
            result.append(Component.literal(new String(Character.toChars(glyph.codepoint()))).setStyle(glyph.style()));
        }
        return result;
    }

    private static List<Glyph> replaceGlyphs(String text, List<Style> styles, List<NameAliasRules.Match> matches) {
        List<Glyph> result = new ArrayList<>();
        int position = 0;
        for (var match : matches) {
            copy(text, styles, position, match.start(), result);
            Style inherited = styles.get(match.start());
            int glyphIndex = 0;
            for (int codepoint : match.alias().display().codePoints().toArray()) {
                result.add(new Glyph(codepoint, inherited.withColor(match.alias().color(glyphIndex++))));
            }
            position = match.end();
        }
        copy(text, styles, position, text.length(), result);
        return result;
    }

    private static void copy(String text, List<Style> styles, int from, int to, List<Glyph> output) {
        for (int i = from; i < to;) {
            int codepoint = text.codePointAt(i);
            output.add(new Glyph(codepoint, styles.get(i)));
            i += Character.charCount(codepoint);
        }
    }
}
