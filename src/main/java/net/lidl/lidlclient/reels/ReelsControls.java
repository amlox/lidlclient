package net.lidl.lidlclient.reels;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.lidl.lidlclient.config.ClientConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public final class ReelsControls implements ClientModInitializer {
    private static final String MODULE = "reels_controls";
    private static final String[] LABELS = {"Next Reel", "Previous Reel", "Play / Pause"};
    private static final KeyMapping[] KEYS = new KeyMapping[3];
    private static final String[] SAVED = new String[3];
    private static boolean enabled, ready;
    private static ReelsBridge bridge;
    private static String status = "Disabled";
    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) {
        enabled = value;
        ClientConfig.set(MODULE, "enabled", value);
        if (bridge != null) { bridge.close(); bridge = null; }
        status = value ? "Waiting for client startup" : "Disabled";
        if (value && ready) start();
    }
    private static String token() {
        String token = ClientConfig.string(MODULE, "connectionCode");
        if (token == null || token.length() < 32) {
            token = UUID.randomUUID().toString();
            ClientConfig.set(MODULE, "connectionCode", token);
        }
        return token;
    }
    private static void start() {
        try { bridge = new ReelsBridge(18764, token()); status = "Bridge ready on 127.0.0.1:18764"; }
        catch (IOException error) { status = "Could not open port 18764. Close other Minecraft instances, then toggle again."; }
    }
    public static void copyCode() {
        Minecraft.getInstance().keyboardHandler.setClipboard(token());
        feedback("Connection code copied. Paste it in the browser extension.");
    }
    public static void showStatus() { feedback(status); }
    private static void feedback(String message) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(Component.literal("[Reels] " + message));
    }
    public static String keyName(int index) { return KEYS[index] == null ? "Unbound" : KEYS[index].getTranslatedKeyMessage().getString(); }
    public static void bind(int index, InputConstants.Key key) {
        if (KEYS[index] == null) return;
        KEYS[index].setKey(key); KEYS[index].setDown(false); KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        SAVED[index] = key.getName(); ClientConfig.set(MODULE, LABELS[index], SAVED[index]);
    }
    @Override public void onInitializeClient() {
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("lidlclient", "reels"));
        for (int i = 0; i < 3; i++) KEYS[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lidlclient.reels." + i, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category));
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            for (int i = 0; i < 3; i++) {
                String saved = ClientConfig.string(MODULE, LABELS[i]);
                if (saved != null) {
                    try { bind(i, InputConstants.getKey(saved)); } catch (IllegalArgumentException ignored) { }
                }
                SAVED[i] = KeyMappingHelper.getBoundKeyOf(KEYS[i]).getName();
            }
            ready = true;
            if (enabled) start();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> { if (bridge != null) bridge.close(); });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean active = ready && enabled && bridge != null && client.player != null
                    && client.screen == null && client.isWindowActive();
            if (!active && bridge != null) bridge.clear();
            for (int i = 0; i < 3; i++) {
                if (ready) {
                    String bound = KeyMappingHelper.getBoundKeyOf(KEYS[i]).getName();
                    if (!Objects.equals(SAVED[i], bound)) { SAVED[i] = bound; ClientConfig.set(MODULE, LABELS[i], bound); }
                }
                while (KEYS[i].consumeClick()) if (active) bridge.send(ReelsBridge.Command.values()[i]);
            }
        });
    }
}
