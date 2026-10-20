package net.lidl.lidlclient.pets;

import net.lidl.lidlclient.config.ClientConfig;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

/** Sends a version-correct pet selection and clears pickup prediction before closing. */
public final class ZeroPingPets implements ClientModInitializer {
    private static boolean enabled = ClientConfig.bool("instant_pets", "enabled", true);
    private static final PetsMenuRules GUARD = new PetsMenuRules();
    private static ClientPacketListener selectedConnection;

    public static boolean isEnabled() { return enabled; }

    public static void setEnabled(boolean value) {
        enabled = value;
        ClientConfig.set("instant_pets", "enabled", value);
        reset();
    }

    private static void reset() {
        GUARD.reset();
        selectedConnection = null;
    }

    private static boolean onHypixel(Minecraft client) {
        var server = client.getCurrentServer();
        if (server == null) return false;
        String host = server.ip.toLowerCase(Locale.ROOT).split(":", 2)[0];
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }

    public static boolean beforeClick(AbstractContainerScreen<?> screen, Slot slot, int slotId,
                                  int button, ContainerInput input) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled || !onHypixel(client) || client.player == null || slot == null
                || client.getConnection() == null || client.gameMode == null || client.screen != screen
                || !(screen.getMenu() instanceof ChestMenu chest) || client.player.containerMenu != chest
                || !chest.getCarried().isEmpty()) return false;
        var stack = slot.getItem();
        var lore = stack.get(DataComponents.LORE);
        List<String> lines = lore == null ? List.of() : lore.lines().stream().map(line -> line.getString()).toList();
        if (!PetsMenuRules.isPetSelection(screen.getTitle().getString(),
                button == 0 && input == ContainerInput.PICKUP, stack.is(Items.PLAYER_HEAD),
                slotId >= 0 && slotId < chest.getRowCount() * 9, lines)) return false;

        GUARD.selected(System.nanoTime());
        selectedConnection = client.getConnection();
        // Use the game's packet/state-id/hashed-stack handling. Close in this same
        // click callback, so pickup prediction is never visible for a rendered frame.
        var selectedStack = stack.copy();
        client.gameMode.handleContainerInput(chest.containerId, slotId, button, input, client.player);
        // This is a server menu action, not a real pickup. Restore prediction before
        // closeContainer can return a carried pet icon to the player's inventory.
        slot.set(selectedStack);
        chest.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
        client.player.closeContainer();
        return true;
    }

    public static boolean suppressReopen(Screen next) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled || client.player == null || !onHypixel(client)
                || selectedConnection == null || selectedConnection != client.getConnection()) {
            reset();
            return false;
        }
        if (next == null) return false;
        if (!(next instanceof AbstractContainerScreen<?> container)
                || !PetsMenuRules.isPetsMenu(container.getTitle().getString())) {
            // Opening a different menu is deliberate navigation, not a pet refresh.
            reset();
            return false;
        }
        if (!GUARD.shouldSuppress(container.getTitle().getString(), System.nanoTime())) return false;
        // A server-opened container must also be acknowledged as closed to avoid desync.
        client.player.closeContainer();
        return true;
    }

    @Override
    public void onInitializeClient() {
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            String root = command.strip().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
            if (root.equals("pets") || root.equals("pet")) reset();
            return true;
        });
    }
}
