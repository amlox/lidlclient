package net.lidl.lidlclient.prank;

import java.util.regex.Pattern;

/** Only exact !ban messages in the supported Hypixel chat channels can trigger. */
public final class ChatBanTrigger {
    private static final String PLAYER = "(?:\\[[^]\\r\\n]+]\\s*)*[A-Za-z0-9_]{1,16}";
    private static final Pattern COMMAND = Pattern.compile(
            "^(?:(?:Party|Co-op|Coop)\\s*>\\s*|From\\s+)"
                    + PLAYER + ":\\s*!ban\\s*$",
            Pattern.CASE_INSENSITIVE);

    private ChatBanTrigger() { }

    public static boolean matches(String message) {
        String plain = message.replaceAll("(?i)§[0-9A-FK-ORX]", "")
                .replace('\u00a0', ' ').strip();
        return COMMAND.matcher(plain).matches();
    }
}
