package net.lidl.lidlclient.gui;

import net.lidl.lidlclient.slots.Slots;

public class ModuleController {

    private static boolean partyCommandsEnabled = false;

    private ModuleController() {
    }

    // --- Mutators (Setters) ---

    public static void setSlotsEnabled(boolean enabled) {
        // Connect this to your existing Slots class
        Slots.setEnabled(enabled);
    }

    public static void setPartyCommandsEnabled(boolean enabled) {
        partyCommandsEnabled = enabled;

        // Connect this to your existing PartyCMDs class
    }

    // --- Accessors (Getters) ---

    public static boolean isSlotsEnabled() {
        return Slots.isEnabled();
    }

    public static boolean isPartyCommandsEnabled() {
        return partyCommandsEnabled;
    }

}
