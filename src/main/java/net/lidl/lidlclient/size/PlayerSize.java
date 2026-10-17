package net.lidl.lidlclient.size;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.loader.api.FabricLoader;
import net.lidl.lidlclient.LidlClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Client-only scaling with built-in player defaults and a local-only override. */
public final class PlayerSize implements ClientModInitializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("lidlclient-player-size.json");
    private static boolean loaded;
    private static boolean enabled = true;
    private static Double localScale;

    private static final class Config {
        boolean enabled = true;
        Double localScale;
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(CONFIG)) return;
        try {
            Config config = GSON.fromJson(Files.readString(CONFIG), Config.class);
            if (config == null) return;
            enabled = config.enabled;
            if (config.localScale != null && Double.isFinite(config.localScale)
                    && config.localScale >= PlayerSizeRules.MIN
                    && config.localScale <= PlayerSizeRules.MAX) localScale = config.localScale;
        } catch (IOException | RuntimeException exception) {
            LidlClient.LOGGER.warn("Could not load player-size settings", exception);
        }
    }

    private static void save() {
        Config config = new Config();
        config.enabled = enabled;
        config.localScale = localScale;
        try {
            Files.createDirectories(CONFIG.getParent());
            Path temporary = CONFIG.resolveSibling(CONFIG.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(config), StandardCharsets.UTF_8);
            Files.move(temporary, CONFIG, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LidlClient.LOGGER.warn("Could not save player-size settings", exception);
        }
    }

    public static boolean isEnabled() { load(); return enabled; }

    public static double getScale() {
        load();
        if (localScale != null) return localScale;
        var player = Minecraft.getInstance().player;
        return PlayerSizeRules.defaultScale(player == null ? null : player.getName().getString());
    }

    public static void setEnabled(boolean value) {
        load();
        enabled = value;
        save();
    }

    public static void setScale(double value) {
        if (!Double.isFinite(value)) return;
        load();
        localScale = Math.clamp(value, PlayerSizeRules.MIN, PlayerSizeRules.MAX);
        save();
    }

    public static float scaleFor(UUID id, String name) {
        load();
        var player = Minecraft.getInstance().player;
        return PlayerSizeRules.scaleFor(name, player != null && player.getUUID().equals(id), localScale, enabled);
    }

    @Override
    public void onInitializeClient() {
        load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("lidsize")
                        .executes(context -> {
                            context.getSource().sendFeedback(Component.literal(
                                    "Player size: /lidl > Player size (right-click for your slider). "
                                    + "Built-in sizes: SyntocxLeftFoot 0.5x; SyntocxRightFoot 3x. "
                                    + "/lidsize reset restores your default."));
                            return 1;
                        })
                        .then(ClientCommands.literal("reset").executes(context -> {
                            localScale = null;
                            save();
                            context.getSource().sendFeedback(Component.literal("Your player size override was reset."));
                            return 1;
                        }))));
    }
}
