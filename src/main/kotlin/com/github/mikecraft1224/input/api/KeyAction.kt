package com.github.mikecraft1224.input.api

import net.minecraft.client.MinecraftClient
import net.minecraft.screen.slot.Slot
import java.util.EnumSet

typealias PressCallback = (MinecraftClient) -> Unit
typealias ReleaseCallback = (MinecraftClient) -> Unit
typealias HoldCallback = (MinecraftClient, Int) -> Unit
typealias HandledScreenCallback = (MinecraftClient, Slot) -> Unit

/**
 * Represents a key action with associated callbacks and context.
 *
 * This is a read-only handle. Do not attempt to drive dispatch by calling
 * [press], [release], or [hold] directly - those are internal to the registry.
 *
 * @property id Unique identifier for the key action.
 * @property source The source of the key action, either a vanilla key binding or a virtual key.
 * @property context The contexts in which this key action is active.
 * @property modifiers Required modifier keys (Ctrl, Shift, Alt) for this action to trigger.
 * @property holdEveryTicks The onHold callback fires every this many ticks while the key is held.
 * @property onPress Callback executed when the key is first pressed.
 * @property onRelease Callback executed when the key is released.
 * @property onHold Callback executed on hold ticks, receiving the accumulated hold-tick count.
 * @property onHandledScreen Callback executed when a slot is interacted with in a handled screen while the key is pressed.
 */
class KeyAction internal constructor(
    val id: String,
    val source: KeySource,
    val context: EnumSet<KeyContext>,
    var modifiers: Modifiers,
    val holdEveryTicks: Int?,
    val onPress: PressCallback,
    val onRelease: ReleaseCallback,
    val onHold: HoldCallback,
    val onHandledScreen: HandledScreenCallback,
) {
    internal var pressed = false
    internal var holdTicks = 0
    internal var lastPressFrame = -1

    /** Whether this keybind has been individually suppressed via [KeybindHandle.block]. */
    @Volatile
    internal var individuallyBlocked: Boolean = false

    internal fun press(client: MinecraftClient, currentFrame: Int) {
        if (!pressed) {
            pressed = true
            holdTicks = 0
            lastPressFrame = currentFrame
            onPress.invoke(client)
        }
    }

    internal fun release(client: MinecraftClient) {
        pressed = false
        holdTicks = 0
        onRelease.invoke(client)
    }

    internal fun hold(client: MinecraftClient) {
        holdTicks++
        if (holdEveryTicks != null && holdEveryTicks > 0 && holdTicks % holdEveryTicks == 0) {
            onHold.invoke(client, holdTicks)
        }
    }
}
