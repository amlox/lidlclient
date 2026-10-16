package net.lidl.lidlclient.prank;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** A local imitation of the legacy Hypixel ban screen, shown after disconnecting. */
public final class FakeBanScreen extends Screen {
    private static final Identifier DIRT = Identifier.withDefaultNamespace("textures/block/dirt.png");
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private int textTop;

    public FakeBanScreen() {
        super(Component.literal("Failed to connect to the server"));
    }

    @Override
    protected void init() {
        lines.clear();
        add(Component.literal("Failed to connect to the server").withStyle(ChatFormatting.GRAY));
        add(Component.empty());
        add(Component.literal("You are permanently banned from this server!").withStyle(ChatFormatting.RED));
        add(Component.empty());
        add(Component.literal("Reason: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("Cheating through the use of unfair game advantages.").withStyle(ChatFormatting.WHITE)));
        add(Component.literal("Find out more: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("https://www.hypixel.net/appeal")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)));
        add(Component.empty());
        add(Component.literal("Ban ID: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("#F9DF549F").withStyle(ChatFormatting.WHITE)));
        add(Component.literal("Sharing your Ban ID may affect the processing of your appeal!").withStyle(ChatFormatting.GRAY));
        int blockHeight = lines.size() * font.lineHeight;
        textTop = Math.max(8, height / 2 - blockHeight / 2 - 20);
        int buttonWidth = Math.min(200, Math.max(1, width - 20));
        addRenderableWidget(Button.builder(Component.literal("Back to server list"), button -> onClose())
                .bounds((width - buttonWidth) / 2,
                        Math.min(height - 28, textTop + blockHeight + 20), buttonWidth, 20).build());
    }

    private void add(Component text) {
        if (text.getString().isEmpty()) lines.add(Component.empty().getVisualOrderText());
        else lines.addAll(font.split(text, Math.max(1, width - 32)));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, DIRT, 0, 0, 0.0f, 0.0f, width, height, 32, 32);
        graphics.fill(0, 0, width, height, 0xC0000000);
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            graphics.text(font, line, (width - font.width(line)) / 2,
                    textTop + i * font.lineHeight, 0xFFFFFFFF, true);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(new TitleScreen()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
