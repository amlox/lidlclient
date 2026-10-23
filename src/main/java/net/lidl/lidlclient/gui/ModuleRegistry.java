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
import net.lidl.lidlclient.cursor.MenuCursor;
import net.lidl.lidlclient.hud.PanelHost;
import net.lidl.lidlclient.plinko.PlinkoPanel;
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
        List<GuiModule> modules = new ArrayList<>();
        modules.add(new GuiModule("gui_editor", "GUI editor", "General settings",
                "Click to move panels; Done or Escape exits",
                PanelHost::editing, PanelHost::setEditing));
        modules.add(new GuiModule("plinko", "Plinko", "Gambling", "Customizable Plinko board",
                PlinkoPanel::isEnabled, PlinkoPanel::setEnabled,
                new ActionSetting("Move panels", PanelHost::openEditor),
                new ActionSetting("Drop test", PlinkoPanel::dropTest),
                new NumberSetting("Background opacity (%)",0,80,5,PlinkoPanel::opacity,PlinkoPanel::setOpacity),
                new NumberSetting("Border thickness",1,3,1,PlinkoPanel::border,PlinkoPanel::setBorder),
                new ActionSetting("Accent colour",PlinkoPanel::editAccent),
                new ActionSetting("Peg colour",PlinkoPanel::editPegs),
                new ActionSetting("Panel colour",PlinkoPanel::editBackground)));
        modules.add(new GuiModule("gui_appearance", "GUI appearance", "General settings",
                "Toggle animated stars; right-click for colours", GuiAppearance::stars, GuiAppearance::setStars,
                new ActionSetting("Accent colour", GuiAppearance::editAccent),
                new ActionSetting("Background colour", GuiAppearance::editBackground),
                new ActionSetting("Reset colours", GuiAppearance::reset)));
        modules.add(new GuiModule("slots", "Slots", "Gambling", "Virtual slot machine",
                Slots::isEnabled, Slots::setEnabled,
                new BooleanSetting("Mute chat", Slots::isMuteMessages, Slots::setMuteMessages),
                new BooleanSetting("Bet / balance", Slots::isShowUi, Slots::setShowUi)));

        modules.add(new GuiModule("green_screen", "im so fucking green", "Fun features",
                "Transparent green screen tint",
                GreenScreen::isEnabled, GreenScreen::setEnabled));

        modules.add(new GuiModule("menu_cursor", "Instant menu cursor", "QoL features",
                "Release cursor while your menu hotkey loads", MenuCursor::isEnabled, MenuCursor::setEnabled,
                new ActionSetting("Edit keybinds", MenuCursor::openBindings),
                new BooleanSetting("Show slot overlay", MenuCursor::showOverlay, MenuCursor::setShowOverlay),
                new NumberSetting("Slot opacity (%)", 5, 80, 5, MenuCursor::overlayOpacity, MenuCursor::setOverlayOpacity),
                new NumberSetting("Wait timeout (seconds)", .25, 5, .25, MenuCursor::timeout, MenuCursor::setTimeout)));
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

        // Editor state is transient: restoring an enabled flag must never open a screen.
        return validate(modules.stream().map(module -> module.id().equals("gui_editor") ? module
                : ModulePersistence.bind(module, ClientConfig.store())).toList());
    }

    public static List<GuiModule> modules() { return MODULES; }

    static List<GuiModule> validate(List<GuiModule> modules) {
        if (modules.stream().map(GuiModule::id).distinct().count() != modules.size()) {
            throw new IllegalArgumentException("Module ids must be unique");
        }
        return List.copyOf(modules);
    }
}


