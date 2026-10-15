
package net.lidl.lidlclient.GuiTest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import net.lidl.lidlclient.LidlClient;

public class GuiTest implements ClientModInitializer {

    // Stores whether the circle should be visible
    private static boolean circleEnabled = false;

    @Override
    public void onInitializeClient() {

        // Register our /circle command
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> {

                    dispatcher.register(
                            ClientCommands.literal("circle").executes(context -> {

                                // Switch between true and false
                                circleEnabled = !circleEnabled;

                                // Send a message in Minecraft chat
                                context.getSource().sendFeedback(
                                        Component.literal(
                                                "Circle: " + (circleEnabled ? "ON" : "OFF")
                                        )
                                );

                                return 1;
                            })
                    );
                }
        );

        // Register our circle HUD renderer
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                LidlClient.id("circle_overlay"),
                GuiTest::renderCircle
        );
    }

    // Called every time the HUD is rendered
    private static void renderCircle(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker
    ) {

        // Don't draw anything if the circle is disabled
        if (!circleEnabled) {
            return;
        }

        int centerX = 100;
        int centerY = 100;
        int radius = 20;

        // Red with full opacity
        int color = 0xFFFF3333;

        // Draw the circle using horizontal rectangles
        for (int y = -radius; y <= radius; y++) {

            int halfWidth = (int) Math.sqrt(
                    radius * radius - y * y
            );

            graphics.fill(
                    centerX - halfWidth,
                    centerY + y,
                    centerX + halfWidth + 1,
                    centerY + y + 1,
                    color
            );
        }
    }
}
