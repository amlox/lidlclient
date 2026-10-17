package net.lidl.lidlclient.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.lidl.lidlclient.LidlClient;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/** The single settings file used by all LidlClient modules. */
public final class ClientConfig {
    private static final Gson JSON = new Gson();
    private static final SettingsStore STORE = new SettingsStore(
            FabricLoader.getInstance().getConfigDir().resolve("lidlclient.json"),
            message -> LidlClient.LOGGER.warn(message));

    private ClientConfig() { }
    public static SettingsStore store() { return STORE; }

    public static boolean bool(String module, String key, boolean fallback) {
        JsonElement value = STORE.get(module, key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                ? value.getAsBoolean() : fallback;
    }

    public static Double number(String module, String key) {
        JsonElement value = STORE.get(module, key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return null;
        double number = value.getAsDouble();
        return Double.isFinite(number) ? number : null;
    }

    public static String string(String module, String key) {
        JsonElement value = STORE.get(module, key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : null;
    }

    public static void set(String module, String key, Object value) {
        STORE.set(module, key, value == null ? null : JSON.toJsonTree(value));
    }

    public static void migrateLegacy(String module, String filename, Map<String, String> keys) {
        if (bool(module, "_legacyImported", false)) return;
        var file = FabricLoader.getInstance().getConfigDir().resolve(filename);
        if (!Files.exists(file)) return;
        try {
            JsonObject old = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            Map<String, JsonElement> migrated = new LinkedHashMap<>();
            keys.forEach((oldKey, newKey) -> {
                if (old.has(oldKey)) migrated.put(newKey, old.get(oldKey));
            });
            if (module.equals("party_commands") && old.has("commands") && old.get("commands").isJsonObject()) {
                old.getAsJsonObject("commands").entrySet().forEach(entry -> migrated.put(entry.getKey(), entry.getValue()));
            }
            if (module.equals("player_size") && old.has("localScale")) {
                for (String axis : new String[]{"X (width)", "Y (height)", "Z (depth)"}) {
                    if (!migrated.containsKey(axis) || migrated.get(axis).isJsonNull()) migrated.put(axis, old.get("localScale"));
                }
            }
            migrated.put("_legacyImported", new JsonPrimitive(true));
            STORE.migrate(module, migrated);
        } catch (Exception exception) {
            LidlClient.LOGGER.warn("Could not import legacy settings from {}", file, exception);
        }
    }
}
