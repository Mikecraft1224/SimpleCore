package com.github.mikecraft1224.input.api

import net.minecraft.client.option.KeyBinding

/**
 * Represents the source of a key binding, either a vanilla key binding or a virtual key code.
 */
sealed interface KeySource {
    class Vanilla(val keyBinding: KeyBinding) : KeySource
    class Virtual(var keyCode: Int) : KeySource
}