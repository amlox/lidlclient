package net.lidl.lidlclient.hotbar;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.lidl.lidlclient.LidlClient;
import net.lidl.lidlclient.config.ClientConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** One manual hotkey press selects Dungeonbreaker; no attacks or automatic swapping. */
public final class DungeonbreakerSwap implements ClientModInitializer {
    private static KeyMapping key;
    private static boolean enabled = true;
    private static boolean keysReady;
    private static String lastSavedKey;

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; ClientConfig.set("dungeonbreaker_swap", "enabled", value); }

    public static String keyName() {
        return key == null ? "Unbound" : key.getTranslatedKeyMessage().getString();
    }

    public static void bindKey(InputConstants.Key input) {
        if (key == null) return;
        key.setKey(input);
        key.setDown(false);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        lastSavedKey = input.getName();
        ClientConfig.set("dungeonbreaker_swap", "Keybind", lastSavedKey);
    }

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(LidlClient.id("hotbar"));
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lidlclient.dungeonbreaker_swap", InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(), category));

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            String saved = ClientConfig.string("dungeonbreaker_swap", "Keybind");
            if (saved != null) {
                try { bindKey(InputConstants.getKey(saved)); }
                catch (IllegalArgumentException exception) { LidlClient.LOGGER.warn("Invalid saved Dungeonbreaker key: {}", saved); }
            }
            lastSavedKey = KeyMappingHelper.getBoundKeyOf(key).getName();
            ClientConfig.set("dungeonbreaker_swap", "Keybind", lastSavedKey);
            keysReady = true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keysReady) {
                String current = KeyMappingHelper.getBoundKeyOf(key).getName();
                if (!Objects.equals(current, lastSavedKey)) {
                    lastSavedKey = current;
                    ClientConfig.set("dungeonbreaker_swap", "Keybind", current);
                }
            }
            while (key.consumeClick()) {
                if (!enabled || client.player == null || client.screen != null) continue;
                var inventory = client.player.getInventory();
                List<String> names = new ArrayList<>(9);
                for (int slot = 0; slot < 9; slot++) {
                    var item = inventory.getItem(slot);
                    names.add(item.isEmpty() ? "" : item.getHoverName().getString());
                }
                int slot = DungeonbreakerSlots.find(names, inventory.getSelectedSlot());
                if (slot < 0) {
                    client.gui.setOverlayMessage(Component.literal(
                            "Dungeonbreaker is not in your hotbar."), false);
                    continue;
                }
                inventory.setSelectedSlot(slot);
            }
        });
    }
}
