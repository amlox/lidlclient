package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.size.PlayerSize;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adjust the render snapshot only; the server's entity state remains untouched. */
@Mixin(AvatarRenderer.class)
public abstract class PlayerSizeMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void lidlclient$applySize(Avatar player, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        state.scale *= PlayerSize.scaleFor(player.getUUID());
    }
}
