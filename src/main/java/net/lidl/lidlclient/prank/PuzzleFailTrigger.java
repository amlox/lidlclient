package net.lidl.lidlclient.prank;

import java.util.regex.Pattern;

/** Matches server announcements, never party chat or another player's failure. */
public final class PuzzleFailTrigger {
    private PuzzleFailTrigger() { }

    public static boolean matches(String message, String username) {
        String plain = message.replaceAll("(?i)§[0-9A-FK-ORX]", "")
                .replace('\u00a0', ' ').strip();
        String player = "(?:\\[[^]\\r\\n]+]\\s*)?" + Pattern.quote(username);
        String action = "(?:(?:has\\s+)?(?:failed|lost|answered|killed)\\b|was\\s+fooled\\b)";
        String failure = "^PUZZLE FAIL!\\s*" + player + "\\s+" + action + ".*";
        String oruo = "^\\[STATUE] Oruo the Omniscient:\\s*" + player
                + "\\s+chose the wrong answer!.*";
        return Pattern.compile("(?:" + failure + "|" + oruo + ")",
                Pattern.CASE_INSENSITIVE).matcher(plain).matches();
    }
}
