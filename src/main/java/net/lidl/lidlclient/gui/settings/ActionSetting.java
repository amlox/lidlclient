package net.lidl.lidlclient.gui.settings;

import java.util.Objects;

/** A one-shot GUI button rather than a stored value. */
public record ActionSetting(String label, Runnable action) implements ModuleSetting {
    public ActionSetting {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Button needs a label");
        Objects.requireNonNull(action);
    }

    public void activate() { action.run(); }
    @Override public String valueText() { return ""; }
    @Override public void change(int direction) { if (direction > 0) activate(); }
}
