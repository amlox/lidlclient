package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.cursor.CursorMouseAccess;
import net.lidl.lidlclient.cursor.MenuCursor;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MenuCursorMouseMixin implements CursorMouseAccess {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Override public void lidlclient$clearLookMotion() {
        accumulatedDX = 0;
        accumulatedDY = 0;
    }
    @Inject(method="grabMouse()V",at=@At("HEAD"),cancellable=true)
    private void lidlclient$keepReleased(CallbackInfo ci) {
        if (MenuCursor.waitingInWorld()) ci.cancel();
    }
    @Inject(method="onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V",at=@At("HEAD"),cancellable=true)
    private void lidlclient$ignoreWorldClicks(long window,MouseButtonInfo button,int action,CallbackInfo ci) {
        // Let releases through so a button held before the hotkey cannot get stuck.
        if (action == GLFW.GLFW_PRESS && MenuCursor.waitingInWorld()) ci.cancel();
    }
}
