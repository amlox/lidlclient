package net.lidl.lidlclient.PartyCMDS.framework;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Minecraft-independent WebSocket transport; all state callbacks run on the supplied executor. */
public final class RoomSyncClient implements AutoCloseable {
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final Executor executor;
    private final Consumer<Map<String,PartyReply>> snapshots;
    private final Consumer<String> status;
    private final Map<String,Pending> pending=new HashMap<>();
    private record Pending(long deadline,BiConsumer<Boolean,String> callback) { }
    private volatile WebSocket socket;
    private volatile boolean closed;
    private volatile boolean broken;
    private volatile boolean ready;
    private long revision;
    private long joiningDeadline;
    private CompletableFuture<WebSocket> connecting;
    private CompletableFuture<?> sending=CompletableFuture.completedFuture(null);
    private synchronized void send(WebSocket ws,String text) {
        sending=sending.thenCompose(ignored -> ws.sendText(text,true));
        sending.exceptionally(error -> { failed(); return null; });
    }
    public RoomSyncClient(Executor executor,Consumer<Map<String,PartyReply>> snapshots,Consumer<String> status) {
        this.executor=executor; this.snapshots=snapshots; this.status=status;
    }
    public long revision() { return revision; }
    public boolean broken() { return broken; }
    public boolean ready() { return ready && !closed && !broken; }
    public void connect(String endpoint,String room,String token) {
        URI uri=URI.create(endpoint);
        if (!("ws".equals(uri.getScheme()) || "wss".equals(uri.getScheme())) || uri.getHost()==null
                || uri.getUserInfo()!=null || uri.getFragment()!=null)
            throw new IllegalArgumentException("Use a ws:// or wss:// server address");
        if (!room.matches("[A-Za-z0-9_-]{1,48}") || token.length()<12 || token.length()>128)
            throw new IllegalArgumentException("Room: 1–48 letters/numbers/_/-. Token: 12–128 characters");
        joiningDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        status.accept("Connecting…");
        connecting=HTTP.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(8)).buildAsync(uri,new WebSocket.Listener() {
            private final StringBuilder text=new StringBuilder();
            @Override public void onOpen(WebSocket ws) {
                if (closed) { ws.abort(); return; }
                socket=ws;
                JsonObject join=new JsonObject(); join.addProperty("type","join"); join.addProperty("protocol",1);
                join.addProperty("room",room); join.addProperty("token",token);
                send(ws,join.toString());
                ws.request(1);
            }
            @Override public CompletionStage<?> onText(WebSocket ws,CharSequence chunk,boolean last) {
                if (text.length()+chunk.length()>1_048_576) { ws.abort(); failed(); return null; }
                text.append(chunk);
                if (last) {
                    String message=text.toString(); text.setLength(0);
                    dispatch(() -> receive(message));
                }
                ws.request(1); return null;
            }
            @Override public CompletionStage<?> onBinary(WebSocket ws,ByteBuffer data,boolean last) {
                ws.abort(); failed(); return null;
            }
            @Override public CompletionStage<?> onClose(WebSocket ws,int code,String reason) { failed(); return null; }
            @Override public void onError(WebSocket ws,Throwable error) { failed(); }
        });
        connecting.exceptionally(error -> { failed(); return null; });
    }
    private void dispatch(Runnable action) {
        if (closed) return;
        try { executor.execute(() -> { if (!closed) action.run(); }); }
        catch (RejectedExecutionException exception) { if (!closed) throw exception; }
    }
    private void failed() {
        broken=true;
        dispatch(() -> {
            ready=false; status.accept("Offline — retrying; check server / room token");
            failPending("Connection lost; draft kept");
        });
    }
    private void receive(String message) {
        try {
            var object=JsonParser.parseString(message).getAsJsonObject();
            String type=ReplyCodec.string(object.get("type"));
            String id=object.has("requestId") && !object.get("requestId").isJsonNull()?ReplyCodec.string(object.get("requestId")):null;
            if (type.equals("snapshot") || type.equals("conflict")) {
                var value=object.get("revision");
                if (value==null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                        || value.getAsDouble()!=value.getAsLong() || value.getAsLong()<0)
                    throw new IllegalArgumentException("Invalid room revision");
                var replies=ReplyCodec.readAll(object.get("commands"),false);
                if (value.getAsLong()<revision) return;
                revision=value.getAsLong(); ready=true; snapshots.accept(Map.copyOf(replies));
                status.accept(type.equals("conflict")?"Edit conflict — review current room and retry":"Connected — room revision "+revision);
                complete(id,type.equals("snapshot"),type.equals("conflict")?"Someone edited first. Your draft is kept; review and retry.":"Shared");
            } else if (type.equals("claim")) {
                complete(id,object.get("granted").getAsBoolean(),"Claim result");
            } else if (type.equals("error")) {
                complete(id,false,"Server rejected the change; draft kept");
            } else throw new IllegalArgumentException("Unknown message");
        } catch (RuntimeException exception) {
            if (socket!=null) socket.abort(); failed();
        }
    }
    public void put(PartyReply reply,BiConsumer<Boolean,String> callback) { put(reply,revision,callback); }
    public void put(PartyReply reply,long baseRevision,BiConsumer<Boolean,String> callback) {
        JsonObject object=new JsonObject(); object.addProperty("type","put");
        object.addProperty("baseRevision",baseRevision); object.add("command",ReplyCodec.JSON.toJsonTree(reply));
        request(object,callback);
    }
    public void delete(String trigger,BiConsumer<Boolean,String> callback) {
        JsonObject object=new JsonObject(); object.addProperty("type","delete");
        object.addProperty("baseRevision",revision); object.addProperty("trigger",trigger); request(object,callback);
    }
    public void claim(String hash,BiConsumer<Boolean,String> callback) {
        JsonObject object=new JsonObject(); object.addProperty("type","claim"); object.addProperty("key",hash); request(object,callback);
    }
    private void request(JsonObject object,BiConsumer<Boolean,String> callback) {
        if (!ready()) { callback.accept(false,"Not connected; draft kept"); return; }
        String id=UUID.randomUUID().toString(); object.addProperty("requestId",id);
        pending.put(id,new Pending(System.nanoTime()+TimeUnit.SECONDS.toNanos(10),callback));
        send(socket,object.toString());
    }
    private void complete(String id,boolean success,String message) {
        Pending operation=pending.remove(id);
        if (operation!=null) operation.callback().accept(success,message);
    }
    public void tick() {
        long now=System.nanoTime();
        if (!ready && !broken && now>joiningDeadline) {
            if (socket!=null) socket.abort(); failed();
        }
        var expired=pending.entrySet().stream().filter(entry -> entry.getValue().deadline()<now).map(Map.Entry::getKey).toList();
        expired.forEach(id -> complete(id,false,"Request timed out; draft kept"));
    }
    private void failPending(String message) {
        var callbacks=new ArrayList<>(pending.values()); pending.clear();
        callbacks.forEach(value -> value.callback().accept(false,message));
    }
    @Override public void close() {
        closed=true; ready=false;
        if (connecting!=null) connecting.cancel(true);
        if (socket!=null) socket.abort();
        failPending("Disconnected; draft kept");
    }
}
