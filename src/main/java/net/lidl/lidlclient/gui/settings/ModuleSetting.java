package net.lidl.lidlclient.gui.settings;

/** Add a new setting type here and its generic widget once, rather than per module. */
public sealed interface ModuleSetting permits BooleanSetting, NumberSetting, ChoiceSetting, ActionSetting {
    String label();
    String valueText();
    void change(int direction);
}
