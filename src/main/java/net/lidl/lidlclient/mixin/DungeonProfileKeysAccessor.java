package net.lidl.lidlclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface DungeonProfileKeysAccessor {
    @Accessor("profileKeyPairManager") ProfileKeyPairManager lidlclient$profileKeys();
}
