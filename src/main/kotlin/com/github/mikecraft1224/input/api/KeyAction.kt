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
 * @property id Unique identifier for the key action.
 * @property source The source of the key action, either a vanilla key binding or a virtual key code.
 * @property context The contexts in which this key action is active.
 * @property modifiers Required modifier keys (Ctrl, Shift, Alt) for this action to trigger.
 * @property holdEveryTicks If set, the onHold callback will be called every specified ticks while the key is held down.
 * @property onPress Callback function to be executed when the key is pressed.
 * @property onRelease Callback function to be executed when the key is released.
 * @property onHold Callback function to be executed when the key is held down, receiving the number of ticks held as a parameter.
 * @property onHandledScreen Callback function to be executed when interacting with a slot in a handled screen while the key is pressed.
 */
class KeyAction internal constructor(
    val id: String,
    val source: KeySource,
    val context: EnumSet<KeyContext>,
    var modifiers: Modifiers,
    val holdEveryTicks: Int?,
    val onPress: PressCallback?,
    val onRelease: ReleaseCallback?,
    val onHold: HoldCallback?,
    val onHandledScreen: HandledScreenCallback?,
) {
    internal var pressed = false
    internal var holdTicks = 0
    internal var lastPressFrame = -1

    fun press(client: MinecraftClient, currentFrame: Int) {
        if (!pressed) {
            pressed = true
            holdTicks = 0
            lastPressFrame = currentFrame
            onPress?.invoke(client)
        }
    }

    fun release(client: MinecraftClient) {
        pressed = false
        holdTicks = 0
        onRelease?.invoke(client)
    }

    fun hold(client: MinecraftClient, currentFrame: Int) {
        holdTicks++
        if (holdEveryTicks != null && holdEveryTicks > 0 && holdTicks % holdEveryTicks == 0) {
            onHold?.invoke(client, holdTicks)
        }
    }
}