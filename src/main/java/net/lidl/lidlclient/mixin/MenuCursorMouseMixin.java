package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.cursor.CursorMouseAccess;
import net.lidl.lidlclient.cursor.MenuCursor;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MenuCursorMouseMixin implements CursorMouseAccess {
    @org.spongepowered.asm.mixin.injection.ModifyArg(method="setup",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/InputConstants;setupMouseCallbacks(Lcom/mojang/blaze3d/platform/Window;Lorg/lwjgl/glfw/GLFWCursorPosCallbackI;Lorg/lwjgl/glfw/GLFWMouseButtonCallbackI;Lorg/lwjgl/glfw/GLFWScrollCallbackI;Lorg/lwjgl/glfw/GLFWDropCallbackI;)V"),index=2)
    private org.lwjgl.glfw.GLFWMouseButtonCallbackI lidlclient$shareRawMouse(org.lwjgl.glfw.GLFWMouseButtonCallbackI original) {
        return net.lidl.lidlclient.input.RawKeyboard.observeMouse(original);
    }
    @Shadow private boolean mouseGrabbed;
    @Shadow private double xpos;
    @Shadow private double ypos;
    @Unique private boolean lidlclient$cursorReleased;
    @Override public void lidlclient$releaseCursor() {
        if (lidlclient$cursorReleased) return;
        var window=Minecraft.getInstance().getWindow();
        xpos=window.getScreenWidth()/2.0;
        ypos=window.getScreenHeight()/2.0;
        lidlclient$cursorReleased=true;
        InputConstants.grabOrReleaseMouse(window,GLFW.GLFW_CURSOR_NORMAL,xpos,ypos);
    }
    @Override public boolean lidlclient$finishCursorRelease() {
        if (!lidlclient$cursorReleased) return false;
        lidlclient$cursorReleased=false;
        mouseGrabbed=false;
        lidlclient$clearLookMotion();
        return true;
    }
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Override public void lidlclient$clearLookMotion() {
        accumulatedDX = 0;
        accumulatedDY = 0;
    }
    @Inject(method="handleAccumulatedMovement()V",at=@At("HEAD"))
    private void lidlclient$finishKeyDispatch(CallbackInfo ci) {
        MenuCursor.beforeMouseMovement();
    }
    @Inject(method="turnPlayer(D)V",at=@At("HEAD"),cancellable=true)
    private void lidlclient$blockCameraLook(double time,CallbackInfo ci) {
        if (MenuCursor.waitingInWorld()) ci.cancel();
    }
    @Inject(method="releaseMouse()V",at=@At("HEAD"),cancellable=true)
    private void lidlclient$handoffWithoutWarp(CallbackInfo ci) {
        // The physical cursor is already free; preserve its position when the real GUI arrives.
        if (lidlclient$finishCursorRelease()) ci.cancel();
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
    @Inject(method="onScroll(JDD)V",at=@At("HEAD"),cancellable=true)
    private void lidlclient$ignoreWorldScroll(long window,double horizontal,double vertical,CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().handle() && MenuCursor.blockingInput()) ci.cancel();
    }
}
