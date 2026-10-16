package net.lidl.lidlclient.prank;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.lidl.lidlclient.LidlClient;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.Locale;

/** Instant local disconnect prank for !ban in party, co-op, or whisper chat. */
public final class PuzzleFailPrank implements ClientModInitializer {
    private ClientPacketListener pendingConnection;
    private boolean preview;

    private static boolean isHypixel(Minecraft client) {
        var server = client.getCurrentServer();
        if (server == null) return false;
        String host = server.ip.toLowerCase(Locale.ROOT).split(":", 2)[0];
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }

    private void receiveCommand(Component message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !isHypixel(client)
                || pendingConnection != null || !ChatBanTrigger.matches(message.getString())) return;
        pendingConnection = client.getConnection();
        preview = false;
        LidlClient.LOGGER.info("Chat ban prank received; disconnecting on this tick.");
    }

    @Override
    public void onInitializeClient() {
        LidlClient.LOGGER.info("Chat ban prank initialized; use /puzzleprank test to preview.");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("puzzleprank")
                        .then(ClientCommands.literal("test").executes(context -> {
                            Minecraft client = Minecraft.getInstance();
                            pendingConnection = client.getConnection();
                            preview = true;
                            context.getSource().sendFeedback(Component.literal(
                                    "[LidlClient] Fake ban test: disconnecting now."));
                            return 1;
                        }))));

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) receiveCommand(message);
        });
        ClientReceiveMessageEvents.CHAT.register(
                (message, signedMessage, sender, params, timestamp) -> receiveCommand(message));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingConnection == null) return;
            if (client.getConnection() != pendingConnection || client.player == null
                    || (!preview && !isHypixel(client))) {
                pendingConnection = null;
                return;
            }
            pendingConnection = null;
            preview = false;
            client.disconnect(new FakeBanScreen(), false);
        });
    }
}
