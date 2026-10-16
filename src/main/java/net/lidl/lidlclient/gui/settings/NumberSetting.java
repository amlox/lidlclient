package net.lidl.lidlclient.gui.settings;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public record NumberSetting(String label, double min, double max, double step,
                            DoubleSupplier getter, DoubleConsumer setter) implements ModuleSetting {
    public NumberSetting {
        if (label == null || label.isBlank() || !Double.isFinite(min)
                || !Double.isFinite(max) || !Double.isFinite(max - min) || max <= min
                || !Double.isFinite(step) || step <= 0) {
            throw new IllegalArgumentException("A number setting needs a label, finite range, and positive step");
        }
        Objects.requireNonNull(getter);
        Objects.requireNonNull(setter);
    }

    public double value() {
        double value = getter.getAsDouble();
        return Double.isFinite(value) ? Math.clamp(value, min, max) : min;
    }

    public void set(double value) {
        if (!Double.isFinite(value)) return;
        double clamped = Math.clamp(value, min, max);
        double snapped = min + Math.round((clamped - min) / step) * step;
        setter.accept(Math.clamp(snapped, min, max));
    }

    public double fraction() { return (value() - min) / (max - min); }
    public void setFraction(double fraction) {
        if (Double.isFinite(fraction)) set(min + Math.clamp(fraction, 0, 1) * (max - min));
    }
    @Override public void change(int direction) { set(value() + (direction < 0 ? -step : step)); }
    @Override public String valueText() {
        return BigDecimal.valueOf(value()).setScale(6, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }
}
