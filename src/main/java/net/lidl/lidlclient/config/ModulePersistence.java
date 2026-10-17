package net.lidl.lidlclient.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.lidl.lidlclient.gui.GuiModule;
import net.lidl.lidlclient.gui.settings.*;
import java.util.ArrayList;
import java.util.List;

/** Automatically persists registered value settings; action buttons are never replayed. */
public final class ModulePersistence {
    private ModulePersistence() { }

    public static GuiModule bind(GuiModule module, SettingsStore store) {
        String id = module.id();
        JsonElement enabled = store.get(id, "enabled");
        if (isBoolean(enabled)) module.setEnabled().accept(enabled.getAsBoolean());
        List<ModuleSetting> settings = new ArrayList<>();
        for (ModuleSetting setting : module.settings()) {
            // Player-size axes have nullable overrides: their own config adapter preserves
            // unset defaults rather than converting a default into an explicit override.
            if (id.equals("player_size") || setting instanceof KeybindSetting || setting instanceof ActionSetting) {
                settings.add(setting);
                continue;
            }
            JsonElement value = store.get(id, setting.label());
            if (setting instanceof BooleanSetting bool) {
                if (isBoolean(value)) bool.setter().accept(value.getAsBoolean());
                settings.add(new BooleanSetting(bool.label(), bool.getter(), next -> {
                    bool.setter().accept(next);
                    store.set(id, bool.label(), new JsonPrimitive(next));
                }));
            } else if (setting instanceof NumberSetting number) {
                if (isNumber(value) && Double.isFinite(value.getAsDouble())) number.set(value.getAsDouble());
                settings.add(new NumberSetting(number.label(), number.min(), number.max(), number.step(),
                        number.getter(), next -> {
                            number.set(next);
                            store.set(id, number.label(), new JsonPrimitive(number.value()));
                        }));
            } else if (setting instanceof ChoiceSetting choice) {
                if (isString(value) && choice.choices().contains(value.getAsString())) choice.setter().accept(value.getAsString());
                settings.add(new ChoiceSetting(choice.label(), choice.choices(), choice.getter(), next -> {
                    choice.setter().accept(next);
                    store.set(id, choice.label(), new JsonPrimitive(next));
                }));
            }
        }
        return new GuiModule(id, module.name(), module.category(), module.description(), module.enabled(), next -> {
            module.setEnabled().accept(next);
            store.set(id, "enabled", new JsonPrimitive(next));
        }, settings);
    }

    private static boolean isBoolean(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
    }
    private static boolean isNumber(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
    }
    private static boolean isString(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }
}
