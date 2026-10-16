package net.lidl.lidlclient.gui;

import net.lidl.lidlclient.gui.settings.ModuleSetting;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** A declaration of a feature; state lives in the feature, never in the screen. */
public record GuiModule(String id, String name, String category, String description,
                        BooleanSupplier enabled, Consumer<Boolean> setEnabled,
                        List<ModuleSetting> settings) {
    public GuiModule {
        if (id == null || id.isBlank() || name == null || name.isBlank()
                || category == null || category.isBlank()) {
            throw new IllegalArgumentException("Module id, name and category must not be blank");
        }
        Objects.requireNonNull(description);
        Objects.requireNonNull(enabled);
        Objects.requireNonNull(setEnabled);
        settings = List.copyOf(settings);
    }

    public GuiModule(String id, String name, String category, String description,
                     BooleanSupplier enabled, Consumer<Boolean> setEnabled,
                     ModuleSetting... settings) {
        this(id, name, category, description, enabled, setEnabled, List.of(settings));
    }

    public boolean isEnabled() { return enabled.getAsBoolean(); }
    public void toggle() { setEnabled.accept(!isEnabled()); }
}
