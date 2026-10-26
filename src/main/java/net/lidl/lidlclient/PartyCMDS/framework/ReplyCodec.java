package net.lidl.lidlclient.PartyCMDS.framework;

import com.google.gson.*;
import java.util.*;

/** Validates saved command definitions and migrates older reply formats. */
public final class ReplyCodec {
    public static final int MAX_COMMANDS=128;
    public static final Gson JSON=new Gson();
    private ReplyCodec() { }
    public static PartyReply read(JsonElement value) {
        if (value==null || !value.isJsonObject()) throw new IllegalArgumentException("Expected a reply object");
        var object=value.getAsJsonObject();
        String trigger=string(object.get("trigger"));
        var enabled=object.get("enabled");
        if (enabled==null || !enabled.isJsonPrimitive() || !enabled.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("Expected an enabled switch");
        var answers=object.get("answers");
        if (answers==null || !answers.isJsonArray()) throw new IllegalArgumentException("Expected answers");
        var destination=object.get("partyChat");
        if (destination!=null && (!destination.isJsonPrimitive() || !destination.getAsJsonPrimitive().isBoolean()))
            throw new IllegalArgumentException("Expected a party-chat switch");
        return new PartyReply(trigger,answers.getAsJsonArray().asList().stream().map(ReplyCodec::string).toList(),
                enabled.getAsBoolean(),destination==null || destination.getAsBoolean(),
                flag(object,"bypassLoopProtection"),flag(object,"runCommands"));
    }
    private static boolean flag(JsonObject object,String name) {
        var value=object.get(name);
        if (value==null) return false;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("Expected a boolean for "+name);
        return value.getAsBoolean();
    }
    public static String string(JsonElement value) {
        if (value==null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Expected text");
        return value.getAsString();
    }
    public static Map<String,PartyReply> readAll(JsonElement value,boolean tolerant) {
        Map<String,PartyReply> replies=new LinkedHashMap<>();
        if (value==null || !value.isJsonArray()) {
            if (tolerant) return replies;
            throw new IllegalArgumentException("Expected command list");
        }
        if (value.getAsJsonArray().size()>MAX_COMMANDS) throw new IllegalArgumentException("Too many commands");
        for (var item:value.getAsJsonArray()) {
            try {
                PartyReply reply=read(item);
                if (replies.putIfAbsent(reply.trigger(),reply)!=null) throw new IllegalArgumentException("Duplicate command");
            } catch (RuntimeException exception) { if (!tolerant) throw exception; }
        }
        return replies;
    }
}
