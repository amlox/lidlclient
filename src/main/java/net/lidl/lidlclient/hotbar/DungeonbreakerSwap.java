package net.lidl.lidlclient.hotbar;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.lidl.lidlclient.LidlClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** One manual hotkey press selects Dungeonbreaker; no attacks or automatic swapping. */
public final class DungeonbreakerSwap implements ClientModInitializer {
    private static KeyMapping key;
    private static boolean enabled = true;

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }

    public static String keyName() {
        return key == null ? "Unbound" : key.getTranslatedKeyMessage().getString();
    }

    public static void bindKey(InputConstants.Key input) {
        if (key == null) return;
        key.setKey(input);
        key.setDown(false);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(LidlClient.id("hotbar"));
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lidlclient.dungeonbreaker_swap", InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(), category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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
