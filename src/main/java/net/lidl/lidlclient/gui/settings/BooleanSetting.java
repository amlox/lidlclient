package net.lidl.lidlclient.gui.settings;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public record BooleanSetting(String label, BooleanSupplier getter,
                             Consumer<Boolean> setter) implements ModuleSetting {
    public BooleanSetting {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Blank setting label");
        Objects.requireNonNull(getter);
        Objects.requireNonNull(setter);
    }

    public boolean value() { return getter.getAsBoolean(); }
    @Override public String valueText() { return value() ? "ON" : "OFF"; }
    @Override public void change(int direction) { setter.accept(!value()); }
}
