package net.lidl.lidlclient.gui;

import net.fabricmc.api.ClientModInitializer;
import net.lidl.lidlclient.green.GreenScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;

/** Permanent client entry point for the module menu. */
public final class ModuleFramework implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ModuleRegistry.modules(); // Restore settings before the first gameplay frame.
        GreenScreen.initialize();
        net.lidl.lidlclient.cursor.MenuCursor.initialize();
        net.lidl.lidlclient.hud.PanelHost.initialize();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("lidlgui").executes(context -> {
                    Minecraft.getInstance().schedule(net.lidl.lidlclient.hud.PanelHost::openEditor);
                    return 1;
                })));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("lidl").executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    client.schedule(() -> client.setScreen(new ModuleScreen()));
                    return 1;
                })));
    }
}
