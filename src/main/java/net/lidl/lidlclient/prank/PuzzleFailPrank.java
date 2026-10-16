package net.lidl.lidlclient.prank;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.Locale;

/** One local prank ten seconds after the local player's puzzle failure. */
public final class PuzzleFailPrank implements ClientModInitializer {
    private static final long DELAY_NS = 10_000_000_000L;
    private ClientPacketListener pendingConnection;
    private long queuedAt;

    private static boolean isHypixel(Minecraft client) {
        var server = client.getCurrentServer();
        if (server == null) return false;
        String host = server.ip.toLowerCase(Locale.ROOT).split(":", 2)[0];
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            Minecraft client = Minecraft.getInstance();
            if (overlay || client.player == null || !isHypixel(client)
                    || pendingConnection != null) return;
            if (PuzzleFailTrigger.matches(message.getString(), client.player.getName().getString())) {
                pendingConnection = client.getConnection();
                queuedAt = System.nanoTime();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingConnection == null) return;
            if (client.getConnection() != pendingConnection || client.player == null
                    || !isHypixel(client)) {
                pendingConnection = null;
                return;
            }
            if (System.nanoTime() - queuedAt < DELAY_NS) return;
            var connection = pendingConnection;
            pendingConnection = null;
            connection.sendCommand("limbo");
            client.setScreen(new FakeBanScreen());
        });
    }
}
