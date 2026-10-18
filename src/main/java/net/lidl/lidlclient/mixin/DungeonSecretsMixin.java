package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.dungeons.DungeonMap;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Also handle the dedicated action-bar packet, not just overlay system chat. */
@Mixin(ClientPacketListener.class)
public abstract class DungeonSecretsMixin {
    @Inject(method="setActionBarText",at=@At("TAIL"))
    private void lidlclient$roomSecrets(ClientboundSetActionBarTextPacket packet,CallbackInfo ci) {
        DungeonMap.recordSecrets(packet.text().getString());
    }
}
