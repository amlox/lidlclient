package net.lidl.lidlclient.gui;

import net.lidl.lidlclient.gui.settings.BooleanSetting;
import net.lidl.lidlclient.gui.settings.ChoiceSetting;
import net.lidl.lidlclient.gui.settings.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ModuleFrameworkTest {
    private GuiModule module(String id, String category, int settingCount) {
        var enabled = new AtomicBoolean();
        var settings = new ArrayList<net.lidl.lidlclient.gui.settings.ModuleSetting>();
        for (int i = 0; i < settingCount; i++) {
            var value = new AtomicBoolean();
            settings.add(new BooleanSetting("Setting " + i, value::get, value::set));
        }
        return new GuiModule(id, id, category, "", enabled::get, enabled::set, settings);
    }

    @Test void toggleReadsLiveFeatureStateInsteadOfShadowState() {
        var enabled = new AtomicBoolean(true);
        var module = new GuiModule("a", "A", "General", "", enabled::get, enabled::set);
        assertTrue(module.isEnabled());
        enabled.set(false); // Feature changed its state outside the menu.
        module.toggle();
        assertTrue(enabled.get());
        module.toggle();
        assertFalse(enabled.get());
    }

    @Test void categoriesAndExpansionFollowStableIdsAfterFilteringAndReordering() {
        var a = module("a", "General", 2);
        var b = module("b", "Social", 5);
        var c = module("c", "General", 0);
        var modules = List.of(b, c, a);
        assertEquals(List.of("Social", "General"), ModuleLayout.categories(modules));
        var rows = ModuleLayout.rows(modules, "General", "a", 38, 34, 10);
        assertEquals(2, rows.size());
        assertEquals("c", rows.getFirst().module().id());
        assertEquals(38, rows.getFirst().height());
        assertEquals(48, rows.getLast().top());
        assertEquals(106, rows.getLast().height());
        assertEquals(154, ModuleLayout.contentHeight(rows));
    }

    @Test void largeListsAndTallDropdownsCanReachTheirLastRows() {
        var modules = new ArrayList<GuiModule>();
        for (int i = 0; i < 500; i++) modules.add(module("m" + i, "General", i == 499 ? 100 : 0));
        var rows = ModuleLayout.rows(modules, "General", "m499", 38, 34, 10);
        int content = ModuleLayout.contentHeight(rows);
        double offset = ModuleLayout.clampScroll(1e9, content, 250);
        assertEquals(content - 250, offset);
        assertEquals(250, rows.getLast().bottom() - offset);
        assertEquals(0, ModuleLayout.clampScroll(-30, content, 250));
        assertEquals(0, ModuleLayout.clampScroll(offset, 38, 250));
    }

    @Test void booleanSettingWritesThroughToFeature() {
        var value = new AtomicBoolean();
        var setting = new BooleanSetting("Show", value::get, value::set);
        setting.change(1);
        assertTrue(value.get());
        assertEquals("ON", setting.valueText());
        setting.change(-1);
        assertFalse(value.get());
    }

    @Test void numericSettingSnapsClampsAndRejectsNonfiniteInput() {
        var value = new AtomicReference<>(10.0);
        var setting = new NumberSetting("Radius", 2, 20, 0.5, value::get, value::set);
        setting.setFraction(0.26);
        assertEquals(6.5, value.get());
        setting.setFraction(2);
        assertEquals(20.0, value.get());
        setting.change(1);
        assertEquals(20.0, value.get());
        setting.set(-50);
        assertEquals(2.0, value.get());
        setting.set(Double.NaN);
        assertEquals(2.0, value.get());
        value.set(Double.NaN);
        assertEquals(0, setting.fraction());
        assertThrows(IllegalArgumentException.class,
                () -> new NumberSetting("Bad", 2, 2, 1, value::get, value::set));
    }

    @Test void choicesWrapInBothDirectionsAndRecoverFromUnknownValue() {
        var value = new AtomicReference<>("A");
        var setting = new ChoiceSetting("Mode", List.of("A", "B", "C"), value::get, value::set);
        setting.change(-1);
        assertEquals("C", value.get());
        setting.change(1);
        assertEquals("A", value.get());
        value.set("Removed option");
        setting.change(1);
        assertEquals("A", value.get());
    }

    @Test void sliderCanReachMaximumWhenStepDoesNotDivideRange() {
        var value = new AtomicReference<>(0.0);
        var setting = new NumberSetting("Count", 0, 10, 3, value::get, value::set);
        setting.setFraction(1);
        assertEquals(10.0, value.get());
        setting.change(-1);
        assertEquals(9.0, value.get());
    }

    @Test void declarationsDefensivelyCopySettings() {
        var mutable = new ArrayList<net.lidl.lidlclient.gui.settings.ModuleSetting>();
        var module = new GuiModule("a", "A", "General", "", () -> false, value -> { }, mutable);
        mutable.add(new BooleanSetting("Extra", () -> false, value -> { }));
        assertTrue(module.settings().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> module.settings().add(mutable.getFirst()));
    }
}
