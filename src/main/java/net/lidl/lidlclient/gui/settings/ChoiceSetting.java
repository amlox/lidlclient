package net.lidl.lidlclient.gui.settings;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record ChoiceSetting(String label, List<String> choices, Supplier<String> getter,
                            Consumer<String> setter) implements ModuleSetting {
    public ChoiceSetting {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Blank setting label");
        choices = List.copyOf(choices);
        if (choices.isEmpty() || choices.stream().anyMatch(String::isBlank)
                || choices.stream().distinct().count() != choices.size()) {
            throw new IllegalArgumentException("Choices must be nonempty, distinct labels");
        }
        Objects.requireNonNull(getter);
        Objects.requireNonNull(setter);
    }

    @Override public String valueText() { return getter.get(); }
    @Override public void change(int direction) {
        int current = choices.indexOf(getter.get());
        int next = current < 0 ? 0 : Math.floorMod(current + (direction < 0 ? -1 : 1), choices.size());
        setter.accept(choices.get(next));
    }
}
