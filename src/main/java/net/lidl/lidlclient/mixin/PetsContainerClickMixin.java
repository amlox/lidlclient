package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.pets.ZeroPingPets;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class PetsContainerClickMixin {
    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("TAIL"))
    private void lidlclient$closePets(Slot slot, int slotId, int button, ContainerInput input, CallbackInfo ci) {
        ZeroPingPets.afterClick((AbstractContainerScreen<?>) (Object) this, slot, slotId, button, input);
    }
}
