package net.lidl.lidlclient.config;

import net.lidl.lidlclient.names.NameChanger;

/** Controls shipped player presets and the built-in command shortcut independently of self overrides. */
public final class BuiltInFeatures {
    private static final String ID="built_in_features";
    private static boolean enabled=ClientConfig.bool(ID,"enabled",true);
    private static boolean sizes=ClientConfig.bool(ID,"Player sizes",true);
    private static boolean names=ClientConfig.bool(ID,"Name aliases",true);
    private static boolean warp=ClientConfig.bool(ID,"Warp shortcut",true);
    private BuiltInFeatures() { }
    public static boolean isEnabled() { return enabled; }
    public static boolean sizes() { return sizes; }
    public static boolean names() { return names; }
    public static boolean warp() { return warp; }
    public static boolean playerSizes() { return enabled && sizes; }
    public static boolean nameAliases() { return enabled && names; }
    public static boolean warpShortcut() { return enabled && warp; }
    public static void setEnabled(boolean value) {
        enabled=value; ClientConfig.set(ID,"enabled",value); NameChanger.refreshBuiltIns();
    }
    public static void setSizes(boolean value) { sizes=value; ClientConfig.set(ID,"Player sizes",value); }
    public static void setNames(boolean value) {
        names=value; ClientConfig.set(ID,"Name aliases",value); NameChanger.refreshBuiltIns();
    }
    public static void setWarp(boolean value) { warp=value; ClientConfig.set(ID,"Warp shortcut",value); }
}
