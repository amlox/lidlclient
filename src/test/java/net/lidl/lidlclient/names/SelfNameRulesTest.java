package net.lidl.lidlclient.names;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SelfNameRulesTest {
    @Test void customSelfOverridesBuiltInWithoutChangingOtherPlayers() {
        var resolver = new NameAliasRules.Resolver("SyntocxLeftFoot", "My Nickname");
        var matches = resolver.find("SYNTOCXLEFTFOOT invited SyntocxRightFoot");
        assertEquals("My Nickname", matches.getFirst().alias().display());
        assertEquals(0, matches.getFirst().alias().color(0));
        assertEquals("Lidl", matches.getLast().alias().display());
    }

    @Test void worksForUnlistedAccountsAndResetRestoresDefaults() {
        var custom = new NameAliasRules.Resolver("OtherPlayer", "Cool Name");
        assertEquals("Cool Name", custom.find("OtherPlayer").getFirst().alias().display());
        assertTrue(new NameAliasRules.Resolver("OtherPlayer", "").find("OtherPlayer").isEmpty());
        assertEquals("Tesco", new NameAliasRules.Resolver("SyntocxLeftFoot", "")
                .find("SyntocxLeftFoot").getFirst().alias().display());
        assertTrue(custom.find("OtherPlayer2").isEmpty());
    }

    @Test void validatesInputWithoutSplittingUnicodeCharacters() {
        assertEquals("Hello", NameAliasRules.cleanCustomName("  §cHello\n  "));
        assertEquals("", NameAliasRules.cleanCustomName(null));
        String longName = "😀".repeat(40);
        String cleaned = NameAliasRules.cleanCustomName(longName);
        assertEquals(32, cleaned.codePointCount(0, cleaned.length()));
        assertFalse(Character.isHighSurrogate(cleaned.charAt(cleaned.length() - 1)));
    }
}
