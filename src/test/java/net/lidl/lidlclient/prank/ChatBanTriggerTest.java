package net.lidl.lidlclient.prank;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatBanTriggerTest {
    @Test void matchesSupportedChannelsAndRanks() {
        assertTrue(ChatBanTrigger.matches("Party > Alex: !ban"));
        assertTrue(ChatBanTrigger.matches("Party > [MVP++] Alex: !ban"));
        assertTrue(ChatBanTrigger.matches("Co-op > [VIP] Alex: !ban"));
        assertTrue(ChatBanTrigger.matches("Coop > Alex: !ban"));
        assertTrue(ChatBanTrigger.matches("From [MVP+] Alex: !ban"));
        assertTrue(ChatBanTrigger.matches("From Alex: !ban"));
        assertTrue(ChatBanTrigger.matches("§dParty > §b[MVP+] Alex§f: §r!BAN "));
    }

    @Test void rejectsPublicChatQuotesArgumentsAndPuzzleFails() {
        assertFalse(ChatBanTrigger.matches("[MVP+] Alex: !ban"));
        assertFalse(ChatBanTrigger.matches("Guild > Alex: !ban"));
        assertFalse(ChatBanTrigger.matches("To Alex: !ban"));
        assertFalse(ChatBanTrigger.matches("Party > Alex: please type !ban"));
        assertFalse(ChatBanTrigger.matches("Party > Alex: !ban Bob"));
        assertFalse(ChatBanTrigger.matches("Party > Alex: !banned"));
        assertFalse(ChatBanTrigger.matches("PUZZLE FAIL! Alex killed a Blaze in the wrong order!"));
    }
}
