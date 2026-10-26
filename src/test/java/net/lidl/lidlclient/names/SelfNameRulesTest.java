package net.lidl.lidlclient.names;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SelfNameRulesTest {
    @Test void customSelfOverridesBuiltInWithoutChangingOtherPlayers() {
        var resolver = new NameAliasRules.Resolver("SyntocxLeftFoot", "My Nickname");
        var matches = resolver.find("SYNTOCXLEFTFOOT invited SyntocxRightFoot");
        assertEquals("My Nickname", matches.getFirst().alias().display());
        assertEquals(0x0055FF, matches.getFirst().alias().color(0));
        assertEquals("Lidl", matches.getLast().alias().display());
    }

    @Test void worksForUnlistedAccountsAndResetRestoresDefaults() {
        var custom = new NameAliasRules.Resolver("OtherPlayer", "Cool Name");
        assertEquals("Cool Name", custom.find("OtherPlayer").getFirst().alias().display());
        assertTrue(new NameAliasRules.Resolver("OtherPlayer", "").find("OtherPlayer").isEmpty());
        assertEquals("✡ Tesco ✡", new NameAliasRules.Resolver("SyntocxLeftFoot", "")
                .find("SyntocxLeftFoot").getFirst().alias().display());
        assertTrue(custom.find("OtherPlayer2").isEmpty());
    }

    @Test void parsesLegacyHexAndResetPerGlyph() {
        var alias = new NameAliasRules.Resolver("OtherPlayer", "&cRed &#FF75ED😀&rX")
                .find("OtherPlayer").getFirst().alias();
        assertEquals("Red 😀X", alias.display());
        assertEquals(0xFF5555, alias.color(0));
        assertEquals(0xFF75ED, alias.color(4));
        assertEquals(0xFFFFFF, alias.color(5));
        var gradient = new NameAliasRules.Resolver("SyntocxLeftFoot", "&aA&rB")
                .find("SyntocxLeftFoot").getFirst().alias();
        assertEquals(0x55FF55, gradient.color(0));
        assertEquals(0xFFFFFF, gradient.color(1));
        assertTrue(new NameAliasRules.Resolver("OtherPlayer", "&c").find("OtherPlayer").isEmpty());
        assertEquals("&zHi", new NameAliasRules.Resolver("OtherPlayer", "&zHi")
                .find("OtherPlayer").getFirst().alias().display());
        String colored = "&#FF75EDA".repeat(240);
        assertEquals(200, new NameAliasRules.Resolver("OtherPlayer", colored)
                .find("OtherPlayer").getFirst().alias().display().length());
    }

    @Test void disablingBuiltInsKeepsOnlyTheCustomSelfAlias() {
        var resolver=new NameAliasRules.Resolver("SyntocxLeftFoot","My name",false);
        var matches=resolver.find("SyntocxLeftFoot invited SyntocxRightFoot");
        assertEquals(1,matches.size());
        assertEquals("My name",matches.getFirst().alias().display());
        assertEquals(0xFFFFFF,matches.getFirst().alias().color(0));
        assertTrue(new NameAliasRules.Resolver("SyntocxLeftFoot","",false)
                .find("SyntocxLeftFoot invited SyntocxRightFoot").isEmpty());
        assertFalse(new NameAliasRules.Resolver(null,"",true).find("SyntocxRightFoot").isEmpty());
    }

    @Test void validatesInputWithoutSplittingUnicodeCharacters() {
        assertEquals("§cHello", NameAliasRules.cleanCustomName("  §cHello\n  "));
        assertEquals("", NameAliasRules.cleanCustomName(null));
        String longName = "😀".repeat(240);
        String cleaned = NameAliasRules.cleanCustomName(longName);
        assertEquals(200, cleaned.codePointCount(0, cleaned.length()));
        assertFalse(Character.isHighSurrogate(cleaned.charAt(cleaned.length() - 1)));
    }
}
