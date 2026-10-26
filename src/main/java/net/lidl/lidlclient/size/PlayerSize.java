package net.lidl.lidlclient.size;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

import java.util.UUID;
import java.util.Map;
import net.lidl.lidlclient.config.ClientConfig;
import net.lidl.lidlclient.config.BuiltInFeatures;

/** Client-only scaling with built-in player defaults and a local-only override. */
public final class PlayerSize implements ClientModInitializer {
    private static boolean loaded;
    private static boolean enabled = true;
    private static Double localX, localY, localZ;

    private static void load() {
        if (loaded) return;
        loaded = true;
        ClientConfig.migrateLegacy("player_size", "lidlclient-player-size.json", Map.of(
                "enabled", "enabled", "localX", "X (width)", "localY", "Y (height)", "localZ", "Z (depth)"));
        enabled = ClientConfig.bool("player_size", "enabled", true);
        localX = PlayerSizeRules.migrateAxis(ClientConfig.number("player_size", "X (width)"), null);
        localY = PlayerSizeRules.migrateAxis(ClientConfig.number("player_size", "Y (height)"), null);
        localZ = PlayerSizeRules.migrateAxis(ClientConfig.number("player_size", "Z (depth)"), null);
    }

    private static void save() {
        ClientConfig.set("player_size", "enabled", enabled);
        ClientConfig.set("player_size", "X (width)", localX);
        ClientConfig.set("player_size", "Y (height)", localY);
        ClientConfig.set("player_size", "Z (depth)", localZ);
    }

    public static boolean isEnabled() { load(); return enabled; }

    private static double ownAxis(Double value) {
        if (value != null) return value;
        var player = Minecraft.getInstance().player;
        return PlayerSizeRules.defaultScale(player == null ? null : player.getName().getString(),BuiltInFeatures.playerSizes());
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
                localX, localY, localZ, enabled,BuiltInFeatures.playerSizes());
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
