package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.cursor.MenuCursor;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class MenuCursorKeyboardMixin {
    @Inject(method="keyPress(JILnet/minecraft/client/input/KeyEvent;)V",at=@At("HEAD"),cancellable=true)
    private void lidlclient$cancelWait(long window,int action,KeyEvent event,CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().handle() && action == GLFW.GLFW_PRESS
                && event.isEscape() && MenuCursor.waitingInWorld()) {
            MenuCursor.cancelAndRestore();
            ci.cancel();
        }
    }
    // Observe the key without consuming it, after vanilla queues other mods' keybinds.
    @Inject(method="keyPress(JILnet/minecraft/client/input/KeyEvent;)V",at=@At("RETURN"))
    private void lidlclient$releaseForMenu(long window,int action,KeyEvent event,CallbackInfo ci) {
        MenuCursor.keyPressed(window,action,event);
    }
}
