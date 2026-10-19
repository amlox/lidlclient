package net.lidl.lidlclient.dungeons;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** One bounded asynchronous transport per service; stale run callbacks cannot update the map. */
final class DungeonRelayConnection {
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final DungeonRelayProtocol.Backend backend;
    private final AtomicInteger generation=new AtomicInteger();
    private volatile WebSocket socket;
    private volatile boolean connecting;
    private volatile String status="Idle";
    private volatile long retryAt;
    private int failures;
    private CompletableFuture<?> sends=CompletableFuture.completedFuture(null);
    DungeonRelayConnection(DungeonRelayProtocol.Backend backend) {this.backend=backend;}
    boolean connected() {var s=socket;return s!=null&&!s.isOutputClosed()&&!s.isInputClosed();}
    String status() {return status;}
    void waiting(String reason) {if(!connecting&&!connected()) status=reason;}
    void close() {
        generation.incrementAndGet();connecting=false;status="Idle";retryAt=0;failures=0;
        var old=socket;socket=null;if(old!=null) old.sendClose(WebSocket.NORMAL_CLOSURE,"Dungeon session ended").exceptionally(e->null);
        synchronized(this) {sends=CompletableFuture.completedFuture(null);}
    }
    private void failed(int epoch) {
        if(!generation.compareAndSet(epoch,epoch+1)) return;
        var old=socket;socket=null;if(old!=null) old.abort();connecting=false;
        DungeonRelayAuth.invalidate(backend);
        long delay=Math.min(120_000L,10_000L<<Math.min(failures++,4));retryAt=System.currentTimeMillis()+delay;
        status="Unavailable / authentication failed; retry in "+delay/1000+"s";
    }
    void connect(Minecraft client,String instance,Runnable ready,Consumer<String> receive) {
        if(connected()||connecting||System.currentTimeMillis()<retryAt) return;
        int epoch=generation.incrementAndGet();connecting=true;status=backend==DungeonRelayProtocol.Backend.ODIN?"Connecting":"Authenticating";
        CompletableFuture<DungeonRelayAuth.Token> auth;
        try {auth=backend==DungeonRelayProtocol.Backend.ODIN?CompletableFuture.completedFuture(null):DungeonRelayAuth.authenticate(HTTP,backend,client);} catch(RuntimeException error) {failed(epoch);return;}
        auth.thenCompose(token->{
            if(generation.get()!=epoch) return CompletableFuture.<WebSocket>failedFuture(new CancellationException());
            URI uri=switch(backend) {
                case ODIN->URI.create("wss://ws.odtheking.com/"+instance);
                case NOAMM->URI.create("wss://ws.noamm.org?name="+URLEncoder.encode(client.getUser().getName(),StandardCharsets.UTF_8)+"&token="+URLEncoder.encode(token.value(),StandardCharsets.UTF_8));
                case SKYBLOCKER->URI.create("wss://ws.hysky.de");
            };
            var builder=HTTP.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10)).header("User-Agent","LidlClient/"+DungeonRelayAuth.version());
            if(backend==DungeonRelayProtocol.Backend.ODIN) builder.header("X-Mod-Version",DungeonRelayAuth.version());
            if(backend==DungeonRelayProtocol.Backend.SKYBLOCKER) builder.header("Authorization","Bearer "+token.value());
            return builder.buildAsync(uri,new WebSocket.Listener() {
                private final StringBuilder text=new StringBuilder();
                private final ByteArrayOutputStream binary=new ByteArrayOutputStream();
                private boolean current(WebSocket ws) {return generation.get()==epoch&&socket==ws;}
                @Override public void onOpen(WebSocket ws) {
                    if(generation.get()!=epoch) {ws.abort();return;}
                    socket=ws;connecting=false;failures=0;status="Connected";ws.request(1);
                    client.execute(()->{if(current(ws)) ready.run();});
                }
                private void deliver(WebSocket ws,String value) {client.execute(()->{if(current(ws)) receive.accept(value);});}
                @Override public CompletionStage<?> onText(WebSocket ws,CharSequence data,boolean last) {
                    if(!current(ws)) {ws.abort();return CompletableFuture.completedFuture(null);}
                    if(text.length()+data.length()>131072) {failed(epoch);return CompletableFuture.completedFuture(null);}
                    text.append(data);if(last) {deliver(ws,text.toString());text.setLength(0);}ws.request(1);return CompletableFuture.completedFuture(null);
                }
                @Override public CompletionStage<?> onBinary(WebSocket ws,ByteBuffer data,boolean last) {
                    if(!current(ws)||binary.size()+data.remaining()>131072) {failed(epoch);return CompletableFuture.completedFuture(null);}
                    byte[] part=new byte[data.remaining()];data.get(part);binary.writeBytes(part);
                    if(last) {deliver(ws,binary.toString(StandardCharsets.UTF_8));binary.reset();}ws.request(1);return CompletableFuture.completedFuture(null);
                }
                @Override public CompletionStage<?> onClose(WebSocket ws,int code,String reason) {if(current(ws)) failed(epoch);return CompletableFuture.completedFuture(null);}
                @Override public void onError(WebSocket ws,Throwable error) {if(generation.get()==epoch) failed(epoch);}
            });
        }).whenComplete((ws,error)->{if(error!=null) failed(epoch);});
    }
    synchronized boolean send(JsonObject message) {
        if(message==null||!connected()) return false;
        WebSocket target=socket;int epoch=generation.get();String payload=message.toString();
        sends=sends.handle((result,error)->null).thenCompose(unused->{
            if(generation.get()!=epoch||socket!=target) return CompletableFuture.completedFuture(null);
            return target.sendText(payload,true).thenApply(ignored->null);
        }).exceptionally(error->{failed(epoch);return null;});return true;
    }
}
