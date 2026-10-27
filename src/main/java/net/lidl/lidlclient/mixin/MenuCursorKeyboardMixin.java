package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.cursor.MenuCursor;
import net.lidl.lidlclient.input.RawKeyboard;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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
        } else if (window == Minecraft.getInstance().getWindow().handle()
                && action != GLFW.GLFW_RELEASE && MenuCursor.blockingInput()) {
            // Discard presses/repeats, but forward releases so no held key can stick.
            ci.cancel();
        }
    }
    // Chain the raw callback instead of injecting watched keys into Minecraft's keybind dispatch.
    @ModifyArg(method="setup",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/InputConstants;setupKeyboardCallbacks(Lcom/mojang/blaze3d/platform/Window;Lorg/lwjgl/glfw/GLFWKeyCallbackI;Lorg/lwjgl/glfw/GLFWCharCallbackI;Lorg/lwjgl/glfw/GLFWPreeditCallbackI;Lorg/lwjgl/glfw/GLFWIMEStatusCallbackI;)V"),index=1)
    private GLFWKeyCallbackI lidlclient$shareRawKeys(GLFWKeyCallbackI original) {
        return RawKeyboard.observe(original);
    }
}
