package net.lidl.lidlclient.cursor;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lidl.lidlclient.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.lidl.lidlclient.LidlClient;
import org.lwjgl.glfw.GLFW;

/** Watches existing menu hotkeys without registering competing key mappings. */
public final class MenuCursor {
    private static final String ID = "menu_cursor";
    private static final CursorWait WAIT = new CursorWait();
    private static final List<InputConstants.Key> KEYS = loadKeys();
    private static final Map<String,SlotOverlay> OVERLAYS = SlotOverlayStore.read(ClientConfig.store().get(ID,"slotOverlays"));
    private static boolean showOverlay = ClientConfig.bool(ID,"Show slot overlay",false);
    private static double overlayOpacity = savedOpacity();
    private static String activeKey;
    private static boolean enabled = ClientConfig.bool(ID,"enabled",false);
    private static double timeout = savedTimeout();
    private static ClientPacketListener connection;
    private static boolean initialized;
    private MenuCursor() { }

    private static List<InputConstants.Key> loadKeys() {
        List<InputConstants.Key> keys = new ArrayList<>();
        var saved = ClientConfig.store().get(ID,"keys");
        if (saved != null && saved.isJsonArray()) {
            for (var value : saved.getAsJsonArray()) {
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) continue;
                try {
                    var key = InputConstants.getKey(value.getAsString());
                    if (validKey(key) && !keys.contains(key)) keys.add(key);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        return keys;
    }
    private static boolean validKey(InputConstants.Key key) {
        return key != InputConstants.UNKNOWN && key.getType() != InputConstants.Type.MOUSE
                && !(key.getType() == InputConstants.Type.KEYSYM && key.getValue() == GLFW.GLFW_KEY_ESCAPE);
    }
    private static double savedOpacity() {
        Double saved = ClientConfig.number(ID,"Slot opacity (%)");
        return saved == null ? 30 : Math.clamp(saved,5,80);
    }
    public static boolean showOverlay() { return showOverlay; }
    public static void setShowOverlay(boolean value) {
        showOverlay=value;
        ClientConfig.set(ID,"Show slot overlay",value);
    }
    public static double overlayOpacity() { return overlayOpacity; }
    public static void setOverlayOpacity(double value) {
        if (!Double.isFinite(value)) return;
        overlayOpacity=Math.clamp(value,5,80);
        ClientConfig.set(ID,"Slot opacity (%)",overlayOpacity);
    }
    public static SlotOverlay overlay(InputConstants.Key key) {
        return OVERLAYS.getOrDefault(key.getName(),SlotOverlay.empty());
    }
    public static void setOverlay(InputConstants.Key key,SlotOverlay overlay) {
        if (!KEYS.contains(key)) return;
        OVERLAYS.put(key.getName(),overlay);
        ClientConfig.set(ID,"slotOverlays",OVERLAYS);
    }
    private static double savedTimeout() {
        Double saved = ClientConfig.number(ID,"Wait timeout (seconds)");
        return saved == null ? 1.5 : Math.clamp(saved,.25,5);
    }
    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) {
        enabled = value;
        ClientConfig.set(ID,"enabled",value);
        if (!value) cancelAndRestore();
    }
    public static double timeout() { return timeout; }
    public static void setTimeout(double value) {
        if (!Double.isFinite(value)) return;
        timeout = Math.clamp(value,.25,5);
        ClientConfig.set(ID,"Wait timeout (seconds)",timeout);
    }
    public static List<InputConstants.Key> keys() { return List.copyOf(KEYS); }
    public static void addKey(InputConstants.Key key) {
        if (validKey(key) && !KEYS.contains(key)) { KEYS.add(key); saveKeys(); }
    }
    public static void removeKey(int index) {
        if (index >= 0 && index < KEYS.size()) {
            OVERLAYS.remove(KEYS.remove(index).getName());
            ClientConfig.set(ID,"slotOverlays",OVERLAYS);
            saveKeys();
        }
    }
    private static void saveKeys() { ClientConfig.set(ID,"keys",KEYS.stream().map(InputConstants.Key::getName).toList()); }
    public static void openBindings() {
        var client = Minecraft.getInstance();
        client.setScreen(new MenuCursorKeysScreen(client.screen));
    }
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,LidlClient.id("menu_cursor_slots"),
                (graphics,deltaTracker) -> {
                    if (!showOverlay || !waitingInWorld() || activeKey == null) return;
                    var overlay=OVERLAYS.get(activeKey);
                    if (overlay==null) return;
                    for (var entry:overlay.slots().entrySet()) {
                        int slot=entry.getKey();
                        if (slot>=overlay.rows()*9) continue;
                        int x=SlotOverlayLayout.x(graphics.guiWidth(),slot);
                        int y=SlotOverlayLayout.y(graphics.guiHeight(),overlay.rows(),slot);
                        graphics.fill(x,y,x+SlotOverlayLayout.SIZE,y+SlotOverlayLayout.SIZE,
                                SlotOverlay.translucent(entry.getValue(),overlayOpacity));
                    }
                });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WAIT.pending()) return;
            var result = WAIT.update(System.nanoTime(),client.screen != null,
                    client.player != null && client.getConnection() == connection,client.isWindowActive());
            if (result == CursorWait.Result.RESTORE) restore(client);
            if (result != CursorWait.Result.WAITING) { connection = null; activeKey = null; }
        });
    }
    /** Called after normal keyboard dispatch, before subsequent mouse movement. */
    public static void keyPressed(long window, int action, KeyEvent event) {
        var client = Minecraft.getInstance();
        if (!enabled || action != GLFW.GLFW_PRESS || window != client.getWindow().handle()
                || !client.isWindowActive() || client.player == null || client.getConnection() == null
                || client.screen != null || !client.mouseHandler.isMouseGrabbed()) return;
        var key = event.key() == GLFW.GLFW_KEY_UNKNOWN
                ? InputConstants.Type.SCANCODE.getOrCreate(event.scancode())
                : InputConstants.Type.KEYSYM.getOrCreate(event.key());
        if (!KEYS.contains(key) || !WAIT.begin(System.nanoTime(),(long)(timeout * 1_000_000_000L))) return;
        connection = client.getConnection();
        activeKey = key.getName();
        client.mouseHandler.releaseMouse();
        clearMotion(client);
    }
    public static boolean waitingInWorld() {
        var client = Minecraft.getInstance();
        return enabled && WAIT.pending() && client.screen == null && client.player != null
                && client.getConnection() == connection && client.isWindowActive();
    }
    public static void screenChanged() {
        if (Minecraft.getInstance().screen != null) {
            WAIT.cancel();
            connection = null;
            activeKey = null;
        }
    }
    public static void cancelAndRestore() {
        var client = Minecraft.getInstance();
        boolean restore = client.screen == null && client.player != null && client.isWindowActive()
                && client.getConnection() == connection;
        if (WAIT.cancel() && restore) restore(client);
        connection = null;
        activeKey = null;
    }
    private static void restore(Minecraft client) {
        clearMotion(client);
        client.mouseHandler.grabMouse();
        clearMotion(client);
    }
    private static void clearMotion(Minecraft client) {
        ((CursorMouseAccess)client.mouseHandler).lidlclient$clearLookMotion();
        client.mouseHandler.setIgnoreFirstMove();
    }
}
