package net.lidl.lidlclient.gui;

import net.lidl.lidlclient.slots.Slots;
import net.lidl.lidlclient.PartyCMDS.PartyCommands;

public class ModuleController {


    private ModuleController() {
    }

    // --- Mutators (Setters) ---

    public static void setSlotsEnabled(boolean enabled) {
        // Connect this to your existing Slots class
        Slots.setEnabled(enabled);
    }

    public static void setPartyCommandsEnabled(boolean enabled) {
        PartyCommands.setEnabled(enabled);
    }

    // --- Accessors (Getters) ---

    public static boolean isSlotsEnabled() {
        return Slots.isEnabled();
    }

    public static boolean isPartyCommandsEnabled() {
        return PartyCommands.isEnabled();
    }

}
