package net.lidl.lidlclient.config;

import com.google.gson.JsonPrimitive;
import net.lidl.lidlclient.gui.GuiModule;
import net.lidl.lidlclient.gui.settings.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class ModulePersistenceTest {
    @TempDir Path directory;

    @Test void registeredGuiValuesRestoreAfterASecondLaunchAndActionsNeverRun() {
        Path file = directory.resolve("lidlclient.json");
        var store = new SettingsStore(file, message -> fail(message));
        var enabled = new AtomicBoolean(true);
        var option = new AtomicBoolean(true);
        var number = new AtomicReference<>(1.0);
        var choice = new AtomicReference<>("A");
        var actions = new AtomicInteger();
        GuiModule original = new GuiModule("test", "Test", "General", "", enabled::get, enabled::set,
                new BooleanSetting("Option", option::get, option::set),
                new NumberSetting("Scale", 0.25, 3, 0.05, number::get, number::set),
                new ChoiceSetting("Mode", List.of("A", "B"), choice::get, choice::set),
                new ActionSetting("Reset", () -> actions.incrementAndGet()));
        GuiModule bound = ModulePersistence.bind(original, store);
        bound.toggle();
        bound.settings().get(0).change(1);
        ((NumberSetting) bound.settings().get(1)).set(2);
        bound.settings().get(2).change(1);

        enabled.set(true); option.set(true); number.set(1.0); choice.set("A");
        ModulePersistence.bind(original, new SettingsStore(file, message -> fail(message)));
        assertFalse(enabled.get());
        assertFalse(option.get());
        assertEquals(2.0, number.get());
        assertEquals("B", choice.get());
        assertEquals(0, actions.get());
    }

    @Test void invalidTypesAreIgnoredAndNumbersAreClamped() {
        var store = new SettingsStore(directory.resolve("lidlclient.json"), message -> fail(message));
        store.set("test", "enabled", new JsonPrimitive("false"));
        store.set("test", "Option", new JsonPrimitive(123));
        store.set("test", "Scale", new JsonPrimitive(100));
        store.set("test", "Mode", new JsonPrimitive("Removed"));
        var enabled = new AtomicBoolean(true);
        var option = new AtomicBoolean(true);
        var number = new AtomicReference<>(1.0);
        var choice = new AtomicReference<>("A");
        ModulePersistence.bind(new GuiModule("test", "Test", "General", "", enabled::get, enabled::set,
                new BooleanSetting("Option", option::get, option::set),
                new NumberSetting("Scale", 0.25, 3, 0.05, number::get, number::set),
                new ChoiceSetting("Mode", List.of("A", "B"), choice::get, choice::set)), store);
        assertTrue(enabled.get()); assertTrue(option.get());
        assertEquals(3.0, number.get()); assertEquals("A", choice.get());
    }
}
