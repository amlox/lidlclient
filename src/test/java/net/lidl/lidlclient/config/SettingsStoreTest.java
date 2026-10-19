package net.lidl.lidlclient.config;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SettingsStoreTest {
    @TempDir Path directory;

    @Test void roundTripsFalseNumbersStringsAndUnknownFields() throws Exception {
        Path file = directory.resolve("lidlclient.json");
        Files.writeString(file, "{\"version\":1,\"future\":{\"keep\":true},\"modules\":{}}");
        var store = new SettingsStore(file, message -> fail(message));
        store.set("slots", "enabled", new JsonPrimitive(false));
        store.set("size", "x", new JsonPrimitive(0.5));
        store.set("hotbar", "key", new JsonPrimitive("key.keyboard.g"));
        var restored = new SettingsStore(file, message -> fail(message));
        assertFalse(restored.get("slots", "enabled").getAsBoolean());
        assertEquals(0.5, restored.get("size", "x").getAsDouble());
        assertEquals("key.keyboard.g", restored.get("hotbar", "key").getAsString());
        assertTrue(JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("future").get("keep").getAsBoolean());
    }

    @Test void migrationsDoNotReplaceNewerChoicesAndResetRemovesOverrides() {
        Path file = directory.resolve("lidlclient.json");
        var store = new SettingsStore(file, message -> fail(message));
        store.set("size", "x", new JsonPrimitive(2));
        store.migrate("size", Map.of("x", new JsonPrimitive(0.5), "y", new JsonPrimitive(0.5)));
        assertEquals(2, store.get("size", "x").getAsInt());
        assertEquals(0.5, store.get("size", "y").getAsDouble());
        store.set("size", "x", null);
        assertNull(new SettingsStore(file, message -> fail(message)).get("size", "x"));
    }

    @Test void malformedFileIsBackedUpBeforeDefaultsCanBeSaved() throws Exception {
        Path file = directory.resolve("lidlclient.json");
        String broken = "{ not json";
        Files.writeString(file, broken);
        var warnings = new ArrayList<String>();
        var store = new SettingsStore(file, warnings::add);
        assertFalse(warnings.isEmpty());
        try (var paths = Files.list(directory)) {
            Path backup = paths.filter(path -> path.getFileName().toString().contains(".broken-")).findFirst().orElseThrow();
            assertEquals(broken, Files.readString(backup));
        }
        assertTrue(store.set("slots", "enabled", new JsonPrimitive(false)));
        assertNotNull(JsonParser.parseString(Files.readString(file)));
    }

    @Test void unsupportedVersionsAndFailedSavesPreserveExistingFile() throws Exception {
        Path file = directory.resolve("lidlclient.json");
        String future = "{\"version\":99,\"modules\":{}}";
        Files.writeString(file, future);
        var warnings = new ArrayList<String>();
        var unsupported = new SettingsStore(file, warnings::add);
        assertFalse(unsupported.set("slots", "enabled", new JsonPrimitive(false)));
        assertEquals(future, Files.readString(file));

        Files.writeString(file, "{\"version\":1,\"modules\":{}}");
        var store = new SettingsStore(file, warnings::add);
        String original = Files.readString(file);
        Files.createDirectory(directory.resolve("lidlclient.json.tmp"));
        assertFalse(store.set("slots", "enabled", new JsonPrimitive(false)));
        assertEquals(original, Files.readString(file));
    }

    @Test void readsReturnCopiesRatherThanExposingMutableState() {
        var store = new SettingsStore(directory.resolve("lidlclient.json"), message -> fail(message));
        JsonObject value = new JsonObject();
        value.addProperty("keep", true);
        store.set("future", "data", value);
        store.get("future", "data").getAsJsonObject().addProperty("keep", false);
        assertTrue(store.get("future", "data").getAsJsonObject().get("keep").getAsBoolean());
    }
    @Test void batchEditsRoundTripTogetherPreservingUnrelatedValuesAndNullableResets() throws Exception {
        Path file=directory.resolve("lidlclient.json");
        var store=new SettingsStore(file,message->fail(message));
        store.set("size","future",new JsonPrimitive("keep"));
        Map<String,JsonElement> edit=new LinkedHashMap<>();
        edit.put("enabled",new JsonPrimitive(false));edit.put("x",new JsonPrimitive(.5));edit.put("y",new JsonPrimitive(3));
        assertTrue(store.setAll("size",edit));
        var restored=new SettingsStore(file,message->fail(message));
        assertFalse(restored.get("size","enabled").getAsBoolean());assertEquals(.5,restored.get("size","x").getAsDouble());assertEquals(3,restored.get("size","y").getAsInt());
        edit.put("x",JsonNull.INSTANCE);edit.put("y",null);assertTrue(store.setAll("size",edit));
        restored=new SettingsStore(file,message->fail(message));assertNull(restored.get("size","x"));assertNull(restored.get("size","y"));assertEquals("keep",restored.get("size","future").getAsString());
        String unchanged=Files.readString(file);assertTrue(store.setAll("size",edit));assertEquals(unchanged,Files.readString(file));
    }
    @Test void failedBatchSaveDoesNotPartiallyReplaceTheOnDiskConfig() throws Exception {
        Path file=directory.resolve("lidlclient.json");var warnings=new ArrayList<String>();
        var store=new SettingsStore(file,warnings::add);store.set("map","first",new JsonPrimitive(1));String original=Files.readString(file);
        Files.createDirectory(directory.resolve("lidlclient.json.tmp"));
        assertFalse(store.setAll("map",Map.of("first",new JsonPrimitive(2),"second",new JsonPrimitive(3))));
        assertEquals(original,Files.readString(file));assertFalse(warnings.isEmpty());
    }
}
