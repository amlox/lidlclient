package net.lidl.lidlclient.names;

import net.lidl.lidlclient.config.ClientConfig;
import net.minecraft.client.Minecraft;

/** Saved display-only module switch. */
public final class NameChanger {
    private static boolean enabled = true;
    private NameChanger() { }
    public static boolean isEnabled() { return enabled; }

    public static void setEnabled(boolean value) {
        boolean changed = enabled != value;
        enabled = value;
        ClientConfig.set("name_changer", "enabled", value);
        if (changed) {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.gui != null) client.gui.getChat().rescaleChat();
        }
    }
}
