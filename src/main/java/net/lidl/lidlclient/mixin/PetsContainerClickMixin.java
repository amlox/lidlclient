package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.pets.ZeroPingPets;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class PetsContainerClickMixin {
    @Shadow private Slot getHoveredSlot(double x, double y) { throw new AssertionError(); }

    // Consume the physical click before vanilla starts pickup/quick-craft or
    // records a pending mouse-release action.
    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
            at = @At("HEAD"), cancellable = true)
    private void lidlclient$selectPetImmediately(MouseButtonEvent event, boolean doubleClick,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0 || event.hasShiftDown()) return;
        Slot slot = getHoveredSlot(event.x(), event.y());
        if (slot != null && ZeroPingPets.beforeClick((AbstractContainerScreen<?>) (Object) this,
                slot, slot.index, 0, ContainerInput.PICKUP)) cir.setReturnValue(true);
    }

    // Also cover clicks dispatched directly by other client UI integrations.
    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"), cancellable = true)
    private void lidlclient$closePets(Slot slot, int slotId, int button, ContainerInput input, CallbackInfo ci) {
        if (ZeroPingPets.beforeClick((AbstractContainerScreen<?>) (Object) this, slot, slotId, button, input)) {
            ci.cancel();
        }
    }
}
