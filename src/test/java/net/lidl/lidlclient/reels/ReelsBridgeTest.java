package net.lidl.lidlclient.reels;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReelsBridgeTest {
    @Test void requiresAuthenticationAndDeliversOnlyQueuedCommands() throws Exception {
        try (var bridge = new ReelsBridge(0, "test-secret")) {
            var client = HttpClient.newHttpClient();
            var uri = URI.create("http://127.0.0.1:" + bridge.port() + "/poll");
            bridge.send(ReelsBridge.Command.NEXT);
            assertEquals(401, client.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            var valid = HttpRequest.newBuilder(uri).header("Authorization", "Bearer test-secret");
            assertEquals(405, client.send(valid.POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            var response = client.send(valid.GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("{\"command\":\"NEXT\"}", response.body());
            bridge.send(ReelsBridge.Command.PREVIOUS);
            bridge.clear();
            bridge.send(ReelsBridge.Command.TOGGLE_PLAY);
            assertTrue(client.send(valid.GET().build(), HttpResponse.BodyHandlers.ofString()).body().contains("TOGGLE_PLAY"));
        }
    }
}
