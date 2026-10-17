package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.size.PlayerSizeRenderState;
import net.lidl.lidlclient.size.PlayerSizeRules;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class PlayerSizeStateMixin implements PlayerSizeRenderState {
    @Unique private PlayerSizeRules.Axes lidlclient$axes = new PlayerSizeRules.Axes(1, 1, 1);

    @Override
    public PlayerSizeRules.Axes lidlclient$getAxes() { return lidlclient$axes; }

    @Override
    public void lidlclient$setAxes(PlayerSizeRules.Axes axes) { lidlclient$axes = axes; }
}
