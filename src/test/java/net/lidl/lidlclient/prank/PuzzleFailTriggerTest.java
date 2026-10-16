package net.lidl.lidlclient.prank;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PuzzleFailTriggerTest {
    @Test void matchesLocalPlayerAnnouncements() {
        assertTrue(PuzzleFailTrigger.matches("PUZZLE FAIL! Alex failed Tic Tac Toe! YIKES!", "Alex"));
        assertTrue(PuzzleFailTrigger.matches("§cPUZZLE FAIL! §rAlex has lost Tic Tac Toe! YIKES!", "Alex"));
        assertTrue(PuzzleFailTrigger.matches("PUZZLE FAIL! [MVP+] Alex has answered incorrectly!", "Alex"));
    }

    @Test void matchesActualPuzzleFailureWording() {
        assertTrue(PuzzleFailTrigger.matches("PUZZLE FAIL! Alex killed a Blaze in the wrong order! Yikes!", "Alex"));
        assertTrue(PuzzleFailTrigger.matches("PUZZLE FAIL! Alex lost Tic Tac Toe! Yikes!", "Alex"));
        assertTrue(PuzzleFailTrigger.matches("PUZZLE FAIL! Alex was fooled by Melrose! Yikes!", "Alex"));
        assertTrue(PuzzleFailTrigger.matches("[STATUE] Oruo the Omniscient: Alex chose the wrong answer! I shall never forget this moment of misrememberance.", "Alex"));
        assertTrue(PuzzleFailTrigger.matches("PUZZLE FAIL!Alex killed a Blaze in the wrong order!", "Alex"));
        assertFalse(PuzzleFailTrigger.matches("[STATUE] Oruo the Omniscient: Alex2 chose the wrong answer!", "Alex"));
        assertFalse(PuzzleFailTrigger.matches("Party > Bob: [STATUE] Oruo the Omniscient: Alex chose the wrong answer!", "Alex"));
    }

    @Test void rejectsOtherPlayersAndChatQuotingTheAnnouncement() {
        assertFalse(PuzzleFailTrigger.matches("PUZZLE FAIL! Alex2 failed Tic Tac Toe!", "Alex"));
        assertFalse(PuzzleFailTrigger.matches("Party > Alex: PUZZLE FAIL! Alex failed Tic Tac Toe!", "Alex"));
        assertFalse(PuzzleFailTrigger.matches("PUZZLE SOLVED! Alex completed Tic Tac Toe!", "Alex"));
    }
}
