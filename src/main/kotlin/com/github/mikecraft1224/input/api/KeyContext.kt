package com.github.mikecraft1224.input.api

/**
 * The context in which a keybind is active.
 */
enum class KeyContext {
    ANY,                // Default
    IN_GAME,            // Only in game (not in menus)
    IN_GUI,             // Only in GUIs excluding chat
    IN_CHAT,            // Only in chat
    IN_HANDLED_SCREEN   // Only in handled screens (e.g. inventory, crafting table)
}