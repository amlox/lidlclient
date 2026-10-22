package net.lidl.lidlclient.input;

import org.lwjgl.glfw.GLFWKeyCallbackI;

/** Wrap the callback being installed; retain and forward it, including modifiers and key releases. */
public final class RawKeyboard {
    public static final SharedHotkeys HOTKEYS=new SharedHotkeys();
    private RawKeyboard() { }
    public static GLFWKeyCallbackI observe(GLFWKeyCallbackI original) {
        return (window,key,scancode,action,modifiers) -> {
            // Capture eligibility before the original callback can open/close a screen.
            // Observation queues work only; it cannot change Minecraft's cursor or key state.
            try {
                HOTKEYS.observe(window,key,scancode,action);
            } finally {
                // Forward even if our observer fails; never take ownership of the callback.
                original.invoke(window,key,scancode,action,modifiers);
            }
        };
    }
}
