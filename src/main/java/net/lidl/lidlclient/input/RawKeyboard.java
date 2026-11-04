package net.lidl.lidlclient.input;

/** Observe vanilla's SDL event dispatch without consuming keys or modifying key states. */
public final class RawKeyboard {
    public static final SharedHotkeys HOTKEYS = new SharedHotkeys();
    private RawKeyboard() { }

    public static void observe(long window, int key, int action) {
        // SDL keyboard keys are physical scancodes. Actions remain deferred until mouse movement.
        HOTKEYS.observe(window, key, key, action);
    }

    public static void observeMouse(long window, int button, int action) {
        HOTKEYS.observeMouse(window, button, action);
    }
}
