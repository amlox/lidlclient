package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.names.*;
import net.minecraft.client.gui.Font;
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
            "prepare8xTextOutline(Lnet/minecraft/util/FormattedCharSequence;FFI)Lnet/minecraft/client/gui/Font$PreparedText;"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence lidlclient$styledNames(FormattedCharSequence text) { return DisplayNames.replace(text); }

    @Inject(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void lidlclient$stringWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (NameChanger.isRenderingEnabled() && !NameChanger.findMatches(text).isEmpty()) {
            cir.setReturnValue(((Font) (Object) this).width(DisplayNames.fromString(text)));
        }
    }

    @Inject(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), cancellable = true)
    private void lidlclient$stringRender(String text, float x, float y, int color, boolean shadow,
                                        int background, CallbackInfoReturnable<Font.PreparedText> cir) {
        if (NameChanger.isRenderingEnabled() && !NameChanger.findMatches(text).isEmpty()) {
            cir.setReturnValue(((Font) (Object) this).prepareText(
                    DisplayNames.fromString(text), x, y, color, shadow, false, background));
        }
    }

}
