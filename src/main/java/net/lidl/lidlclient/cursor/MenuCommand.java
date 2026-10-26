package net.lidl.lidlclient.cursor;

/** Stored commands omit the slash expected only by the chat input. Blank means observe only. */
public final class MenuCommand {
    private MenuCommand() { }
    public static String normalize(String input) {
        String value=input.strip();
        if (value.isEmpty()) return "";
        if (value.startsWith("/")) value=value.substring(1).strip();
        if (value.isEmpty() || value.startsWith("/") || value.length()>256
                || value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Enter one command, e.g. /pets, or leave blank.");
        return value;
    }
}
