package net.lidl.lidlclient.PartyCMDS;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Command switches independent from Minecraft, shared by GUI and chat handlers. */
public final class PartyCommandSwitches {
    public static final List<String> COMMANDS = List.of("!ban", "!tesco", "!dice", "!coinflip",
            "!slots", "!iq", "!femboy", "!jew", "!autism", "!white", "!mexican", "!help");
    private boolean enabled = true;
    private final Map<String, Boolean> commands = new LinkedHashMap<>();

    public PartyCommandSwitches() { COMMANDS.forEach(command -> commands.put(command, true)); }
    private static String normalize(String command) { return command.toLowerCase(Locale.ROOT); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public boolean configured(String command) { return commands.getOrDefault(normalize(command), false); }
    public boolean allows(String command) { return enabled && configured(command); }
    public void set(String command, boolean value) {
        String key = normalize(command);
        if (!commands.containsKey(key)) throw new IllegalArgumentException("Unknown command: " + command);
        commands.put(key, value);
    }
    public static String commandName(String message) {
        return message.strip().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    }
    public Map<String, Boolean> snapshot() { return Map.copyOf(commands); }
    public List<String> activeCommands() { return COMMANDS.stream().filter(this::allows).toList(); }
}
