package net.lidl.lidlclient.reels;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.*;

/** Loopback-only, authenticated, short-lived commands. No browsing data is received. */
public final class ReelsBridge implements AutoCloseable {
    public enum Command { NEXT, PREVIOUS, TOGGLE_PLAY }
    private record Pending(Command command, long deadline) { }
    private final BlockingQueue<Pending> queue = new ArrayBlockingQueue<>(16);
    private final HttpServer server;
    private final ExecutorService workers = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "Lidl Reels bridge"); thread.setDaemon(true); return thread;
    });
    public ReelsBridge(int port, String token) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 4);
        byte[] expected = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
        server.createContext("/poll", exchange -> {
            try {
                String supplied = exchange.getRequestHeaders().getFirst("Authorization");
                if (!exchange.getRequestURI().getPath().equals("/poll")) {
                    exchange.sendResponseHeaders(404, -1); return;
                }
                if (!exchange.getRequestMethod().equals("GET")) {
                    exchange.sendResponseHeaders(405, -1); return;
                }
                if (supplied == null || !MessageDigest.isEqual(expected, supplied.getBytes(StandardCharsets.UTF_8))) {
                    exchange.sendResponseHeaders(401, -1); return;
                }
                Pending pending = queue.poll(15, TimeUnit.SECONDS);
                while (pending != null && System.nanoTime() > pending.deadline()) pending = queue.poll();
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                if (pending == null) { exchange.sendResponseHeaders(204, -1); return; }
                byte[] body = ("{\"command\":\"" + pending.command().name() + "\"}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally { exchange.close(); }
        });
        server.setExecutor(workers);
        server.start();
    }
    public int port() { return server.getAddress().getPort(); }
    public void send(Command command) { queue.offer(new Pending(command, System.nanoTime() + TimeUnit.SECONDS.toNanos(2))); }
    public void clear() { queue.clear(); }
    @Override public void close() { queue.clear(); server.stop(0); workers.shutdownNow(); }
}
