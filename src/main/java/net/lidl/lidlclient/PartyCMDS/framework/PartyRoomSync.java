package net.lidl.lidlclient.PartyCMDS.framework;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lidl.lidlclient.config.ClientConfig;
import net.minecraft.client.Minecraft;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.function.BiConsumer;

/** Sharing is opt-in each launch; room configuration and downloaded replies are retained. */
public final class PartyRoomSync {
    private static RoomSyncClient client;
    private static boolean initialized;
    private static long retryAfter;
    private static String status="Offline — local replies only";
    private static String endpoint=value("roomEndpoint","ws://127.0.0.1:8765");
    private static String room=value("roomName","friends");
    private static String token=value("roomToken","");
    private PartyRoomSync() { }
    private static String value(String key,String fallback) {
        String value=ClientConfig.string("party_commands",key); return value==null?fallback:value;
    }
    public static String endpoint() { return endpoint; }
    public static String room() { return room; }
    public static String token() { return token; }
    public static String scope() { return endpoint+"\n"+room; }
    public static String status() { return status; }
    public static long revision() { return connected()?client.revision():-1; }
    public static boolean connected() { return client!=null && client.ready(); }
    public static boolean connectingOrConnected() { return client!=null; }
    public static void initialize() {
        if (initialized) return; initialized=true; PartyReplyLibrary.restoreRoom();
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (client==null) return;
            client.tick();
            if (client.broken()) {
                long now=System.nanoTime();
                if (retryAfter==0) retryAfter=now+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
                else if (now>=retryAfter) {
                    try { connect(endpoint,room,token); }
                    catch (RuntimeException error) { status=error.getMessage(); }
                }
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> disconnect());
    }
    public static void connect(String address,String name,String secret) {
        disconnect(); endpoint=address.strip(); room=name.strip(); token=secret;
        PartyReplyLibrary.restoreRoom();
        RoomSyncClient next=new RoomSyncClient(Minecraft.getInstance()::execute,PartyReplyLibrary::acceptRoom,value -> status=value);
        try { next.connect(endpoint,room,token); }
        catch (RuntimeException exception) { next.close(); throw exception; }
        client=next;
        ClientConfig.set("party_commands","roomEndpoint",endpoint);
        ClientConfig.set("party_commands","roomName",room);
        ClientConfig.set("party_commands","roomToken",token);
    }
    public static void disconnect() {
        if (client!=null) client.close(); client=null; retryAfter=0; status="Offline — local / cached replies";
    }
    public static void publish(PartyReply reply,long baseRevision,BiConsumer<Boolean,String> callback) {
        if (!connected()) { callback.accept(false,"Connect to a room first"); return; }
        client.put(reply,baseRevision<0?client.revision():baseRevision,(success,message) -> {
            if (success) PartyReplyLibrary.remove(reply.trigger());
            callback.accept(success,message);
        });
    }
    public static void delete(String trigger,BiConsumer<Boolean,String> callback) {
        if (!connected()) { callback.accept(false,"Connect to delete a shared reply"); return; }
        client.delete(trigger,callback);
    }
    public static void claim(String server,String message,BiConsumer<Boolean,String> callback) {
        if (!connected()) { callback.accept(false,"Room disconnected"); return; }
        try {
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((server+"\n"+message).getBytes(StandardCharsets.UTF_8)));
            client.claim(hash,callback);
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
