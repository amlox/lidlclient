package net.lidl.lidlclient.dungeons;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import java.io.ByteArrayOutputStream;
import java.net.URI;

import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** One bounded asynchronous Lidl-only transport; stale run callbacks cannot update the map. */
final class DungeonRelayConnection {
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final AtomicInteger generation=new AtomicInteger();
    private volatile WebSocket socket;
    private volatile boolean connecting;
    private volatile String status="Idle";
    private volatile long retryAt;
    private int failures;
    private CompletableFuture<?> sends=CompletableFuture.completedFuture(null);
    boolean connected() {var s=socket;return s!=null&&!s.isOutputClosed()&&!s.isInputClosed();}
    String status() {return status;}
    void close() {
        generation.incrementAndGet();connecting=false;status="Idle";retryAt=0;failures=0;
        var old=socket;socket=null;if(old!=null) old.sendClose(WebSocket.NORMAL_CLOSURE,"Dungeon session ended").exceptionally(e->null);
        synchronized(this) {sends=CompletableFuture.completedFuture(null);}
    }
    private void failed(int epoch) {
        if(!generation.compareAndSet(epoch,epoch+1)) return;
        var old=socket;socket=null;if(old!=null) old.abort();connecting=false;
        long delay=Math.min(120_000L,10_000L<<Math.min(failures++,4));retryAt=System.currentTimeMillis()+delay;
        status="Relay unavailable; retry in "+delay/1000+"s";
    }
    void connect(Minecraft client,URI uri,Runnable ready,Consumer<String> receive) {
        if(connected()||connecting||System.currentTimeMillis()<retryAt) return;
        int epoch=generation.incrementAndGet();connecting=true;status="Connecting";
        try {
            var builder=HTTP.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10)).header("User-Agent","LidlClient").header("X-Mod-Version",net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("lidlclient").orElseThrow().getMetadata().getVersion().getFriendlyString());
            builder.buildAsync(uri,new WebSocket.Listener() {
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
                    if(text.length()+data.length()>8192) {failed(epoch);return CompletableFuture.completedFuture(null);}
                    text.append(data);if(last) {deliver(ws,text.toString());text.setLength(0);}ws.request(1);return CompletableFuture.completedFuture(null);
                }
                @Override public CompletionStage<?> onBinary(WebSocket ws,ByteBuffer data,boolean last) {
                    if(!current(ws)||binary.size()+data.remaining()>8192) {failed(epoch);return CompletableFuture.completedFuture(null);}
                    byte[] part=new byte[data.remaining()];data.get(part);binary.writeBytes(part);
                    if(last) {deliver(ws,binary.toString(StandardCharsets.UTF_8));binary.reset();}ws.request(1);return CompletableFuture.completedFuture(null);
                }
                @Override public CompletionStage<?> onClose(WebSocket ws,int code,String reason) {if(current(ws)) failed(epoch);return CompletableFuture.completedFuture(null);}
                @Override public void onError(WebSocket ws,Throwable error) {if(generation.get()==epoch) failed(epoch);}
            }).whenComplete((ws,error)->{if(error!=null) failed(epoch);});
        } catch(RuntimeException error) {failed(epoch);}
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
