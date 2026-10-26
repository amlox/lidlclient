package net.lidl.lidlclient.commands;

import net.fabricmc.api.ClientModInitializer;
import net.lidl.lidlclient.config.BuiltInFeatures;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

/** Built-in shortcut for the Crimson Isle warp command, controlled under General settings. */
public final class WarpCommands implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("nigger").executes(context -> {
                    if (!BuiltInFeatures.warpShortcut()) {
                        context.getSource().sendError(Component.literal("The built-in warp shortcut is disabled in General settings."));
                        return 0;
                    }
                    var connection = context.getSource().getClient().getConnection();
                    if (connection == null) {
                        context.getSource().sendError(Component.literal("You must be connected to a server to warp."));
                        return 0;
                    }
                    connection.sendCommand("warp crimson");
                    return 1;
                })));
    }
}
