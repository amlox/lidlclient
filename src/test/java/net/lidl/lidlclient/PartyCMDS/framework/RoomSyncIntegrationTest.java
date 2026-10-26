package net.lidl.lidlclient.PartyCMDS.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real Java WebSocket clients against the Python relay started by CI. */
@EnabledIfEnvironmentVariable(named="PARTY_SYNC_TEST_URL",matches=".+")
class RoomSyncIntegrationTest {
    private static final class Session implements AutoCloseable {
        final ExecutorService executor=Executors.newSingleThreadExecutor();
        final BlockingQueue<Map<String,PartyReply>> snapshots=new LinkedBlockingQueue<>();
        final RoomSyncClient client=new RoomSyncClient(executor,snapshots::add,message -> { });
        Session(String room,String token) throws Exception {
            executor.submit(() -> client.connect(System.getenv("PARTY_SYNC_TEST_URL"),room,token)).get(5,TimeUnit.SECONDS);
            if (snapshots.poll(10,TimeUnit.SECONDS)==null) {
                close(); fail("Initial room snapshot");
            }
        }
        Map<String,PartyReply> next() throws Exception {
            var value=snapshots.poll(10,TimeUnit.SECONDS); assertNotNull(value,"Expected broadcast"); return value;
        }
        CompletableFuture<Boolean> put(PartyReply reply) {
            CompletableFuture<Boolean> future=new CompletableFuture<>();
            executor.execute(() -> client.put(reply,(ok,message) -> future.complete(ok))); return future;
        }
        CompletableFuture<Boolean> claim(String hash) {
            CompletableFuture<Boolean> future=new CompletableFuture<>();
            executor.execute(() -> client.claim(hash,(ok,message) -> future.complete(ok))); return future;
        }
        public void close() throws Exception {
            executor.submit(client::close).get(5,TimeUnit.SECONDS); executor.shutdownNow();
        }
    }
    @Test void twoJavaClientsShareEditDeleteAndCoordinateOneResponder() throws Exception {
        String room="test-"+UUID.randomUUID(),token=UUID.randomUUID().toString();
        var first=new PartyReply("!hello",List.of("Hi {player}"),true);
        try (Session a=new Session(room,token);Session b=new Session(room,token)) {
            assertTrue(a.put(first).get(10,TimeUnit.SECONDS));
            assertEquals(first,b.next().get("!hello")); a.next();
            var edited=new PartyReply("!hello",List.of("Edited"),false);
            assertTrue(b.put(edited).get(10,TimeUnit.SECONDS));
            assertEquals(edited,a.next().get("!hello")); b.next();
            CompletableFuture<Boolean> stale=new CompletableFuture<>();
            b.executor.execute(() -> b.client.put(first,0,(ok,message) -> stale.complete(ok)));
            assertFalse(stale.get(10,TimeUnit.SECONDS));
            assertEquals(edited,b.next().get("!hello"));
            var claimA=a.claim("a".repeat(64)); var claimB=b.claim("a".repeat(64));
            assertNotEquals(claimA.get(10,TimeUnit.SECONDS),claimB.get(10,TimeUnit.SECONDS));
            CompletableFuture<Boolean> deleted=new CompletableFuture<>();
            b.executor.execute(() -> b.client.delete("!hello",(ok,message) -> deleted.complete(ok)));
            assertTrue(deleted.get(10,TimeUnit.SECONDS));
            assertTrue(a.next().isEmpty()); b.next();
        }
        // Reconnect downloads persisted room state rather than inventing a local revision.
        try (Session reconnect=new Session(room,token)) { assertTrue(reconnect.client.ready()); }
    }
}
