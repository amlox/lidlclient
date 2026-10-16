package net.lidl.lidlclient.prank;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PuzzleFailTriggerTest {
    @Test void matchesLocalPlayerAnnouncements() {
        assertTrue(PuzzleFailTrigger.matches("PUZZLE FAIL! Alex failed Tic Tac Toe! YIKES!", "Alex"));
        assertTrue(PuzzleFailTrigger.matches("§cPUZZLE FAIL! §rAlex has lost Tic Tac Toe! YIKES!", "Alex"));
        assertTrue(PuzzleFailTrigger.matches("PUZZLE FAIL! [MVP+] Alex has answered incorrectly!", "Alex"));
    }

    @Test void rejectsOtherPlayersAndChatQuotingTheAnnouncement() {
        assertFalse(PuzzleFailTrigger.matches("PUZZLE FAIL! Alex2 failed Tic Tac Toe!", "Alex"));
        assertFalse(PuzzleFailTrigger.matches("Party > Alex: PUZZLE FAIL! Alex failed Tic Tac Toe!", "Alex"));
        assertFalse(PuzzleFailTrigger.matches("PUZZLE SOLVED! Alex completed Tic Tac Toe!", "Alex"));
    }
}
