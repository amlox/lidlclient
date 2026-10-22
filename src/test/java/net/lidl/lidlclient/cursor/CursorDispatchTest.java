package net.lidl.lidlclient.cursor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CursorDispatchTest {
    @Test void renderFramesCannotReleaseBeforeNormalKeybindTick() {
        var dispatch = new CursorDispatch();
        assertTrue(dispatch.queue());
        for (int frame=0;frame<4;frame++) assertFalse(dispatch.consume());
        assertTrue(dispatch.pending());
        dispatch.afterTick();
        assertTrue(dispatch.pending()); // END tick only marks ready, never releases there.
        assertTrue(dispatch.consume());
        assertFalse(dispatch.consume());
    }
    @Test void repeatedEventsDoNotReplaceTheOriginalRequestOrDelayIt() {
        var dispatch = new CursorDispatch();
        dispatch.queue();
        dispatch.afterTick();
        assertFalse(dispatch.queue());
        assertTrue(dispatch.consume());
    }
    @Test void screenFocusDisconnectOrDisableCancellationCannotReleaseLater() {
        var dispatch = new CursorDispatch();
        dispatch.queue(); dispatch.afterTick(); dispatch.cancel();
        assertFalse(dispatch.pending());
        assertFalse(dispatch.consume());
        assertTrue(dispatch.queue());
        assertFalse(dispatch.consume()); // A new request needs its own tick.
        dispatch.afterTick();
        assertTrue(dispatch.consume());
    }
}
