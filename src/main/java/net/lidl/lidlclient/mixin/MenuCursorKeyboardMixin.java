package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.cursor.MenuCursor;
import net.lidl.lidlclient.input.RawKeyboard;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class MenuCursorKeyboardMixin {
    @Inject(method="keyPress(JILnet/minecraft/client/input/KeyEvent;)V",at=@At("HEAD"),cancellable=true)
    private void lidlclient$cancelWait(long window,int action,KeyEvent event,CallbackInfo ci) {
        RawKeyboard.observe(window, event.key(), action);
        if (window == Minecraft.getInstance().getWindow().handle() && action == InputConstants.PRESS
                && event.isEscape() && MenuCursor.waitingInWorld()) {
            MenuCursor.cancelAndRestore();
            ci.cancel();
        } else if (window == Minecraft.getInstance().getWindow().handle()
                && action != InputConstants.RELEASE && MenuCursor.blockingInput()) {
            // Discard presses/repeats, but forward releases so no held key can stick.
            ci.cancel();
        }
    }
}
