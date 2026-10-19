package net.lidl.lidlclient.dungeons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.lidl.lidlclient.LidlClient;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

/** Settings are persisted by ModulePersistence through the standard Lidl GUI. */
public final class DungeonMap implements ClientModInitializer {
    private static boolean enabled = true, names = true, players = true, stats, darkenUndiscovered = true, secretSync=true, puzzlePrediction=true;
    private static String overlay = "Checkmarks", view = "Full scanned";
    private static double size = 190, x = 2, y = 15, textScale = 0.8, headScale = 1, darkness = 65;
    private static DungeonMapScanner scanner;
    private static int ticks;
    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) {
        enabled=value;
        if(!value) {ticks=0;if(scanner!=null) scanner.reset();}
    }
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
    public static String view() { return view; }
    public static void setView(String value) { view = value; }
    public static boolean fullMap() { return view.equals("Full scanned"); }
    public static boolean darkenUndiscovered() { return darkenUndiscovered; }
    public static void setDarkenUndiscovered(boolean value) { darkenUndiscovered = value; }
    public static boolean puzzlePrediction() {return puzzlePrediction;}
    public static void setPuzzlePrediction(boolean value) {puzzlePrediction=value;}
    public static boolean secretSync() {return secretSync;}
    public static void setSecretSync(boolean value) {secretSync=value;if(scanner!=null) scanner.sync().reset();}
    public static double darkness() { return darkness; }
    public static void setDarkness(double value) { darkness = value; }
    public static void recordSecrets(String actionBar) {
        if(!enabled) return;
        var client=Minecraft.getInstance();
        if(scanner==null || !scanner.active() || client.player==null) return;
        var count=DungeonSecretProgress.parse(actionBar);
        if(count==null) return;
        int x=(int)Math.round((client.player.getX()-DungeonMapModel.WORLD_START)/32.0)*2;
        int z=(int)Math.round((client.player.getZ()-DungeonMapModel.WORLD_START)/32.0)*2;
        scanner.model().updateSecrets(x,z,count.found(),count.total());
        scanner.sync().local(scanner,x,z);
    }
    public static void openSyncStatus() {var client=Minecraft.getInstance();client.setScreen(new DungeonRelayStatusScreen(client.screen));}
    public static DungeonMapScanner scanner() { return scanner; }
    public static void open() {
        if(!enabled||scanner!=null&&scanner.inBoss()) return;
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new DungeonMapScreen(client.screen));
    }

    @Override public void onInitializeClient() {
        scanner = new DungeonMapScanner();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client->scanner.sync().reset());
        ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            if(enabled&&overlay) recordSecrets(message.getString());
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(!enabled) return;
            if (++ticks % 5 == 0) scanner.tick(client);
            else if(scanner.active()) scanner.markers().update(client,scanner.party());
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, LidlClient.id("dungeon_map"), (graphics, delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (!enabled || !scanner.active() || client.options.hideGui || client.screen instanceof DungeonMapScreen) return;
            int extent = Math.max(80, Math.min((int) size, Math.min(graphics.guiWidth() - 16, graphics.guiHeight() - 45)));
            int left = (int) ((graphics.guiWidth() - extent) * x / 100);
            int panelHeight = (int) Math.ceil(extent * (stats ? 140.0 : 128.0) / 128.0);
            int top = (int) (Math.max(0, graphics.guiHeight() - panelHeight) * y / 100);
            DungeonMapRenderer.draw(graphics, scanner.model(), left, top, extent, names, players, scanner.floor());
        });
    }
}


