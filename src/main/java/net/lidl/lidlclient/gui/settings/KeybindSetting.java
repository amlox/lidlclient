package net.lidl.lidlclient.gui.settings;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** A keybind row; the screen captures the next keyboard or mouse input. */
public record KeybindSetting(String label, Supplier<String> getter,
                             Consumer<InputConstants.Key> setter) implements ModuleSetting {
    public KeybindSetting {
        Objects.requireNonNull(label);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(setter);
    }

    public void bind(InputConstants.Key key) { setter.accept(key); }
    @Override public String valueText() { return getter.get(); }
    @Override public void change(int direction) { }
}
