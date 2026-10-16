# Adding a module

Open `src/main/java/net/lidl/lidlclient/gui/ModuleRegistry.java` and add one declaration
inside `createModules()`, before `return validate(modules)`:

```java
modules.add(new GuiModule("my_feature", "My Feature", "General", "What the feature does",
        MyFeature::isEnabled, MyFeature::setEnabled));
```

Your feature supplies a getter and setter:

```java
private static boolean enabled;
public static boolean isEnabled() { return enabled; }
public static void setEnabled(boolean value) { enabled = value; }
```

The actual feature must check that state in its event handlers, or activate/deactivate
its behavior in the setter. Registering a menu entry does not create feature behavior.
If your feature already has a boolean field, lambdas work too:

```java
modules.add(new GuiModule("my_feature", "My Feature", "Visual", "Example",
        () -> MyFeature.enabled, value -> MyFeature.enabled = value));
```

Use a unique stable id for every module. Category names are arbitrary strings. A new
category automatically gets a sidebar button. Order in the registry is display order
within a category; the first occurrence determines category order. No coordinates,
screen branches, category arrays, or state-restoration switches are needed.

## Settings

Settings follow the enable getter/setter in the declaration. Import the setting classes
from `net.lidl.lidlclient.gui.settings`.

```java
modules.add(new GuiModule("my_feature", "My Feature", "Visual", "Example",
        MyFeature::isEnabled, MyFeature::setEnabled,
        new BooleanSetting("Show labels", MyFeature::isShowLabels, MyFeature::setShowLabels),
        new NumberSetting("Radius", 2, 20, 0.5, MyFeature::getRadius, MyFeature::setRadius),
        new ChoiceSetting("Mode", List.of("Soft", "Bright", "Outline"),
                MyFeature::getMode, MyFeature::setMode)));
```

These `MyFeature` methods are examples: implement them in your feature. Number getters
return `double` and setters accept `double`. For integer fields, use adapters such as
`() -> MyFeature.radius` and `value -> MyFeature.radius = (int) Math.round(value)` with
a step of 1. Choice getters/setters use `String` values; enum features can convert in
their getter/setter lambdas.

Left-click a module to toggle it. Right-click to expand/collapse its settings. Modules
without settings do not show an empty dropdown. Left-click a boolean row to toggle it.
Left/right-click a choice to cycle forward/backward. For numbers, drag/click the track,
or left/right-click the label/value to increment/decrement one step.

Scroll over the module area to move through modules and expanded settings. The module
scrollbar can also be dragged. Scroll over the category rail to reach additional
categories. Rendering and hit testing share the same layout; offscreen content is
clipped. Only visible cards/settings submit geometry, so adding modules does not add
an unlimited amount of drawing work every frame.

## Where state lives

The GUI reads the feature's live getter. It does not keep its own enabled boolean.
This means changes made elsewhere are reflected the next frame and when reopening.
Persistence across game restarts is the responsibility of your feature/config storage;
this refactor does not add a new disk config format.

Slots uses its real getter/setter, including its existing settings. PartyCMDS and
Circle retain their existing controller placeholders; connect their feature behavior
when those implementations are ready. The old display-only keybind row is omitted;
this framework does not pretend to provide working keybind assignment.

## Appearance

`gui/render/SmoothRenderer.java` draws rounded geometry with fractional vertices and
a one-physical-pixel alpha fringe. It uses Minecraft's existing batched GUI pipeline,
without custom shaders or a separate native renderer. Borders and glow are gradient
meshes, not stepped stacks of integer rectangles. Antialiasing width adapts to GUI scale.

`ShizoStyleScreen` controls panel colors and spacing. `PANEL` is an ARGB color: its first
byte controls opacity (`00` transparent, `FF` opaque). `SLIDE_DURATION_NS` controls the
slide-in; 500,000,000 means 0.5 seconds. Typography still uses Minecraft's default font;
the smooth renderer changes shapes, borders and glow, not font assets.

## Checks

Run `./gradlew build`. JUnit covers live toggle state, setting writes, slider bounds,
choice wrapping, category filtering, reordering and scrolling through 500 modules with
a 100-setting dropdown. In-game checks: open `/lidl`, try both mouse buttons, resize/change
GUI scale, scroll a long registry and drag number tracks and the scrollbar.
