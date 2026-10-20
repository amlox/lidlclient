package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.cursor.MenuCursor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MenuCursorScreenMixin {
    @Inject(method="setScreen",at=@At("TAIL"))
    private void lidlclient$handoffCursor(Screen next,CallbackInfo ci) {
        MenuCursor.screenChanged();
    }
}
