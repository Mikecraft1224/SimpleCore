package com.github.mikecraft1224.input

import com.github.mikecraft1224.Logger
import com.github.mikecraft1224.bus.api.Feature
import com.github.mikecraft1224.bus.api.Subscribe
import com.github.mikecraft1224.bus.events.ClientTickEvent
import com.github.mikecraft1224.bus.events.InventoryKeyPressEvent
import com.github.mikecraft1224.input.api.*
import com.github.mikecraft1224.mixin.KeyBindingAccessor
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.input.KeyInput
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry for all keybinds — both vanilla (Fabric-registered [KeyBinding]) and
 * virtual (raw GLFW polling, supporting keyboard keys and mouse buttons).
 *
 * Keybinds are dispatched once per client tick at [ClientTickEvent.Phase.END].
 * Handled-screen keybinds also respond immediately to [InventoryKeyPressEvent] so that
 * Minecraft's own key-handling does not swallow the event first.
 *
 * ### Registering a keybind
 * ```kotlin
 * val zoomHandle = KeybindRegistry.registerVirtual(
 *     id = "mymod.zoom",
 *     key = KeyDescriptor.keyboard(GLFW.GLFW_KEY_C),
 *     context = KeyContext.IN_GAME,
 *     onPress = { _ -> startZoom() },
 *     onRelease = { _ -> stopZoom() },
 * )
 *
 * // Later, when the feature is torn down:
 * zoomHandle.unregister()
 * ```
 */
@Suppress("UNUSED")
@Feature
object KeybindRegistry {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private val keyActions = mutableMapOf<String, KeyAction>()

    @Volatile
    private var frame = 0

    @Volatile
    private var blocked = false

    /**
     * The set of [KeyContext] values that are currently suppressed globally.
     * Any keybind whose context intersects this set will not fire.
     */
    private val blockedContexts: MutableSet<KeyContext> = ConcurrentHashMap.newKeySet()

    // -------------------------------------------------------------------------
    // Public utilities
    // -------------------------------------------------------------------------

    /**
     * Returns `true` if the given [InputUtil.Key] is currently held down.
     *
     * Handles both keyboard keys ([InputUtil.Type.KEYSYM]) and mouse buttons
     * ([InputUtil.Type.MOUSE]) by routing to the appropriate GLFW query.
     *
     * @param key The key or mouse button to query.
     */
    fun isKeyDown(key: InputUtil.Key): Boolean {
        val windowHandle = MinecraftClient.getInstance().window.handle
        return when (key.category) {
            InputUtil.Type.KEYSYM -> GLFW.glfwGetKey(windowHandle, key.code) == GLFW.GLFW_PRESS
            InputUtil.Type.MOUSE  -> GLFW.glfwGetMouseButton(windowHandle, key.code) == GLFW.GLFW_PRESS
            else                  -> false
        }
    }

    /**
     * Overload for backward compatibility. Queries a keyboard key by its raw GLFW key code.
     *
     * @param keyCode A GLFW key constant (e.g. `GLFW.GLFW_KEY_C`).
     */
    fun isKeyDown(keyCode: Int): Boolean =
        isKeyDown(InputUtil.Type.KEYSYM.createFromCode(keyCode))

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers a vanilla keybind that appears in Minecraft's keybinding options screen.
     *
     * The [id] is passed directly to [KeyBinding] where Minecraft treats it as a translation key.
     * It must follow the `key.<modid>.<action>` convention and have a corresponding entry in
     * `en_us.json`; a warning is logged if the convention is not followed.
     *
     * Vanilla keybinds are bound to keyboard keys only. Mouse button support requires a virtual
     * keybind via [registerVirtual].
     *
     * @param id Translation key in the form `key.<modid>.<action>` (e.g. `key.mymod.zoom`).
     * @param category The options-screen category. Defaults to [KeyBinding.Category.MISC].
     * @param defaultKey The default key descriptor. Defaults to [InputUtil.UNKNOWN_KEY] (unbound).
     * @param context One or more [KeyContext] values. Pass no arguments to default to [KeyContext.ANY].
     * @param holdEveryTicks The [onHold] callback fires every this many ticks while held. Defaults to 1.
     * @param onPress Fired on the tick the key transitions from up to down.
     * @param onRelease Fired on the tick the key transitions from down to up.
     * @param onHold Fired every [holdEveryTicks] ticks while the key is held; receives hold-tick count.
     * @param onHandledScreen Fired immediately when a slot is interacted with while the key is held.
     * @return A [KeybindHandle] for runtime unregistration and suppression.
     */
    fun registerVanilla(
        id: String,
        category: KeyBinding.Category = KeyBinding.Category.MISC,
        defaultKey: KeyDescriptor = KeyDescriptor(),
        vararg context: KeyContext,
        holdEveryTicks: Int = 1,
        onPress: PressCallback = {},
        onRelease: ReleaseCallback = {},
        onHold: HoldCallback = { _, _ -> },
        onHandledScreen: HandledScreenCallback = { _, _ -> },
    ): KeybindHandle {
        val kb = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                id,
                InputUtil.Type.KEYSYM,
                defaultKey.key.code,
                category,
            )
        )
        val ctx = resolveContext(*context)
        val act = KeyAction(
            id,
            KeySource.Vanilla(kb),
            ctx,
            defaultKey.modifiers,
            holdEveryTicks,
            onPress,
            onRelease,
            onHold,
            onHandledScreen,
        )

        if (!id.matches(Regex("^key\\.[a-z0-9_]+\\.[a-z0-9_.]+$"))) {
            Logger.warn("Vanilla keybind id '$id' does not follow the recommended 'key.<modid>.<action>' translation key convention.")
        }

        keyActions[id] = act
        return KeybindHandle(act) { keyActions.remove(id) }
    }

    /**
     * Registers a virtual keybind driven by direct GLFW polling.
     *
     * Virtual keybinds do not appear in Minecraft's options screen and support both keyboard
     * keys and mouse buttons via [KeyDescriptor.keyboard] and [KeyDescriptor.mouse].
     * They can be rebound at runtime via [updateVirtualKeybind].
     *
     * A warning is logged if [id] is already registered, since the previous keybind will be
     * silently overwritten.
     *
     * @param id Unique registry key for this keybind (arbitrary string; not a translation key).
     * @param key The initial key or mouse button and optional modifiers.
     * @param context One or more [KeyContext] values. Pass no arguments to default to [KeyContext.ANY].
     * @param holdEveryTicks The [onHold] callback fires every this many ticks while held. Defaults to 1.
     * @param onPress Fired on the tick the key transitions from up to down.
     * @param onRelease Fired on the tick the key transitions from down to up.
     * @param onHold Fired every [holdEveryTicks] ticks while the key is held; receives hold-tick count.
     * @param onHandledScreen Fired immediately when a slot is interacted with while the key is held.
     * @return A [KeybindHandle] for runtime unregistration and suppression.
     */
    fun registerVirtual(
        id: String,
        key: KeyDescriptor = KeyDescriptor(),
        vararg context: KeyContext,
        holdEveryTicks: Int = 1,
        onPress: PressCallback = {},
        onRelease: ReleaseCallback = {},
        onHold: HoldCallback = { _, _ -> },
        onHandledScreen: HandledScreenCallback = { _, _ -> },
    ): KeybindHandle {
        if (keyActions.containsKey(id)) {
            Logger.warn("Virtual keybind with id '$id' is already registered and will be overwritten.")
        }

        val ctx = resolveContext(*context)
        val act = KeyAction(
            id,
            KeySource.Virtual(key.key),
            ctx,
            key.modifiers,
            holdEveryTicks,
            onPress,
            onRelease,
            onHold,
            onHandledScreen,
        )
        keyActions[id] = act
        return KeybindHandle(act) { keyActions.remove(id) }
    }

    // -------------------------------------------------------------------------
    // Runtime mutation
    // -------------------------------------------------------------------------

    /**
     * Updates the bound key and modifiers of a virtual keybind at runtime (e.g. from config).
     *
     * Silently does nothing if [id] is not registered or belongs to a vanilla keybind.
     *
     * @param id The id passed to [registerVirtual].
     * @param newKey The replacement key descriptor.
     */
    fun updateVirtualKeybind(id: String, newKey: KeyDescriptor) {
        (keyActions[id]?.source as? KeySource.Virtual)?.key = newKey.key
        keyActions[id]?.modifiers = newKey.modifiers
    }

    // -------------------------------------------------------------------------
    // Global blocking
    // -------------------------------------------------------------------------

    /**
     * Blocks all keybind processing and releases all currently pressed keys.
     *
     * USE WITH CAUTION: intended for situations where the game window loses focus or key presses
     * are consumed by another system (e.g. an external text input overlay).
     *
     * Call [unblockKeybind] to resume normal processing.
     */
    fun blockKeybind() {
        blocked = true
    }

    /**
     * Resumes keybind processing after a call to [blockKeybind].
     */
    fun unblockKeybind() {
        blocked = false
    }

    // -------------------------------------------------------------------------
    // Context-level blocking
    // -------------------------------------------------------------------------

    /**
     * Suppresses all keybinds whose context set intersects [contexts].
     *
     * Any keybind that is currently pressed and whose context is being blocked will be released
     * on the next tick. Call [unblockContext] to lift the suppression.
     *
     * @param contexts One or more [KeyContext] values to suppress.
     */
    fun blockContext(vararg contexts: KeyContext) {
        blockedContexts.addAll(contexts.toList())
    }

    /**
     * Lifts context-level suppression previously applied by [blockContext].
     *
     * @param contexts One or more [KeyContext] values to un-suppress.
     */
    fun unblockContext(vararg contexts: KeyContext) {
        blockedContexts.removeAll(contexts.toSet())
    }

    /**
     * Returns a snapshot of the currently suppressed [KeyContext] values.
     */
    fun getBlockedContexts(): Set<KeyContext> = blockedContexts.toSet()

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Converts a vararg context to an EnumSet, defaulting to ANY when nothing is provided. */
    private fun resolveContext(vararg context: KeyContext): EnumSet<KeyContext> =
        if (context.isEmpty()) EnumSet.of(KeyContext.ANY)
        else EnumSet.copyOf(context.toList())

    // -------------------------------------------------------------------------
    // Tick dispatch
    // -------------------------------------------------------------------------

    @Subscribe
    private fun onClientTick(event: ClientTickEvent) {
        if (event.phase != ClientTickEvent.Phase.END) return

        frame++

        if (blocked) {
            keyActions.values.forEach { action ->
                if (action.pressed) action.release(event.client)
            }
            return
        }

        val win = event.client.window
        val screen = event.client.currentScreen
        val inChat = screen is ChatScreen
        val inHandledScreen = screen is HandledScreen<*>

        keyActions.forEach { _, action ->
            // --- Global context-block check ---
            if (action.context.any { it in blockedContexts }) {
                if (action.pressed) action.release(event.client)
                return@forEach
            }

            // --- Per-keybind block check ---
            if (action.individuallyBlocked) {
                if (action.pressed) action.release(event.client)
                return@forEach
            }

            // --- Context activation ---
            val inContext = action.context.contains(KeyContext.ANY) ||
                (action.context.contains(KeyContext.IN_GAME)          && screen == null) ||
                (action.context.contains(KeyContext.IN_CUSTOM_SCREEN) && screen != null && !inChat) ||
                (action.context.contains(KeyContext.IN_CHAT)          && inChat) ||
                (action.context.contains(KeyContext.IN_HANDLED_SCREEN) && inHandledScreen)

            if (!inContext) {
                if (action.pressed) action.release(event.client)
                return@forEach
            }

            // --- Modifier check ---
            if (!action.modifiers.matches(win)) {
                if (action.pressed) action.release(event.client)
                return@forEach
            }

            // --- Key state ---
            val (isDown, pressedThisFrame) = when (val src = action.source) {
                is KeySource.Vanilla -> {
                    val down = isKeyDown((src.keyBinding as KeyBindingAccessor).boundKey)
                    val edge = down && !action.pressed
                    down to edge
                }
                is KeySource.Virtual -> {
                    val down = isKeyDown(src.key)
                    val edge = down && !action.pressed
                    down to edge
                }
            }

            // --- Dispatch ---
            when {
                pressedThisFrame            -> action.press(event.client, frame)
                !isDown && action.pressed   -> action.release(event.client)
                isDown  && action.pressed   -> action.hold(event.client)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Inventory key-press dispatch
    // -------------------------------------------------------------------------

    @Subscribe
    private fun onInventoryKeyPress(event: InventoryKeyPressEvent) {
        if (blocked) return

        val client = MinecraftClient.getInstance()
        val screen = client.currentScreen
        if (screen !is HandledScreen<*>) return

        keyActions.values.forEach { action ->
            if (action.individuallyBlocked) return@forEach
            if (action.context.any { it in blockedContexts }) return@forEach
            if (!action.context.contains(KeyContext.IN_HANDLED_SCREEN) && !action.context.contains(KeyContext.ANY)) return@forEach
            if (!action.modifiers.matchesMask(event.modifiers)) return@forEach

            val matches = when (val src = action.source) {
                is KeySource.Virtual -> src.key.code == event.keyCode
                is KeySource.Vanilla -> src.keyBinding.matchesKey(KeyInput(event.keyCode, event.scanCode, event.modifiers))
            }
            if (!matches) return@forEach

            if (!action.pressed) {
                action.press(client, frame)
                action.onHandledScreen.invoke(client, event.hoveredSlot ?: return@forEach)
            }
        }
    }
}
