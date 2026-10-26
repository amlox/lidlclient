package net.lidl.lidlclient.PartyCMDS.framework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsecutiveCommandLimitTest {
    @Test void exactLimitBlocksFurtherAttemptsUntilAnotherCommandCompletes() {
        var streak=new ConsecutiveCommandLimit();
        for (int i=0;i<3;i++) {
            assertTrue(streak.allows("!dice",3));
            streak.completed("!dice");
        }
        assertFalse(streak.allows("!DICE",3));
        assertFalse(streak.allows("!dice",3));
        assertTrue(streak.allows("!coinflip",1));
        // Checking another command without completing it must not reset the streak.
        assertFalse(streak.allows("!dice",3));
        streak.completed("!coinflip");
        assertTrue(streak.allows("!dice",3));
        assertFalse(streak.allows("!coinflip",1));
    }
    @Test void unlimitedResetAndLiveLimitChanges() {
        var streak=new ConsecutiveCommandLimit();
        for (int i=0;i<100;i++) {
            assertTrue(streak.allows("!dice",0));
            streak.completed("!dice");
        }
        assertFalse(streak.allows("!dice",3));
        assertTrue(streak.allows("!dice",101));
        streak.reset();
        assertTrue(streak.allows("!dice",1));
    }
}
