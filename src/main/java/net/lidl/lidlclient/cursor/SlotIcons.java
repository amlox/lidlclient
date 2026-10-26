package net.lidl.lidlclient.cursor;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Store only chosen stacks, including server-provided SkyBlock components and head profiles. */
public final class SlotIcons {
    private SlotIcons() { }
    public static String encode(ItemStack stack) {
        var client=Minecraft.getInstance();
        if (stack.isEmpty() || client.level==null) return null;
        try {
            var ops=client.level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            return ItemStack.CODEC.encodeStart(ops,stack.copy()).result()
                    .map(Object::toString).filter(s -> s.length()<=65536).orElse(null);
        } catch (RuntimeException ignored) { return null; }
    }
    public static ItemStack decode(String saved) {
        var client=Minecraft.getInstance();
        if (saved==null || client.level==null) return ItemStack.EMPTY;
        try {
            var ops=client.level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            return ItemStack.CODEC.parse(ops,JsonParser.parseString(saved)).result().orElse(ItemStack.EMPTY);
        } catch (RuntimeException ignored) { return ItemStack.EMPTY; }
    }
}
