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

import net.lidl.lidlclient.config.ClientConfig;
import net.lidl.lidlclient.config.ModulePersistence;
import net.lidl.lidlclient.names.NameChanger;
import net.lidl.lidlclient.dungeons.DungeonMap;
import net.lidl.lidlclient.dungeons.DungeonMapColors;
import net.lidl.lidlclient.gui.settings.ChoiceSetting;
import java.util.ArrayList;
import java.util.List;

/** Register new modules here. Categories, positions and settings widgets are automatic. */
public final class ModuleRegistry {
    private ModuleRegistry() { }

    public static final List<String> CATEGORIES = List.of(
            "General settings", "Gambling", "QoL features", "Fun features", "Cheats");

    private static final List<GuiModule> MODULES = createModules();

    private static List<GuiModule> createModules() {
        NameChanger.initializeSettings();
        DungeonMapColors.initialize();
        List<GuiModule> modules = new ArrayList<>();
        modules.add(new GuiModule("dungeon_map", "Dungeon map", "QoL features",
                "Scanned rooms, names and clear states (Noamm based)",
                DungeonMap::isEnabled, DungeonMap::setEnabled,
                new ChoiceSetting("Map view", List.of("Full scanned", "Entered rooms"), DungeonMap::view, DungeonMap::setView),
                new BooleanSetting("Darken undiscovered rooms", DungeonMap::darkenUndiscovered, DungeonMap::setDarkenUndiscovered),
                new NumberSetting("Undiscovered darkness (%)", 0, 100, 5, DungeonMap::darkness, DungeonMap::setDarkness),
                new ActionSetting("Room / door colours", DungeonMapColors::open),
                new BooleanSetting("Party secret sync (chat)", DungeonMap::partySync, DungeonMap::setPartySync),
                new BooleanSetting("Room overlays", DungeonMap::names, DungeonMap::setNames),
                new ChoiceSetting("Room style", List.of("Checkmarks", "Room names", "Names + secrets"), DungeonMap::overlay, DungeonMap::setOverlay),
                new BooleanSetting("Player markers", DungeonMap::players, DungeonMap::setPlayers),
                new BooleanSetting("Map stats", DungeonMap::stats, DungeonMap::setStats),
                new NumberSetting("Room text scale", 0.4, 1.2, 0.1, DungeonMap::textScale, DungeonMap::setTextScale),
                new NumberSetting("Player head scale", 0.5, 1.5, 0.1, DungeonMap::headScale, DungeonMap::setHeadScale),
                new NumberSetting("Map size", 100, 360, 5, DungeonMap::size, DungeonMap::setSize),
                new NumberSetting("Map X (%)", 0, 100, 1, DungeonMap::x, DungeonMap::setX),
                new NumberSetting("Map Y (%)", 0, 100, 1, DungeonMap::y, DungeonMap::setY),
                new ActionSetting("Open dungeon map", DungeonMap::open)));
        modules.add(new GuiModule("slots", "Slots", "Gambling", "Virtual slot machine",
                Slots::isEnabled, Slots::setEnabled,
                new BooleanSetting("Mute chat", Slots::isMuteMessages, Slots::setMuteMessages),
                new BooleanSetting("Bet / balance", Slots::isShowUi, Slots::setShowUi)));

        modules.add(new GuiModule("green_screen", "im so fucking green", "Fun features",
                "Transparent green screen tint",
                GreenScreen::isEnabled, GreenScreen::setEnabled));

        modules.add(new GuiModule("dungeonbreaker_swap", "Dungeonbreaker swap", "QoL features",
                "Select Dungeonbreaker from your hotbar",
                DungeonbreakerSwap::isEnabled, DungeonbreakerSwap::setEnabled,
                new KeybindSetting("Keybind", DungeonbreakerSwap::keyName, DungeonbreakerSwap::bindKey)));
        modules.add(new GuiModule("instant_pets", "Instant Pets close", "Cheats",
                "Close after pet selection and suppress automatic reopen",
                ZeroPingPets::isEnabled, ZeroPingPets::setEnabled));
        modules.add(new GuiModule("player_size", "Player size", "Fun features",
                "Custom self X/Y/Z size (built-in sizes always active)",
                PlayerSize::isEnabled, PlayerSize::setEnabled,
                new NumberSetting("X (width)", 0.25, 3.0, 0.05,
                        PlayerSize::getScaleX, PlayerSize::setScaleX),
                new NumberSetting("Y (height)", 0.25, 3.0, 0.05,
                        PlayerSize::getScaleY, PlayerSize::setScaleY),
                new NumberSetting("Z (depth)", 0.25, 3.0, 0.05,
                        PlayerSize::getScaleZ, PlayerSize::setScaleZ),
                new ActionSetting("Reset to default", PlayerSize::resetSize)));

        modules.add(new GuiModule("name_changer", "Name changer", "Fun features",
                "Custom self name (built-in aliases always active)",
                NameChanger::isEnabled, NameChanger::setEnabled,
                new ActionSetting("Edit your name", NameChanger::openEditor)));

        // Party commands retain the existing controller placeholder until connected.
        modules.add(new GuiModule("party_commands", "PartyCMDS", "QoL features", "Party commands",
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
                    "General settings",
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

        return validate(modules.stream().map(module -> ModulePersistence.bind(module, ClientConfig.store())).toList());
    }

    public static List<GuiModule> modules() { return MODULES; }

    static List<GuiModule> validate(List<GuiModule> modules) {
        if (modules.stream().map(GuiModule::id).distinct().count() != modules.size()) {
            throw new IllegalArgumentException("Module ids must be unique");
        }
        return List.copyOf(modules);
    }
}

