package net.lidl.lidlclient.input;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedHotkeysTest {
    @Test void mouseFourIsDistinctFromKeyboardAndOnlyFiresOnPress() {
        var hotkeys=new SharedHotkeys(); var seen=new ArrayList<Long>();
        hotkeys.bind("mouse4",List.of(SharedHotkeys.Key.mouse(3)),seen::add);
        hotkeys.observe(10,3,3,1);
        hotkeys.observeMouse(20,3,1);
        hotkeys.observeMouse(20,3,0);
        hotkeys.observeMouse(20,3,2);
        hotkeys.observeMouse(30,4,1);
        hotkeys.dispatch();
        assertEquals(List.of(20L),seen);
    }
    @Test void keyboardDoesNotReactToMouseAndRemovedMouseBindingDiscardsQueuedPress() {
        var hotkeys=new SharedHotkeys(); var seen=new ArrayList<Long>();
        hotkeys.bind("keyboard",List.of(SharedHotkeys.Key.scancode(3)),seen::add);
        hotkeys.bind("mouse",List.of(SharedHotkeys.Key.mouse(3)),seen::add);
        hotkeys.observeMouse(1,3,1);
        hotkeys.unbind("mouse");
        hotkeys.dispatch();
        assertTrue(seen.isEmpty());
    }

    @Test void sharedKeysNotifyEveryOwnerWithoutClaimingTheKey() {
        var hotkeys=new SharedHotkeys(); var seen=new ArrayList<String>();
        hotkeys.bind("pets",List.of(SharedHotkeys.Key.symbol(82)),window -> seen.add("pets:"+window));
        hotkeys.bind("cursor",List.of(SharedHotkeys.Key.symbol(82)),window -> seen.add("cursor:"+window));
        hotkeys.observe(7,82,19,1);
        assertTrue(seen.isEmpty()); // The native input callback does not run module actions.
        seen.add("original callback");
        hotkeys.dispatch();
        assertEquals(List.of("original callback","pets:7","cursor:7"),seen);
    }
    @Test void quickTapSurvivesBetweenFramesButRepeatAndReleaseDoNotRetrigger() {
        var hotkeys=new SharedHotkeys(); var seen=new ArrayList<Long>();
        hotkeys.bind("a",List.of(SharedHotkeys.Key.symbol(82)),seen::add);
        hotkeys.observe(1,82,19,1); hotkeys.observe(1,82,19,2); hotkeys.observe(1,82,19,0);
        hotkeys.dispatch(); hotkeys.dispatch();
        assertEquals(List.of(1L),seen);
    }
    @Test void scancodesAndSymbolsAreIndependentAndDuplicateKeysFireOnce() {
        var hotkeys=new SharedHotkeys(); var seen=new ArrayList<Long>();
        hotkeys.bind("a",List.of(SharedHotkeys.Key.scancode(19),SharedHotkeys.Key.symbol(82)),seen::add);
        hotkeys.observe(1,-1,19,1); hotkeys.observe(2,82,0,1); hotkeys.observe(3,81,18,1);
        hotkeys.dispatch();
        assertEquals(List.of(1L,2L),seen);
    }
    @Test void rebindAndRemovalDiscardStalePendingPresses() {
        var hotkeys=new SharedHotkeys(); var seen=new ArrayList<Long>();
        hotkeys.bind("a",List.of(SharedHotkeys.Key.symbol(82)),seen::add);
        hotkeys.observe(1,82,19,1);
        hotkeys.bind("a",List.of(SharedHotkeys.Key.symbol(84)),seen::add);
        hotkeys.dispatch(); assertTrue(seen.isEmpty());
        hotkeys.observe(2,84,20,1); hotkeys.unbind("a"); hotkeys.dispatch();
        assertTrue(seen.isEmpty());
    }
    @Test void guiKeysCannotBecomeGameplayHotkeysWhenTheScreenCloses() {
        var hotkeys=new SharedHotkeys(); var seen=new ArrayList<Long>();
        var inWorld=new AtomicBoolean(false);
        hotkeys.bind("a",List.of(SharedHotkeys.Key.symbol(82)),inWorld::get,seen::add);
        hotkeys.observe(1,82,19,1); inWorld.set(true); hotkeys.dispatch();
        assertTrue(seen.isEmpty());
        hotkeys.observe(2,82,19,1); hotkeys.dispatch();
        assertEquals(List.of(2L),seen);
    }
    @Test void inputFloodIsBoundedAndCanAcceptNewEventsAfterDispatch() {
        var hotkeys=new SharedHotkeys(); var seen=new ArrayList<Long>();
        hotkeys.bind("a",List.of(SharedHotkeys.Key.symbol(82)),seen::add);
        for (int i=0;i<1000;i++) hotkeys.observe(i,82,19,1);
        hotkeys.dispatch(); assertEquals(64,seen.size());
        hotkeys.observe(1001,82,19,1); hotkeys.dispatch(); assertEquals(65,seen.size());
    }
}
