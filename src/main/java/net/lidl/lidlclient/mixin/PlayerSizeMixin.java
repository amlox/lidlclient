package net.lidl.lidlclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.lidl.lidlclient.size.PlayerSize;
import net.lidl.lidlclient.size.PlayerSizeRenderState;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Model-local scaling only; entity attributes and collision stay unchanged. */
@Mixin(AvatarRenderer.class)
public abstract class PlayerSizeMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void lidlclient$extractSize(Avatar player, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        ((PlayerSizeRenderState) state).lidlclient$setAxes(
                PlayerSize.scalesFor(player.getUUID(), player.getName().getString()));
    }

    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("TAIL"))
    private void lidlclient$scaleAxes(AvatarRenderState state, PoseStack pose, CallbackInfo ci) {
        var axes = ((PlayerSizeRenderState) state).lidlclient$getAxes();
        pose.scale(axes.x(), axes.y(), axes.z());
    }
}
