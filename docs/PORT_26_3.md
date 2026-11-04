# Testing the Minecraft 26.3 port

This is a test port of Lidl Client 0.5. It keeps the existing Gradle project and
configuration format. The artifact version is `0.5+mc26.3`.

## IntelliJ IDEA

1. Fetch origin and check out `port/minecraft-26.3` in the existing project.
2. Set **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM**
   to **JDK 25**. Use the project's Gradle wrapper.
3. Click **Reload All Gradle Projects**.
4. Run the Gradle `build` task, then `runClient` (or the generated Minecraft Client
   run configuration). On Windows, the terminal equivalent is `./gradlew.bat build`
   followed by `./gradlew.bat runClient`; on macOS/Linux use `bash ./gradlew`.

No manual source edits or new project are required. If you have uncommitted local
work, commit or stash it before switching branches. To return to the old version,
switch back to `master` and reload Gradle.

## Changes

- Minecraft 26.3, matching Fabric API, and pinned Loom 1.18.4; Java stays at 25.
- SDL keyboard/mouse observation through vanilla input handlers; deferred shared
  hotkeys and key-release forwarding are retained.
- SDL cursor release, keyboard constants, and physical mouse-button checks.
  Inventory packet click IDs remain unchanged.
- Screens use `Minecraft.gui`; HUD messages, chat and toasts use their new owners.
- Render pipelines use RenderPearl; name rendering uses the current prepared-text
  and outlined-text hooks. Screen-transition mixins now target `Gui`.

Saved bindings use Minecraft key names. Verify them after upgrading, particularly
mouse bindings and non-US keyboards. Old platform-specific `scancode.*` bindings
are no longer valid and must be rebound in the menu.

## Validation before handoff

Direct Java 25 compilation against the official 26.3 client and dependencies passed,
with Fabric API's declared `guiRenderState` access widening applied to a scratch
compile jar. The existing 122 JUnit tests passed in that direct harness. Exact mixin
target method descriptors were checked against the official client classes.

This does **not** establish a successful Loom build or game launch. Loom setup in
the preparation environment fails at its Unix-domain socket capability probe
(`Operation not permitted`). No environment workaround or modified dependency is
included in this branch. Run the normal Gradle build and client locally.

## In-game checks

- Open `/lidl`; toggle modules, right-click settings, drag sliders and the colour
  picker, and type into text fields. Check that settings persist after restart.
- Test menu cursor keyboard and Mouse 4/5 bindings alongside the other menu mod.
  Check quick taps, held keys, Escape, timeout, focus loss, menu arrival, and
  legitimate-mode blocking. Confirm the camera stays still while waiting and
  input resumes afterward.
- Check pet selection and suppression of the delayed server menu reopening.
- Check player-size axes and names in chat, tab, nameplates and outlined text.
- Check reminder toasts, hotbar swapping, Reels keys and the farming warning.

If the build fails, send the first compiler error and the surrounding output from
IDEA's Build window. If launch fails, send `run/logs/latest.log` and the crash report
under `run/crash-reports/`, if one was generated. Include the action that triggered
an in-game problem and whether other mods were loaded.
