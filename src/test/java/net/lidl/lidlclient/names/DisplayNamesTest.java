package net.lidl.lidlclient.names;

import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DisplayNamesTest {
    private static String text(FormattedCharSequence sequence) {
        StringBuilder result = new StringBuilder();
        sequence.accept((index, style, cp) -> { result.appendCodePoint(cp); return true; });
        return result.toString();
    }

    @Test void preservesSourceTextAndSurroundingStylesAcrossComponentBoundaries() {
        Style rank = Style.EMPTY.withColor(0x55FFFF);
        Style name = Style.EMPTY.withBold(true).withInsertion("/msg SyntocxFeetPics ");
        Component original = Component.empty()
                .append(Component.literal("[MVP+] ").setStyle(rank))
                .append(Component.literal("Syntocx").setStyle(name))
                .append(Component.literal("FeetPics").setStyle(name))
                .append(Component.literal(": hello").setStyle(rank));
        FormattedText copy = DisplayNames.replace((FormattedText) original);
        assertEquals("[MVP+] petal: hello", copy.getString());
        assertEquals("[MVP+] SyntocxFeetPics: hello", original.getString());
        List<Style> aliasStyles = new ArrayList<>();
        copy.visit((style, part) -> {
            if (!part.isEmpty() && !part.equals(":")) {
                if ("/msg SyntocxFeetPics ".equals(style.getInsertion())) aliasStyles.add(style);
            }
            return Optional.empty();
        }, Style.EMPTY);
        assertEquals(List.of(0xFF75ED, 0xFC83EB, 0xFC90EB, 0xFFA1ED, 0xFFADEE),
                aliasStyles.stream().map(style -> style.getColor().getValue()).toList());
        assertTrue(aliasStyles.stream().allMatch(Style::isBold));
    }

    @Test void handlesUnicodeBeforeANameAndKeepsUnchangedSequencesByIdentity() {
        var source = FormattedCharSequence.forward("😀 quak07", Style.EMPTY);
        var result = DisplayNames.replace(source);
        assertEquals("😀 quak", text(result));
        assertEquals("😀 quak07", text(source));
        assertSame(result, DisplayNames.replace(result));
        var ordinary = FormattedCharSequence.forward("hello world", Style.EMPTY);
        assertSame(ordinary, DisplayNames.replace(ordinary));
    }

    @Test void rawLegacyColoredTextGetsAliasRgbAndRetainsRankText() {
        var result = DisplayNames.fromString("§b[MVP+] §aSyntocxRightFoot: hi");
        assertEquals("[MVP+] Lidl: hi", text(result));
        List<Integer> colors = new ArrayList<>();
        result.accept((index, style, cp) -> { colors.add(style.getColor().getValue()); return true; });
        assertEquals(0x55FFFF, colors.getFirst());
        for (int i = 7; i < 11; i++) assertEquals(0xFF5555, colors.get(i));
    }
}
