package net.lidl.lidlclient.gui;

import net.lidl.lidlclient.gui.settings.BooleanSetting;
import net.lidl.lidlclient.gui.settings.KeybindSetting;
import net.lidl.lidlclient.hotbar.DungeonbreakerSwap;
import net.lidl.lidlclient.gui.settings.ActionSetting;
import net.lidl.lidlclient.gui.settings.NumberSetting;
import net.lidl.lidlclient.size.PlayerSize;
import net.lidl.lidlclient.slots.Slots;
import net.lidl.lidlclient.PartyCMDS.PartyCommands;
import net.lidl.lidlclient.gui.settings.ModuleSetting;
import net.lidl.lidlclient.pets.ZeroPingPets;
import net.lidl.lidlclient.green.GreenScreen;

import java.util.ArrayList;
import java.util.List;

/** Register new modules here. Categories, positions and settings widgets are automatic. */
public final class ModuleRegistry {
    private ModuleRegistry() { }

    private static final List<GuiModule> MODULES = createModules();

    private static List<GuiModule> createModules() {
        List<GuiModule> modules = new ArrayList<>();
        modules.add(new GuiModule("slots", "Slots", "General", "Virtual slot machine",
                Slots::isEnabled, Slots::setEnabled,
                new BooleanSetting("Mute chat", Slots::isMuteMessages, Slots::setMuteMessages),
                new BooleanSetting("Bet / balance", Slots::isShowUi, Slots::setShowUi)));

        modules.add(new GuiModule("green_screen", "im so fucking green", "General",
                "Transparent green screen tint",
                GreenScreen::isEnabled, GreenScreen::setEnabled));

        modules.add(new GuiModule("dungeonbreaker_swap", "Dungeonbreaker swap", "General",
                "Select Dungeonbreaker from your hotbar",
                DungeonbreakerSwap::isEnabled, DungeonbreakerSwap::setEnabled,
                new KeybindSetting("Keybind", DungeonbreakerSwap::keyName, DungeonbreakerSwap::bindKey)));
        modules.add(new GuiModule("instant_pets", "Instant Pets close", "SkyBlock",
                "Close after pet selection and suppress automatic reopen",
                ZeroPingPets::isEnabled, ZeroPingPets::setEnabled));
        modules.add(new GuiModule("player_size", "Player size", "General",
                "Built-in sizes with your own X/Y/Z sliders",
                PlayerSize::isEnabled, PlayerSize::setEnabled,
                new NumberSetting("X (width)", 0.25, 3.0, 0.05,
                        PlayerSize::getScaleX, PlayerSize::setScaleX),
                new NumberSetting("Y (height)", 0.25, 3.0, 0.05,
                        PlayerSize::getScaleY, PlayerSize::setScaleY),
                new NumberSetting("Z (depth)", 0.25, 3.0, 0.05,
                        PlayerSize::getScaleZ, PlayerSize::setScaleZ),
                new ActionSetting("Reset to default", PlayerSize::resetSize)));

        // Party commands retain the existing controller placeholder until connected.
        modules.add(new GuiModule("party_commands", "PartyCMDS", "Social", "Party commands",
                PartyCommands::isEnabled, PartyCommands::setEnabled,
                PartyCommands.commandNames().stream().<ModuleSetting>map(command ->
                        new BooleanSetting(command,
                                () -> PartyCommands.isCommandConfigured(command),
                                value -> PartyCommands.setCommandEnabled(command, value))).toList()));

        // Temporary modules for testing scrolling.
        for (int i = 1; i <= 30; i++) {
            final boolean[] enabled = {false};
            final boolean[] option = {false};

            modules.add(new GuiModule(
                    "scroll_test_" + i,
                    "Test Module " + i,
                    "General",
                    "Temporary scrolling test",
                    () -> enabled[0],
                    value -> enabled[0] = value,
                    new BooleanSetting(
                            "Test setting",
                            () -> option[0],
                            value -> option[0] = value
                    )
            ));
        }

        return validate(modules);
    }

    public static List<GuiModule> modules() { return MODULES; }

    static List<GuiModule> validate(List<GuiModule> modules) {
        if (modules.stream().map(GuiModule::id).distinct().count() != modules.size()) {
            throw new IllegalArgumentException("Module ids must be unique");
        }
        return List.copyOf(modules);
    }
}
