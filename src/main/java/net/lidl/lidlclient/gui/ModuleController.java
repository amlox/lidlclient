package net.lidl.lidlclient.gui;

import net.lidl.lidlclient.slots.Slots;

public class ModuleController {

    private static boolean slotsEnabled = false;
    private static boolean partyCommandsEnabled = false;
    private static boolean circleEnabled = false;

    private ModuleController() {
    }

    // --- Mutators (Setters) ---

    public static void setSlotsEnabled(boolean enabled) {
        slotsEnabled = enabled;

        // Connect this to your existing Slots class
        Slots.setEnabled(enabled);
    }

    public static void setPartyCommandsEnabled(boolean enabled) {
        partyCommandsEnabled = enabled;

        // Connect this to your existing PartyCMDs class
    }

    public static void setCircleEnabled(boolean enabled) {
        circleEnabled = enabled;

        // Connect this to your existing Circle class
    }

    // --- Accessors (Getters) ---

    public static boolean isSlotsEnabled() {
        return slotsEnabled;
    }

    public static boolean isPartyCommandsEnabled() {
        return partyCommandsEnabled;
    }

    public static boolean isCircleEnabled() {
        return circleEnabled;
    }
}
