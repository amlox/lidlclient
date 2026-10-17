package net.lidl.lidlclient.size;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.lidl.lidlclient.LidlClient;
import net.minecraft.client.Minecraft;

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
    private static Double localX, localY, localZ;

    private static final class Config {
        boolean enabled = true;
        Double localScale; // Legacy uniform-size config, migrated on load.
        Double localX, localY, localZ;
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(CONFIG)) return;
        try {
            Config config = GSON.fromJson(Files.readString(CONFIG), Config.class);
            if (config == null) return;
            enabled = config.enabled;
            localX = PlayerSizeRules.migrateAxis(config.localX, config.localScale);
            localY = PlayerSizeRules.migrateAxis(config.localY, config.localScale);
            localZ = PlayerSizeRules.migrateAxis(config.localZ, config.localScale);
        } catch (IOException | RuntimeException exception) {
            LidlClient.LOGGER.warn("Could not load player-size settings", exception);
        }
    }

    private static void save() {
        Config config = new Config();
        config.enabled = enabled;
        config.localX = localX;
        config.localY = localY;
        config.localZ = localZ;
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

    private static double ownAxis(Double value) {
        if (value != null) return value;
        var player = Minecraft.getInstance().player;
        return PlayerSizeRules.defaultScale(player == null ? null : player.getName().getString());
    }

    public static double getScaleX() { load(); return ownAxis(localX); }
    public static double getScaleY() { load(); return ownAxis(localY); }
    public static double getScaleZ() { load(); return ownAxis(localZ); }

    public static void setEnabled(boolean value) {
        load();
        enabled = value;
        save();
    }

    public static void setScaleX(double value) { setAxis(0, value); }
    public static void setScaleY(double value) { setAxis(1, value); }
    public static void setScaleZ(double value) { setAxis(2, value); }

    private static void setAxis(int axis, double value) {
        if (!Double.isFinite(value)) return;
        load();
        double scale = Math.clamp(value, PlayerSizeRules.MIN, PlayerSizeRules.MAX);
        switch (axis) {
            case 0 -> localX = scale;
            case 1 -> localY = scale;
            case 2 -> localZ = scale;
            default -> throw new IllegalArgumentException("Unknown axis");
        }
        save();
    }

    public static PlayerSizeRules.Axes scalesFor(UUID id, String name) {
        load();
        var player = Minecraft.getInstance().player;
        return PlayerSizeRules.axesFor(name, player != null && player.getUUID().equals(id),
                localX, localY, localZ, enabled);
    }

    public static void resetSize() {
        load();
        localX = localY = localZ = null;
        save();
    }

    @Override
    public void onInitializeClient() {
        load();
    }
}
