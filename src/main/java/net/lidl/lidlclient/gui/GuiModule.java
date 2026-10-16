
package net.lidl.lidlclient.gui;

public class GuiModule {

    public final String name;
    public final String description;
    public final String tab;

    public boolean enabled = false;

    private final Runnable onEnable;
    private final Runnable onDisable;

    public GuiModule(
            String name,
            String description,
            String tab,
            Runnable onEnable,
            Runnable onDisable
    ) {
        this.name = name;
        this.description = description;
        this.tab = tab;
        this.onEnable = onEnable;
        this.onDisable = onDisable;
    }

    public void toggle() {
        enabled = !enabled;

        if (enabled) {
            if (onEnable != null) {
                onEnable.run();
            }
        } else {
            if (onDisable != null) {
                onDisable.run();
            }
        }
    }
}
