package com.github.mikecraft1224.input

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
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import java.util.*

@Feature
object KeybindRegistry {
    fun isKeyDown(keyCode: Int): Boolean {
        val window = MinecraftClient.getInstance().window
        return InputUtil.isKeyPressed(window.handle, keyCode)
    }

    private val keyActions = mutableMapOf<String, KeyAction>()

    private var frame = 0
    private var blocked = false

    fun registerVanilla(
        id: String,
        categoryKey: String,

        defaultKey: Int = GLFW.GLFW_KEY_UNKNOWN,
        context: EnumSet<KeyContext> = EnumSet.of(KeyContext.ANY),
        modifiers: Modifiers = Modifiers(),
        holdEveryTicks: Int = 1,

        onPress: PressCallback = {},
        onRelease: ReleaseCallback = {},
        onHold: HoldCallback = { _, _ -> },
        onHandledScreen: HandledScreenCallback = { _, _ -> },
    ): KeyAction {
        val kb = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                id,
                InputUtil.Type.KEYSYM,
                defaultKey,
                categoryKey
            )
        )
        val act = KeyAction(
            id,
            KeySource.Vanilla(kb),
            context,
            modifiers,
            holdEveryTicks,
            onPress,
            onRelease,
            onHold,
            onHandledScreen
        )
        keyActions[id] = act

        return act
    }

    fun registerVirtual(
        id: String,

        key: ConfigKeybind,
        context: EnumSet<KeyContext> = EnumSet.of(KeyContext.ANY),
        holdEveryTicks: Int = 1,

        onPress: PressCallback = {},
        onRelease: ReleaseCallback = {},
        onHold: HoldCallback = { _, _ -> },
        onHandledScreen: HandledScreenCallback = { _, _ -> },
    ): KeyAction {
        val act = KeyAction(
            id,
            KeySource.Virtual(key.keyCode),
            context,
            key.modifiers,
            holdEveryTicks,
            onPress,
            onRelease,
            onHold,
            onHandledScreen
        )
        keyActions[id] = act

        return act
    }

    fun updateVirtualKeybind(id: String, newKey: ConfigKeybind) {
        (keyActions[id]?.source as? KeySource.Virtual)?.keyCode = newKey.keyCode
        keyActions[id]?.modifiers = newKey.modifiers
    }

    /**
     * Blocks all keybind processing and releases all currently pressed keys.
     * USE WITH CAUTION: This is intended for use when the game window loses focus or keypresses are used elsewhere.
     * I.e. when a text input is active.
     */
    fun blockKeybind() {
        blocked = true
    }

    /**
     * Unblocks keybind processing.
     */
    fun unblockKeybind() {
        blocked = false
    }

    // Client tick event handler
    @Subscribe
    private fun onClientTick(event: ClientTickEvent) {
        if (event.phase != ClientTickEvent.Phase.END) return

        frame++

        if (blocked) {
            // Release all pressed keys
            keyActions.values.forEach { action ->
                if (action.pressed) action.release(event.client)
            }
            return
        }

        val win = event.client.window.handle
        val screen = event.client.currentScreen
        val inChat = screen is ChatScreen
        val inHandledScreen = screen is HandledScreen<*>

        keyActions.forEach { key, action ->
            // Determine context
            val inContext = action.context.contains(KeyContext.ANY) ||
                (action.context.contains(KeyContext.IN_GAME) && screen == null) ||
                (action.context.contains(KeyContext.IN_GUI) && screen != null && !inChat) ||
                (action.context.contains(KeyContext.IN_CHAT) && inChat) ||
                (action.context.contains(KeyContext.IN_HANDLED_SCREEN) && inHandledScreen)

            if (!inContext) {
                if (action.pressed) action.release(event.client)
                return@forEach
            }

            // Modifiers
            if (!action.modifiers.matches(win)) {
                if (action.pressed) action.release(event.client)
                return@forEach
            }

            // Key state
            val (isDown, pressedThisFrame) = when (val src = action.source) {
                is KeySource.Vanilla -> {
                    val down = isKeyDown((src.keyBinding as KeyBindingAccessor).boundKey.code)
                    val edge = down && !action.pressed
                    down to edge
                }
                is KeySource.Virtual -> {
                    val down = isKeyDown(src.keyCode)
                    val edge = down && !action.pressed
                    down to edge
                }
            }

            // Handle press
            if (pressedThisFrame) {
                action.press(event.client, event.tickCount)
            } else if (!isDown && action.pressed) {
                action.release(event.client)
            } else if (isDown && action.pressed) {
                action.hold(event.client, event.tickCount)
            }
        }
    }

    @Subscribe
    private fun onInventoryKeyPress(event: InventoryKeyPressEvent) {
        if (blocked) return

        val client = MinecraftClient.getInstance()
        val screen = client.currentScreen
        if (screen !is HandledScreen<*>) return

        // Immediate handled-screen callback
        keyActions.values.forEach { action ->
            if (!action.context.contains(KeyContext.IN_HANDLED_SCREEN) && !action.context.contains(KeyContext.ANY)) return@forEach
            if (!action.modifiers.matchesMask(event.modifiers)) return@forEach

            val matches = when (val src = action.source) {
                is KeySource.Virtual -> src.keyCode == event.keyCode
                is KeySource.Vanilla -> src.keyBinding.matchesKey(event.keyCode, event.scanCode)
            }
            if (!matches) return@forEach

            // Ensure press edge
            if (!action.pressed) {
                action.press(client, frame)
                action.onHandledScreen?.invoke(client, event.hoveredSlot ?: return@forEach)
            }
        }
    }
}