package net.lidl.lidlclient;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import net.lidl.lidlclient.gui.ShizoStyleScreen;

public class LidlClientGui implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> {

                    dispatcher.register(
                            LiteralArgumentBuilder
                                    .<FabricClientCommandSource>literal("lidl")
                                    .executes(context -> {

                                        Minecraft client = Minecraft.getInstance();

                                        client.schedule(() -> {
                                            client.setScreen(new ShizoStyleScreen());
                                        });

                                        return 1;
                                    })
                    );
                }
        );
    }
}