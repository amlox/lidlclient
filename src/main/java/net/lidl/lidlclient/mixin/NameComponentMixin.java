package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.names.DisplayNames;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Return a visual copy; never mutate the source component or its cached text. */
@Mixin(MutableComponent.class)
public abstract class NameComponentMixin {
    @Inject(method = "getVisualOrderText", at = @At("RETURN"), cancellable = true)
    private void lidlclient$visualNames(CallbackInfoReturnable<FormattedCharSequence> cir) {
        cir.setReturnValue(DisplayNames.replace(cir.getReturnValue()));
    }
}
