package net.lidl.lidlclient.size;

import java.util.Locale;

/** Shared defaults shipped with every LidlClient build. */
public final class PlayerSizeRules {
    public static final double MIN = 0.25;
    public static final double MAX = 3.0;
    private PlayerSizeRules() { }

    public static double defaultScale(String name) {
        return defaultScale(name,true);
    }

    public static double defaultScale(String name,boolean builtIns) {
        return !builtIns || name == null ? 1 : BuiltInSizes.PLAYERS.getOrDefault(name.toLowerCase(Locale.ROOT), 1.0);
    }

    public static float scaleFor(String name, boolean localPlayer, Double localOverride, boolean enabled) {
        return scaleFor(name,localPlayer,localOverride,enabled,true);
    }

    public static float scaleFor(String name,boolean localPlayer,Double localOverride,boolean enabled,boolean builtIns) {
        if (enabled && localPlayer && localOverride != null && Double.isFinite(localOverride)
                && localOverride >= MIN && localOverride <= MAX) return localOverride.floatValue();
        return (float) defaultScale(name,builtIns);
    }

    public record Axes(float x, float y, float z) { }

    public static Axes axesFor(String name, boolean local, Double x, Double y, Double z, boolean enabled) {
        return axesFor(name,local,x,y,z,enabled,true);
    }

    public static Axes axesFor(String name,boolean local,Double x,Double y,Double z,boolean enabled,boolean builtIns) {
        return new Axes(scaleFor(name,local,x,enabled,builtIns),
                scaleFor(name,local,y,enabled,builtIns),scaleFor(name,local,z,enabled,builtIns));
    }

    public static Double migrateAxis(Double axis, Double legacy) {
        if (axis != null && Double.isFinite(axis) && axis >= MIN && axis <= MAX) return axis;
        if (legacy != null && Double.isFinite(legacy) && legacy >= MIN && legacy <= MAX) return legacy;
        return null;
    }
}
