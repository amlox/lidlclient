package net.lidl.lidlclient.cursor;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lidl.lidlclient.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.lidl.lidlclient.input.RawKeyboard;
import net.lidl.lidlclient.input.SharedHotkeys;
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
    private static final Map<String,String> COMMANDS = loadCommands();
    private static final Map<String,SlotOverlay> OVERLAYS = SlotOverlayStore.read(ClientConfig.store().get(ID,"slotOverlays"));
    private static boolean showOverlay = ClientConfig.bool(ID,"Show slot overlay",false);
    private static double overlayOpacity = savedOpacity();
    private static final SlotOverlayRenderer RENDERER=new SlotOverlayRenderer();
    private static boolean simulateGui=ClientConfig.bool(ID,"Simulate container GUI",true);
    private static boolean showIcons=ClientConfig.bool(ID,"Show item icons",true);
    private static String activeKey;
    private static net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> observedScreen;
    private static String observedKey;
    private static ClientPacketListener observedConnection;
    private static final Map<String,MenuIconSnapshot> LAST_MENUS=new java.util.HashMap<>();
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
        return key != InputConstants.UNKNOWN
                && (key.getType()!=InputConstants.Type.MOUSE || key.getValue()>=0 && key.getValue()<=GLFW.GLFW_MOUSE_BUTTON_LAST)
                && !(key.getType() == InputConstants.Type.KEYSYM && key.getValue() == GLFW.GLFW_KEY_ESCAPE);
    }
    private static Map<String,String> loadCommands() {
        Map<String,String> commands=new java.util.HashMap<>();
        var saved=ClientConfig.store().get(ID,"commands");
        if (saved!=null && saved.isJsonObject()) saved.getAsJsonObject().entrySet().forEach(entry -> {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                try {
                    String command=MenuCommand.normalize(entry.getValue().getAsString());
                    if (!command.isEmpty()) commands.put(entry.getKey(),command);
                } catch (IllegalArgumentException ignored) { }
            }
        });
        return commands;
    }
    public static String command(InputConstants.Key key) { return COMMANDS.getOrDefault(key.getName(),""); }
    public static void setCommand(InputConstants.Key key,String value) {
        String command=MenuCommand.normalize(value);
        if (!KEYS.contains(key)) return;
        if (command.isEmpty()) COMMANDS.remove(key.getName());
        else COMMANDS.put(key.getName(),command);
        ClientConfig.set(ID,"commands",COMMANDS);
    }
    public static boolean simulateGui() { return simulateGui; }
    public static void setSimulateGui(boolean value) {
        simulateGui=value; ClientConfig.set(ID,"Simulate container GUI",value);
    }
    public static boolean showIcons() { return showIcons; }
    public static void setShowIcons(boolean value) {
        showIcons=value; ClientConfig.set(ID,"Show item icons",value);
    }
    public static MenuIconSnapshot lastMenu(InputConstants.Key key) {
        return LAST_MENUS.get(key.getName());
    }
    public static Map<Integer,String> inventoryIcons() {
        var player=Minecraft.getInstance().player;
        Map<Integer,String> icons=new java.util.LinkedHashMap<>();
        if (player!=null) for (int slot=0;slot<36;slot++) {
            String icon=SlotIcons.encode(player.getInventory().getItem(SlotEditorLayout.inventoryIndex(slot)));
            if (icon!=null) icons.put(slot,icon);
        }
        return Map.copyOf(icons);
    }
    /** Snapshot only the server menu associated with this hotkey, after its contents have loaded. */
    public static void beforeScreenChange() {
        var client=Minecraft.getInstance();
        if (observedScreen!=null && client.screen==observedScreen && client.getConnection()==observedConnection
                && KEYS.stream().anyMatch(key -> key.getName().equals(observedKey))) {
            Map<Integer,String> icons=new java.util.LinkedHashMap<>();
            int rows=((net.minecraft.world.inventory.ChestMenu)observedScreen.getMenu()).getRowCount();
            for (int slot=0;slot<rows*9;slot++) {
                String icon=SlotIcons.encode(observedScreen.getMenu().getSlot(slot).getItem());
                if (icon!=null) icons.put(slot,icon);
            }
            LAST_MENUS.put(observedKey,new MenuIconSnapshot(rows,icons));
        }
        observedScreen=null; observedKey=null; observedConnection=null;
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
            String removed=KEYS.remove(index).getName();
            OVERLAYS.remove(removed); LAST_MENUS.remove(removed);
            COMMANDS.remove(removed); ClientConfig.set(ID,"commands",COMMANDS);
            ClientConfig.set(ID,"slotOverlays",OVERLAYS);
            saveKeys();
        }
    }
    private static void saveKeys() {
        ClientConfig.set(ID,"keys",KEYS.stream().map(InputConstants.Key::getName).toList());
        syncHotkeys();
    }
    private static final List<String> HOTKEY_OWNERS = new ArrayList<>();
    private static void syncHotkeys() {
        // One listener per key retains its own slot-overlay identity. Rebinding invalidates queued events.
        HOTKEY_OWNERS.forEach(RawKeyboard.HOTKEYS::unbind);
        HOTKEY_OWNERS.clear();
        for (var key:KEYS) {
            var raw=key.getType()==InputConstants.Type.MOUSE ? SharedHotkeys.Key.mouse(key.getValue())
                    : key.getType()==InputConstants.Type.SCANCODE
                    ? SharedHotkeys.Key.scancode(key.getValue()) : SharedHotkeys.Key.symbol(key.getValue());
            String owner=ID+":"+key.getName();
            HOTKEY_OWNERS.add(owner);
            RawKeyboard.HOTKEYS.bind(owner,List.of(raw),() -> {
                var client=Minecraft.getInstance();
                return enabled && client.isWindowActive() && client.player!=null
                        && client.getConnection()!=null && client.screen==null && client.mouseHandler.isMouseGrabbed();
            },window -> keyPressed(window,key));
        }
    }
    public static void openBindings() {
        var client = Minecraft.getInstance();
        client.setScreen(new MenuCursorKeysScreen(client.screen));
    }
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        syncHotkeys();
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,LidlClient.id("menu_cursor_slots"),
                (graphics,deltaTracker) -> {
                    if (!waitingInWorld() || activeKey == null) return;
                    RENDERER.draw(graphics,OVERLAYS.getOrDefault(activeKey,SlotOverlay.empty()),
                            simulateGui,showOverlay,showIcons);
                });
        ClientTickEvents.END_CLIENT_TICK.register(MenuCursor::updateWait);
    }
    private static void updateWait(Minecraft client) {
        if (!WAIT.pending()) return;
        var result = WAIT.update(System.nanoTime(),client.screen != null,
                client.player != null && client.getConnection() == connection,client.isWindowActive());
        if (result == CursorWait.Result.RESTORE) restore(client);
        if (result != CursorWait.Result.WAITING) {
            ((CursorMouseAccess)client.mouseHandler).lidlclient$finishCursorRelease();
            connection = null; activeKey = null; RENDERER.clear();
        }
    }
    /** Called by the independent raw-input dispatcher; never consumes another mod's binding. */
    private static void keyPressed(long window,InputConstants.Key key) {
        var client=Minecraft.getInstance();
        if (!enabled || window!=client.getWindow().handle() || !client.isWindowActive()
                || client.player==null || client.getConnection()==null || client.screen!=null
                || !client.mouseHandler.isMouseGrabbed()
                || !WAIT.begin(System.nanoTime(),(long)(timeout*1_000_000_000L))) return;
        connection=client.getConnection();
        activeKey=key.getName();
        // Keep logical gameplay focus intact for other mods, while freeing the physical cursor.
        ((CursorMouseAccess)client.mouseHandler).lidlclient$releaseCursor();
        clearMotion(client);
        String command=command(key);
        if (!command.isEmpty()) connection.sendCommand(command);
    }
    public static void beforeMouseMovement() {
        updateWait(Minecraft.getInstance());
        RawKeyboard.HOTKEYS.dispatch();
        if (waitingInWorld()) clearMotion(Minecraft.getInstance());
    }
    public static boolean waitingInWorld() {
        var client = Minecraft.getInstance();
        return enabled && WAIT.pending() && client.screen == null && client.player != null
                && client.getConnection() == connection && client.isWindowActive();
    }
    public static void screenChanged() {
        var client=Minecraft.getInstance();
        if (client.screen != null) {
            if (WAIT.pending() && activeKey!=null && client.getConnection()==connection
                    && client.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen
                    && screen.getMenu() instanceof net.minecraft.world.inventory.ChestMenu) {
                observedScreen=screen; observedKey=activeKey; observedConnection=connection;
            }
            WAIT.cancel();
            ((CursorMouseAccess)Minecraft.getInstance().mouseHandler).lidlclient$finishCursorRelease();
            connection = null;
            activeKey = null;
            RENDERER.clear();
        }
    }
    public static void cancelAndRestore() {
        var client = Minecraft.getInstance();
        boolean restore = client.screen == null && client.player != null && client.isWindowActive()
                && client.getConnection() == connection;
        if (WAIT.cancel()) {
            if (restore) restore(client);
            else ((CursorMouseAccess)client.mouseHandler).lidlclient$finishCursorRelease();
        }
        connection = null;
        activeKey = null;
        RENDERER.clear();
    }
    private static void restore(Minecraft client) {
        ((CursorMouseAccess)client.mouseHandler).lidlclient$finishCursorRelease();
        clearMotion(client);
        client.mouseHandler.grabMouse();
        clearMotion(client);
    }
    private static void clearMotion(Minecraft client) {
        ((CursorMouseAccess)client.mouseHandler).lidlclient$clearLookMotion();
        client.mouseHandler.setIgnoreFirstMove();
    }
}
