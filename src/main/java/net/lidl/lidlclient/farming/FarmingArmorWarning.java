package net.lidl.lidlclient.farming;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import net.lidl.lidlclient.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;

/** Local HUD warning; never sends chat, swaps armor, or requires an API key. */
public final class FarmingArmorWarning {
    private static final List<EquipmentSlot> ARMOR = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static boolean enabled = true;
    private static boolean initialized;
    private static int visibleTicks;

    private FarmingArmorWarning() { }
    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) visibleTicks = 0;
        ClientConfig.set("farming_armor_warning", "enabled", value);
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(FarmingArmorWarning::tick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("lidlclient", "farming_armor_warning"), (graphics, delta) -> {
                    Minecraft client = Minecraft.getInstance();
                    if (!enabled || visibleTicks == 0 || client.player == null
                            || client.screen != null) return;
                    String message = "WRONG ARMOR";
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(graphics.guiWidth() / 2f, graphics.guiHeight() / 2f);
                    graphics.pose().scale(2f, 2f);
                    graphics.text(client.font, message, -client.font.width(message) / 2,
                            -client.font.lineHeight / 2, 0xFFFF5555, true);
                    graphics.pose().popMatrix();
                });
    }

    private static void tick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null || client.screen != null
                || client.player.isSpectator() || !inGarden(client) || allowedArmor(client)) {
            visibleTicks = 0;
            return;
        }
        if (visibleTicks > 0) visibleTicks--;
        // Read held attack each tick so continuous harvesting is covered as well as the first click.
        if (client.options.keyAttack.isDown() && client.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK
                && crop(client.level.getBlockState(hit.getBlockPos()))) visibleTicks = 40;
    }

    private static boolean allowedArmor(Minecraft client) {
        List<String> ids = new ArrayList<>(4);
        for (EquipmentSlot slot : ARMOR) {
            var stack = client.player.getItemBySlot(slot);
            var data = stack.get(DataComponents.CUSTOM_DATA);
            ids.add(data == null ? "" : data.copyTag().getCompoundOrEmpty("ExtraAttributes")
                    .getStringOr("id", ""));
        }
        return FarmingArmorRules.allowedArmor(ids);
    }

    private static boolean inGarden(Minecraft client) {
        var server = client.getCurrentServer();
        var connection = client.getConnection();
        if (server == null || connection == null || !FarmingArmorRules.hypixel(server.ip)) return false;
        var scoreboard = client.level.getScoreboard();
        var objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return false;
        List<String> sidebar = scoreboard.listPlayerScores(objective).stream()
                .filter(score -> !score.isHidden())
                .map(score -> PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(score.owner()),
                        score.ownerName()).getString()).toList();
        List<String> tab = connection.getOnlinePlayers().stream()
                .map(info -> info.getTabListDisplayName())
                .filter(java.util.Objects::nonNull).map(name -> name.getString()).toList();
        return FarmingArmorRules.garden(objective.getDisplayName().getString(), sidebar, tab);
    }

    private static boolean crop(BlockState state) {
        return state.getBlock() instanceof CropBlock || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.NETHER_WART) || state.is(Blocks.COCOA)
                || state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)
                || state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.SUNFLOWER) || state.is(Blocks.BLUE_ORCHID)
                || state.is(Blocks.ROSE_BUSH)
                || state.is(Blocks.CACTUS);
    }
}
