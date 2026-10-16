package net.lidl.lidlclient.prank;

import java.util.regex.Pattern;

/** Matches server announcements, never party chat or another player's failure. */
public final class PuzzleFailTrigger {
    private PuzzleFailTrigger() { }

    public static boolean matches(String message, String username) {
        String plain = message.replaceAll("(?i)§[0-9A-FK-ORX]", "").strip();
        return Pattern.compile("^PUZZLE FAIL!\\s+(?:\\[[^]\\r\\n]+]\\s+)?"
                + Pattern.quote(username)
                + "\\s+(?:failed\\b|has (?:failed|lost|answered|killed)\\b).*",
                Pattern.CASE_INSENSITIVE).matcher(plain).matches();
    }
}
