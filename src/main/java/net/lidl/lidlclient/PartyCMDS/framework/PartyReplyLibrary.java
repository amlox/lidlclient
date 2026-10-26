package net.lidl.lidlclient.PartyCMDS.framework;

import net.lidl.lidlclient.config.ClientConfig;
import java.util.*;

/** Local edits override shared replies; shared replies override the legacy handlers. */
public final class PartyReplyLibrary {
    private static Map<String,PartyReply> local;
    private static Map<String,PartyReply> shared=Map.of();
    private PartyReplyLibrary() { }
    private static void load() {
        if (local!=null) return;
        try { local=ReplyCodec.readAll(ClientConfig.store().get("party_commands","replies"),true); }
        catch (RuntimeException ignored) { local=new LinkedHashMap<>(); }
    }
    public static PartyReply find(String trigger) {
        load(); return local.containsKey(trigger)?local.get(trigger):shared.get(trigger);
    }
    public static Map<String,PartyReply> all() {
        load(); Map<String,PartyReply> result=new TreeMap<>(shared); result.putAll(local); return Map.copyOf(result);
    }
    public static boolean isLocal(String trigger) { load(); return local.containsKey(trigger); }
    public static PartyReply roomReply(String trigger) { return shared.get(trigger); }
    public static boolean isShared(String trigger) { return shared.containsKey(trigger); }
    public static void save(PartyReply reply) {
        load();
        if (!local.containsKey(reply.trigger()) && local.size()>=ReplyCodec.MAX_COMMANDS)
            throw new IllegalArgumentException("Local library is full (64 commands)");
        local.put(reply.trigger(),reply); persist();
    }
    public static void remove(String trigger) { load(); local.remove(trigger); persist(); }
    private static void persist() { ClientConfig.set("party_commands","replies",local.values()); }
    public static void acceptRoom(Map<String,PartyReply> replies) {
        shared=Map.copyOf(replies);
        ClientConfig.set("party_commands","roomCache",shared.values());
        ClientConfig.set("party_commands","roomCacheScope",PartyRoomSync.scope());
    }
    public static void restoreRoom() {
        shared=Map.of();
        if (PartyRoomSync.scope().equals(ClientConfig.string("party_commands","roomCacheScope"))) {
            try { shared=Map.copyOf(ReplyCodec.readAll(ClientConfig.store().get("party_commands","roomCache"),true)); }
            catch (RuntimeException ignored) { }
        }
    }
    public static PartyReply example(String trigger) {
        return switch (trigger) {
            case "!dice" -> new PartyReply(trigger,List.of("You rolled a {roll:1:6}!"),true);
            case "!coinflip" -> new PartyReply(trigger,List.of("Coinflip: Heads!","Coinflip: Tails!"),true);
            case "!iq" -> new PartyReply(trigger,List.of("{player} has {roll:-100:200} IQ!"),true);
            case "!femboy" -> new PartyReply(trigger,List.of("{player} is {roll:1:100}% a femboy!"),true);
            default -> new PartyReply(trigger,List.of("Hello {player}!"),true);
        };
    }
}
