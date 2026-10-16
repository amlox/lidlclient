package net.lidl.lidlclient.gui;

import net.lidl.lidlclient.gui.settings.BooleanSetting;
import net.lidl.lidlclient.slots.Slots;

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

        // Party commands retain the existing controller placeholder until connected.
        modules.add(new GuiModule("party_commands", "PartyCMDS", "Social", "Party commands",
                ModuleController::isPartyCommandsEnabled, ModuleController::setPartyCommandsEnabled));

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
