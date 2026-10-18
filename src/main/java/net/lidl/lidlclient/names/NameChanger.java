package net.lidl.lidlclient.names;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lidl.lidlclient.config.ClientConfig;
import net.minecraft.client.Minecraft;
import java.util.*;

/** Saved custom-name switch. Built-in aliases always remain active. */
public final class NameChanger {
    private static boolean enabled = true;
    private static boolean initialized;
    private static boolean editing;
    private static String customName = "";
    private static String localUsername;
    private static NameAliasRules.Resolver resolver = new NameAliasRules.Resolver(null, "");

    private NameChanger() { }
    public static boolean isEnabled() { return enabled; }
    public static boolean isRenderingEnabled() { return !editing; }
    public static String customName() { return customName; }
    public static void setEditing(boolean value) { editing = value; }

    public static void initializeSettings() {
        if (initialized) return;
        initialized = true;
        customName = NameAliasRules.cleanCustomName(ClientConfig.string("name_changer", "self_name"));
        ClientLifecycleEvents.CLIENT_STARTED.register(NameChanger::updatePlayer);
        ClientTickEvents.END_CLIENT_TICK.register(NameChanger::updatePlayer);
    }

    private static void updatePlayer(Minecraft client) {
        String username = client.player == null ? client.getUser().getName() : client.player.getName().getString();
        if (!Objects.equals(username, localUsername)) {
            localUsername = username;
            resolver = new NameAliasRules.Resolver(localUsername, customName);
            refreshChat();
        }
    }

    public static List<NameAliasRules.Match> findMatches(String text) { return enabled ? resolver.find(text) : NameAliasRules.find(text); }

    public static void setCustomName(String value) {
        customName = NameAliasRules.cleanCustomName(value);
        ClientConfig.set("name_changer", "self_name", customName);
        resolver = new NameAliasRules.Resolver(localUsername, customName);
        refreshChat();
    }

    public static void openEditor() {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new SelfNameScreen(client.screen));
    }

    private static void refreshChat() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.gui != null) client.gui.getChat().rescaleChat();
    }

    public static void setEnabled(boolean value) {
        boolean changed = enabled != value;
        enabled = value;
        ClientConfig.set("name_changer", "enabled", value);
        if (changed) refreshChat();
    }
}
