package com.github.mikecraft1224.input.api

/**
 * The context in which a keybind is active.
 *
 * ## Context overlap
 * [IN_CUSTOM_SCREEN] fires whenever a non-chat [net.minecraft.client.gui.screen.Screen] is open,
 * which includes [net.minecraft.client.gui.screen.ingame.HandledScreen] subclasses (inventory,
 * chest, crafting table, etc.). A keybind registered with [IN_CUSTOM_SCREEN] will therefore also
 * activate inside handled screens.
 *
 * [IN_HANDLED_SCREEN] is a strict subset: it fires only inside handled screens. Registering both
 * [IN_CUSTOM_SCREEN] and [IN_HANDLED_SCREEN] in the same context set is redundant - [IN_CUSTOM_SCREEN]
 * already covers handled screens.
 *
 * If you need a keybind that fires in custom/mod screens but NOT in handled screens, do not use
 * [IN_CUSTOM_SCREEN] alone; add your own screen-type guard inside the callback.
 */
enum class KeyContext {
    /** Active in every situation regardless of screen state. Default when no context is specified. */
    ANY,

    /** Active only while no screen is open (i.e. the player is directly in the game world). */
    IN_GAME,

    /**
     * Active while any non-chat [net.minecraft.client.gui.screen.Screen] is open, including
     * handled screens. Does NOT activate in chat. See class-level KDoc for overlap details.
     */
    IN_CUSTOM_SCREEN,

    /** Active only while the chat screen is open. */
    IN_CHAT,

    /**
     * Active only while a [net.minecraft.client.gui.screen.ingame.HandledScreen] is open
     * (e.g. inventory, chest, crafting table). This is a strict subset of [IN_CUSTOM_SCREEN].
     */
    IN_HANDLED_SCREEN,
}
