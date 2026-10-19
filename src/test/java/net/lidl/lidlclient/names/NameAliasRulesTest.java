package net.lidl.lidlclient.names;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NameAliasRulesTest {
    @Test void exactMappingsAndColors() {
        assertEquals(List.of("✡ Tesco ✡", "Lidl", "petal", "Syntocx", "Kontsus", "quak"),
                NameAliasRules.ALIASES.stream().map(NameAliasRules.Alias::display).toList());
        assertEquals(List.of(0xFF75ED, 0xFC83EB, 0xFC90EB, 0xFFA1ED, 0xFFADEE),
                NameAliasRules.find("SyntocxFeetPics").getFirst().alias().colors());
        assertEquals(0x80AAFF, NameAliasRules.find("syntocxleftfoot").getFirst().alias().color(4));
        assertEquals(0xFF5555, NameAliasRules.find("syntocxrightfoot").getFirst().alias().color(1));
        assertEquals(0xFF55FF, NameAliasRules.find("__beacon").getFirst().alias().color(1));
        assertEquals(0x5555FF, NameAliasRules.find("Fawnelle").getFirst().alias().color(1));
        assertEquals(0xFFFF55, NameAliasRules.find("quak07").getFirst().alias().color(1));
    }

    @Test void matchingIsCaseInsensitiveAndRequiresWholeMinecraftNames() {
        assertEquals(1, NameAliasRules.find("[MVP+] SYNTOCXLEFTFOOT: hello").size());
        assertEquals(2, NameAliasRules.find("quak07 invited __beacon").size());
        assertTrue(NameAliasRules.find("xquak07 quak07_ syntocxleftfoot2").isEmpty());
        assertTrue(NameAliasRules.find("Tesco Lidl petal Kontsus").isEmpty());
    }
}
