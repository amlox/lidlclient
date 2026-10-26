package net.lidl.lidlclient.PartyCMDS.framework;

import net.lidl.lidlclient.config.ClientConfig;
import java.util.*;

/** One local definition per trigger, with editable overrides for built-in commands. */
public final class PartyReplyLibrary {
    private static Map<String,PartyReply> local;
    private PartyReplyLibrary() { }
    private static void load() {
        if (local!=null) return;
        try { local=ReplyCodec.readAll(ClientConfig.store().get("party_commands","replies"),true); }
        catch (RuntimeException ignored) { local=new LinkedHashMap<>(); }
        // Retain previously downloaded replies as local definitions when removing sharing.
        try {
            var cached=ReplyCodec.readAll(ClientConfig.store().get("party_commands","roomCache"),true);
            cached.forEach((trigger,reply) -> {
                if (local.size()<ReplyCodec.MAX_COMMANDS) local.putIfAbsent(trigger,reply);
            });
            if (!cached.isEmpty()) persist();
        } catch (RuntimeException ignored) { }
        for (String key:List.of("roomCache","roomCacheScope","roomEndpoint","roomName","roomToken"))
            if (ClientConfig.store().get("party_commands",key)!=null) ClientConfig.set("party_commands",key,null);
    }
    public static PartyReply find(String trigger) { load(); return local.get(trigger); }
    public static Map<String,PartyReply> all() { load(); return Map.copyOf(local); }
    public static boolean isLocal(String trigger) { load(); return local.containsKey(trigger); }
    public static void create(PartyReply reply) {
        load();
        var names=new java.util.HashSet<>(net.lidl.lidlclient.PartyCMDS.PartyCommandSwitches.COMMANDS);
        names.addAll(local.keySet());
        PartyReply.requireAvailable(reply.trigger(),names);
        save(reply);
    }
    public static void save(PartyReply reply) {
        load();
        if (!local.containsKey(reply.trigger()) && local.size()>=ReplyCodec.MAX_COMMANDS)
            throw new IllegalArgumentException("Command library is full (128 commands)");
        local.put(reply.trigger(),reply); persist();
    }
    public static void remove(String trigger) { load(); local.remove(trigger); persist(); }
    private static void persist() { ClientConfig.set("party_commands","replies",local.values()); }
    public static PartyReply example(String trigger) {
        return switch (trigger) {
            case "!dice" -> new PartyReply(trigger,List.of("You rolled a {roll:1:6}!"),true);
            case "!coinflip" -> new PartyReply(trigger,List.of("Coinflip: Heads!","Coinflip: Tails!"),true);
            case "!iq" -> new PartyReply(trigger,List.of("{player} has {roll:-100:200} IQ!"),true);
            case "!jew" -> new PartyReply(trigger,List.of("{player} is {roll:1:100}% a jew!"),true);
            case "!autism" -> new PartyReply(trigger,List.of("{player} is {roll:1:100}% autistic!"),true);
            case "!white" -> new PartyReply(trigger,List.of("{player} is {roll:1:100}% white!"),true);
            case "!mexican" -> new PartyReply(trigger,List.of("{player} is {roll:1:100}% mexican!"),true);
            case "!slots" -> new PartyReply(trigger,List.of("Rolled [ {slot1} | {slot2} | {slot3} ]{jackpot}"),true);
            case "!femboy" -> new PartyReply(trigger,List.of("{player} is {roll:1:100}% a femboy!"),true);
            default -> new PartyReply(trigger,List.of("Hello {player}!"),true);
        };
    }
}
