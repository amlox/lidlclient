package net.lidl.lidlclient.PartyCMDS;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartyCommandSwitchesTest {
    @Test void eachCommandCanBeDisabledWithoutDisablingOthers() {
        for (String command : PartyCommandSwitches.COMMANDS) {
            var switches = new PartyCommandSwitches();
            switches.set(command, false);
            assertFalse(switches.allows(command));
            for (String other : PartyCommandSwitches.COMMANDS) {
                if (!other.equals(command)) assertTrue(switches.allows(other));
            }
            assertFalse(switches.activeCommands().contains(command));
        }
    }

    @Test void masterSwitchPreservesIndividualChoices() {
        var switches = new PartyCommandSwitches();
        switches.set("!ban", false);
        switches.setEnabled(false);
        assertTrue(switches.activeCommands().isEmpty());
        assertTrue(switches.configured("!tesco"));
        switches.setEnabled(true);
        assertFalse(switches.allows("!ban"));
        assertTrue(switches.allows("!tesco"));
    }

    @Test void commandArgumentsDoNotChangeTheToggleLookup() {
        assertEquals("!tesco", PartyCommandSwitches.commandName("  !TESCO   a question  "));
        assertEquals("!dice", PartyCommandSwitches.commandName("!dice"));
    }

    @Test void namesAreCaseInsensitiveAndUnknownCommandsStayDisabled() {
        var switches = new PartyCommandSwitches();
        switches.set("!BAN", false);
        assertFalse(switches.allows("!ban"));
        assertFalse(switches.allows("!missing"));
        assertThrows(IllegalArgumentException.class, () -> switches.set("!missing", true));
    }
}
