package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.pets.ZeroPingPets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class PetsScreenMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void lidlclient$suppressPetsReopen(Screen next, CallbackInfo ci) {
        if (ZeroPingPets.suppressReopen(next)) ci.cancel();
    }
}
