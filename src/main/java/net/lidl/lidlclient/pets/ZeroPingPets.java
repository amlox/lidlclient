package net.lidl.lidlclient.pets;

import net.lidl.lidlclient.config.ClientConfig;

import net.fabricmc.api.ClientModInitializer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
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

/** Sends a server-only pet click without local inventory pickup, then closes immediately. */
public final class ZeroPingPets implements ClientModInitializer {
    private static boolean enabled = true;
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
                || client.getConnection() == null || client.screen != screen
                || !(screen.getMenu() instanceof ChestMenu chest) || !chest.getCarried().isEmpty()) return false;
        var stack = slot.getItem();
        var lore = stack.get(DataComponents.LORE);
        List<String> lines = lore == null ? List.of() : lore.lines().stream().map(line -> line.getString()).toList();
        if (!PetsMenuRules.isPetSelection(screen.getTitle().getString(),
                button == 0 && input == ContainerInput.PICKUP, stack.is(Items.PLAYER_HEAD),
                slotId >= 0 && slotId < chest.getRowCount() * 9, lines)) return false;

        GUARD.selected(System.nanoTime());
        selectedConnection = client.getConnection();
        // Hypixel interprets this as a menu selection. Do not run vanilla's local
        // pickup prediction or move the pet icon onto the cursor.
        selectedConnection.send(new ServerboundContainerClickPacket(
                chest.containerId, chest.getStateId(), (short) slotId, (byte) button, input,
                Int2ObjectMaps.<HashedStack>emptyMap(), HashedStack.EMPTY));
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
