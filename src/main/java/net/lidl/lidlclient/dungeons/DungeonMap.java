package net.lidl.lidlclient.dungeons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.lidl.lidlclient.LidlClient;
import net.minecraft.client.Minecraft;

/** Settings are persisted by ModulePersistence through the standard Lidl GUI. */
public final class DungeonMap implements ClientModInitializer {
    private static boolean enabled = true, names = true, players = true, stats;
    private static String overlay = "Checkmarks";
    private static double size = 190, x = 2, y = 15, opacity = 20, textScale = 0.8, headScale = 1;
    private static DungeonMapScanner scanner;
    private static int ticks;
    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean names() { return names; }
    public static void setNames(boolean value) { names = value; }
    public static boolean players() { return players; }
    public static void setPlayers(boolean value) { players = value; }
    public static String overlay() { return overlay; }
    public static void setOverlay(String value) { overlay = value; }
    public static boolean stats() { return stats; }
    public static void setStats(boolean value) { stats = value; }
    public static double textScale() { return textScale; }
    public static void setTextScale(double value) { textScale = value; }
    public static double headScale() { return headScale; }
    public static void setHeadScale(double value) { headScale = value; }
    public static double size() { return size; }
    public static void setSize(double value) { size = value; }
    public static double x() { return x; }
    public static void setX(double value) { x = value; }
    public static double y() { return y; }
    public static void setY(double value) { y = value; }
    public static double opacity() { return opacity; }
    public static void setOpacity(double value) { opacity = value; }
    public static DungeonMapScanner scanner() { return scanner; }
    public static void open() {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new DungeonMapScreen(client.screen));
    }

    @Override public void onInitializeClient() {
        scanner = new DungeonMapScanner();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++ticks % 5 == 0) scanner.tick(client);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommands.literal("lidlmap").executes(context -> {
                    Minecraft.getInstance().schedule(DungeonMap::open); return 1;
                })));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, LidlClient.id("dungeon_map"), (graphics, delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (!enabled || !scanner.active() || client.options.hideGui || client.screen instanceof DungeonMapScreen) return;
            int extent = Math.max(80, Math.min((int) size, Math.min(graphics.guiWidth() - 16, graphics.guiHeight() - 45)));
            int left = (int) ((graphics.guiWidth() - extent) * x / 100);
            int panelHeight = (int) Math.ceil(extent * (stats ? 140.0 : 128.0) / 128.0);
            int top = (int) (Math.max(0, graphics.guiHeight() - panelHeight) * y / 100);
            DungeonMapRenderer.draw(graphics, scanner.model(), left, top, extent, names, players, opacity, scanner.floor());
        });
    }
}
