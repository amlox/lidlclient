package net.lidl.lidlclient.mixin;

import net.lidl.lidlclient.names.DisplayNames;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Replace before wrapping so names are not split into fragments in cached chat lines. */
@Mixin(StringSplitter.class)
public abstract class NameSplitterMixin {
    @ModifyVariable(method = {
            "splitLines(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/network/chat/Style;)Ljava/util/List;",
            "splitLines(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/network/chat/Style;Ljava/util/function/BiConsumer;)V"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedText lidlclient$wrapNames(FormattedText text) { return DisplayNames.replace(text); }
}
