package net.lidl.lidlclient.config;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.function.Consumer;

/** Versioned JSON storage with unknown-field preservation and replace-after-write saves. */
public final class SettingsStore {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final Consumer<String> warning;
    private JsonObject root = fresh();
    private boolean writable = true;

    public SettingsStore(Path file, Consumer<String> warning) {
        this.file = file;
        this.warning = warning;
        if (!Files.exists(file)) return;
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file));
            if (!parsed.isJsonObject()) throw new IllegalArgumentException("Expected a config object");
            JsonObject candidate = parsed.getAsJsonObject();
            JsonElement version = candidate.get("version");
            if (version == null || !version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()
                    || version.getAsDouble() != 1) {
                writable = false;
                warning.accept("Unsupported settings version; keeping the file unchanged: " + file);
                return;
            }
            if (!candidate.has("modules") || !candidate.get("modules").isJsonObject()) {
                throw new IllegalArgumentException("Missing modules object");
            }
            root = candidate;
        } catch (IOException exception) {
            writable = false;
            warning.accept("Could not read settings; the file will not be overwritten: " + exception.getMessage());
        } catch (RuntimeException exception) {
            try {
                Path backup = file.resolveSibling(file.getFileName() + ".broken-" + System.currentTimeMillis());
                Files.copy(file, backup);
                warning.accept("Malformed settings backed up to " + backup);
            } catch (IOException backupFailure) {
                writable = false;
                warning.accept("Could not back up malformed settings; preserving the original file.");
            }
        }
    }

    private static JsonObject fresh() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("modules", new JsonObject());
        return root;
    }

    public synchronized JsonElement get(String module, String key) {
        JsonElement section = root.getAsJsonObject("modules").get(module);
        if (section == null || !section.isJsonObject()) return null;
        JsonElement value = section.getAsJsonObject().get(key);
        return value == null ? null : value.deepCopy();
    }

    public synchronized boolean set(String module, String key, JsonElement value) {
        JsonObject modules = root.getAsJsonObject("modules");
        JsonElement existing = modules.get(module);
        JsonObject section = existing != null && existing.isJsonObject() ? existing.getAsJsonObject() : new JsonObject();
        JsonElement previous = section.get(key);
        if (value == null || value.isJsonNull()) {
            if (previous == null) return true;
            section.remove(key);
        } else {
            if (value.equals(previous)) return true;
            section.add(key, value.deepCopy());
        }
        modules.add(module, section);
        return save();
    }

    public synchronized void migrate(String module, Map<String, JsonElement> values) {
        JsonObject modules = root.getAsJsonObject("modules");
        JsonElement existing = modules.get(module);
        JsonObject section = existing != null && existing.isJsonObject() ? existing.getAsJsonObject() : new JsonObject();
        boolean changed = false;
        for (var entry : values.entrySet()) {
            if (!section.has(entry.getKey()) && entry.getValue() != null && !entry.getValue().isJsonNull()) {
                section.add(entry.getKey(), entry.getValue().deepCopy());
                changed = true;
            }
        }
        if (changed) {
            modules.add(module, section);
            save();
        }
    }

    private boolean save() {
        if (!writable) return false;
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, JSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            warning.accept("Could not save settings: " + exception.getMessage());
            return false;
        }
    }
}
