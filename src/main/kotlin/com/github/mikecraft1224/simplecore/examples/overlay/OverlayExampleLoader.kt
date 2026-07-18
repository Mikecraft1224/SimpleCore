package com.github.mikecraft1224.simplecore.examples.overlay

import com.github.mikecraft1224.simplecore.SimpleCore
import com.github.mikecraft1224.simplecore.input.KeybindRegistry
import com.github.mikecraft1224.simplecore.input.api.KeyContext
import com.github.mikecraft1224.simplecore.input.api.KeyDescriptor
import com.github.mikecraft1224.simplecore.overlay.HudGroup
import org.lwjgl.glfw.GLFW

/**
 * Loads the overlay examples when `SimpleCore.examples.overlay` is true.
 *
 * This loader demonstrates the **opt-in example pattern** - features are not annotated
 * with `@Feature` so they are excluded from auto-loading. Activation is gated on the
 * examples flag and done manually here.
 *
 * It also demonstrates `HudGroup`, the recommended pattern for multi-overlay mods. A
 * [HudGroup] groups a mod's overlays under a single name so [HudGroup.openEditor] opens
 * a filtered editor showing only that mod's elements - other mods' overlays are absent.
 *
 * In a real mod you would:
 * 1. Create one `HudGroup` for your mod (typically a top-level val in your main object).
 * 2. Annotate your `HudElement` subclass with `@Feature` so the event bus side is wired
 *    automatically by `FeatureAutoLoader`.
 * 3. Call `group.register(MyHud)` explicitly - only `HudManager.register()` needs a manual
 *    call; the `@Feature` annotation handles event subscriptions.
 * 4. Wire `group.openEditor()` to a keybind, command, or config button as your mod prefers.
 */
object OverlayExampleLoader {

    // One HudGroup per mod - gives the editor a scoped, named view of just these overlays.
    private val OVERLAYS = HudGroup("SimpleCore Examples")

    fun register() {
        // Examples are opt-in, so SessionTracker is NOT annotated @Feature and is excluded from
        // auto-loading. Manual registerFeature() is the activation gate - it only runs when
        // examples.overlay = true. In a real mod you would annotate your HudElement with @Feature
        // and rely on FeatureAutoLoader to wire @Subscribe handlers automatically.
        SimpleCore.EVENTBUS.registerFeature(SessionTracker)

        // HudGroup.register() delegates to HudManager.register() and tracks the element label.
        // Opening OVERLAYS.openEditor() will show only the elements registered to this group.
        OVERLAYS.register(SessionTracker)

        // Wire the editor to a keybind. HudGroup does NOT register any keybind automatically -
        // choose keybind, command, or config button based on how often your players need it.
        KeybindRegistry.registerVirtual(
            id = "simplecore.overlay_editor",
            key = KeyDescriptor.keyboard(GLFW.GLFW_KEY_O),
            KeyContext.IN_GAME,
            onPress = { _ -> OVERLAYS.openEditor() },
        )
    }
}
