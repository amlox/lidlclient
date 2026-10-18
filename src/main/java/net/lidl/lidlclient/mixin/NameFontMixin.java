package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.names.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Align rendering and layout for component, preformatted and raw-string text. */
@Mixin(Font.class)
public abstract class NameFontMixin {
    @ModifyVariable(method = {
            "width(Lnet/minecraft/network/chat/FormattedText;)I",
            "split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",
            "splitIgnoringLanguage(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",
            "substrByWidth(Lnet/minecraft/network/chat/FormattedText;I)Lnet/minecraft/network/chat/FormattedText;",
            "wordWrapHeight(Lnet/minecraft/network/chat/FormattedText;I)I"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedText lidlclient$layoutNames(FormattedText text) { return DisplayNames.replace(text); }

    @ModifyVariable(method = {
            "width(Lnet/minecraft/util/FormattedCharSequence;)I",
            "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
            "drawInBatch8xOutline(Lnet/minecraft/util/FormattedCharSequence;FFIILorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence lidlclient$styledNames(FormattedCharSequence text) { return DisplayNames.replace(text); }

    @Inject(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void lidlclient$stringWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (NameChanger.isEnabled() && !NameAliasRules.find(text).isEmpty()) {
            cir.setReturnValue(((Font) (Object) this).width(DisplayNames.fromString(text)));
        }
    }

    @Inject(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), cancellable = true)
    private void lidlclient$stringRender(String text, float x, float y, int color, boolean shadow,
                                        int background, CallbackInfoReturnable<Font.PreparedText> cir) {
        if (NameChanger.isEnabled() && !NameAliasRules.find(text).isEmpty()) {
            cir.setReturnValue(((Font) (Object) this).prepareText(
                    DisplayNames.fromString(text), x, y, color, shadow, false, background));
        }
    }

    @Inject(method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
            at = @At("HEAD"), cancellable = true)
    private void lidlclient$rawWorldText(String text, float x, float y, int color, boolean shadow,
            Matrix4fc pose, MultiBufferSource buffers, Font.DisplayMode mode, int background, int light, CallbackInfo ci) {
        if (NameChanger.isEnabled() && !NameAliasRules.find(text).isEmpty()) {
            ((Font) (Object) this).drawInBatch(DisplayNames.fromString(text), x, y, color,
                    shadow, pose, buffers, mode, background, light);
            ci.cancel();
        }
    }
}
