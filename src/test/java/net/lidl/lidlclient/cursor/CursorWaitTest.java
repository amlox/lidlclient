package net.lidl.lidlclient.cursor;

import org.junit.jupiter.api.Test;
import static net.lidl.lidlclient.cursor.CursorWait.Result.*;
import static org.junit.jupiter.api.Assertions.*;

class CursorWaitTest {
    @Test void screenHandoffIsAcceptedOnlyOnceDuringAnActiveWait() {
        var wait = new CursorWait();
        assertFalse(wait.acceptScreen(100,true,true));
        wait.begin(100,1000);
        assertTrue(wait.acceptScreen(200,true,true));
        assertFalse(wait.pending());
        assertFalse(wait.acceptScreen(201,true,true));
    }
    @Test void unrelatedOrLateScreensCannotTriggerAnOpeningNotification() {
        var wait = new CursorWait();
        wait.begin(100,1000);
        assertFalse(wait.acceptScreen(200,false,true));
        assertFalse(wait.pending());
        wait.begin(100,1000);
        assertFalse(wait.acceptScreen(200,true,false));
        wait.begin(100,1000);
        assertFalse(wait.acceptScreen(1100,true,true));
        wait.begin(100,1000);
        assertFalse(wait.acceptScreen(99,true,true));
        wait.begin(100,1000);
        wait.cancel();
        assertFalse(wait.acceptScreen(200,true,true));
    }
    @Test void releasesOnceAndDoesNotExtendTheDeadlineOnRepeatedPresses() {
        var wait = new CursorWait();
        assertEquals(NONE,wait.update(0,false,true,true));
        assertTrue(wait.begin(100,1000));
        assertFalse(wait.begin(900,1000));
        assertEquals(WAITING,wait.update(1099,false,true,true));
        assertEquals(RESTORE,wait.update(1100,false,true,true));
        assertFalse(wait.pending());
        assertEquals(NONE,wait.update(1101,false,true,true));
    }
    @Test void realScreenTakesOwnershipWithoutRegrabbingEvenAtTheDeadline() {
        var wait = new CursorWait();
        wait.begin(0,1000);
        assertEquals(HANDOFF,wait.update(1000,true,true,true));
        assertEquals(NONE,wait.update(2000,false,true,true));
    }
    @Test void focusLossAndDisconnectNeverForceMouseCapture() {
        var wait = new CursorWait();
        wait.begin(100,1000);
        assertEquals(HANDOFF,wait.update(120,false,true,false));
        assertEquals(NONE,wait.update(2000,false,true,true));
        wait.begin(100,1000);
        assertEquals(HANDOFF,wait.update(120,false,false,true));
    }
    @Test void escapeDisableAndScreenArrivalCanCancelWithoutLeavingAStaleWait() {
        var wait = new CursorWait();
        wait.begin(100,1000);
        assertTrue(wait.cancel());
        assertFalse(wait.cancel());
        assertEquals(NONE,wait.update(2000,false,true,true));
        assertTrue(wait.begin(2001,1000));
        assertTrue(wait.pending());
    }
    @Test void invalidDurationAndClockResetCannotLeaveTheCursorUnbounded() {
        var wait = new CursorWait();
        assertFalse(wait.begin(100,0));
        assertFalse(wait.begin(100,-10));
        wait.begin(100,1000);
        assertEquals(RESTORE,wait.update(99,false,true,true));
    }
}
