package net.lidl.lidlclient.size;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Client-only model scaling. Never changes entity attributes, collision or camera height. */
public final class PlayerSize implements ClientModInitializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("lidlclient-player-size.json");
    private static boolean loaded;
    private static boolean enabled;
    private static double localScale = 1;
    private static final Map<UUID, Double> SHARED = new HashMap<>();

    private static final class Config {
        boolean enabled;
        double localScale = 1;
        Map<String, Double> shared = new HashMap<>();
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(CONFIG)) return;
        try {
            Config config = GSON.fromJson(Files.readString(CONFIG), Config.class);
            if (config == null) return;
            enabled = config.enabled;
            if (Double.isFinite(config.localScale) && config.localScale >= SizeCode.MIN
                    && config.localScale <= SizeCode.MAX) localScale = config.localScale;
            if (config.shared != null) config.shared.forEach((id, size) -> {
                try {
                    SizeCode code = new SizeCode(UUID.fromString(id), size == null ? Double.NaN : size);
                    SHARED.put(code.player(), code.scale());
                } catch (IllegalArgumentException ignored) {
                    // Discard invalid entries without losing the rest of the config.
                }
            });
        } catch (IOException | RuntimeException exception) {
            LidlClient.LOGGER.warn("Could not load player-size settings", exception);
        }
    }

    private static void save() {
        Config config = new Config();
        config.enabled = enabled;
        config.localScale = localScale;
        SHARED.forEach((id, size) -> config.shared.put(id.toString(), size));
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
    public static double getScale() { load(); return localScale; }

    public static void setEnabled(boolean value) {
        load();
        enabled = value;
        save();
    }

    public static void setScale(double value) {
        if (!Double.isFinite(value)) return;
        load();
        localScale = Math.clamp(value, SizeCode.MIN, SizeCode.MAX);
        save();
    }

    public static float scaleFor(UUID player) {
        load();
        Minecraft client = Minecraft.getInstance();
        if (!enabled || client.player == null) return 1;
        return (float) (client.player.getUUID().equals(player)
                ? localScale : SHARED.getOrDefault(player, 1.0));
    }

    @Override
    public void onInitializeClient() {
        load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("lidsize")
                        .executes(context -> {
                            context.getSource().sendFeedback(Component.literal(
                                    "Player size: /lidl > Player size (right-click for slider). "
                                    + "/lidsize copy | import <code> | reset | clear"));
                            return 1;
                        })
                        .then(ClientCommands.literal("copy").executes(context -> {
                            Minecraft client = Minecraft.getInstance();
                            if (client.player == null) return 0;
                            String code = new SizeCode(client.player.getUUID(), isEnabled() ? getScale() : 1).encode();
                            client.keyboardHandler.setClipboard(code);
                            context.getSource().sendFeedback(Component.literal("Copied size code: " + code));
                            return 1;
                        }))
                        .then(ClientCommands.literal("import")
                                .then(ClientCommands.argument("code", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            try {
                                                SizeCode code = SizeCode.decode(StringArgumentType.getString(context, "code"));
                                                Minecraft client = Minecraft.getInstance();
                                                if (client.player == null) return 0;
                                                if (code.player().equals(client.player.getUUID())) {
                                                    context.getSource().sendError(Component.literal(
                                                            "Use your own size slider in /lidl. Import is for other players."));
                                                    return 0;
                                                }
                                                SHARED.put(code.player(), code.scale());
                                                enabled = true;
                                                save();
                                                context.getSource().sendFeedback(Component.literal(
                                                        "Imported " + code.scale() + "x size for " + code.player()
                                                                + ". Only visible on your client."));
                                                return 1;
                                            } catch (IllegalArgumentException exception) {
                                                context.getSource().sendError(Component.literal(exception.getMessage()));
                                                return 0;
                                            }
                                        })))
                        .then(ClientCommands.literal("reset").executes(context -> {
                            setScale(1);
                            context.getSource().sendFeedback(Component.literal("Your player size is now 1x."));
                            return 1;
                        }))
                        .then(ClientCommands.literal("clear").executes(context -> {
                            SHARED.clear();
                            save();
                            context.getSource().sendFeedback(Component.literal("Cleared imported player sizes."));
                            return 1;
                        }))));
    }
}
