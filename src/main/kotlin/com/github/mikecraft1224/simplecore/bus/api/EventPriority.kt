package com.github.mikecraft1224.simplecore.bus.api

/**
 * Represents the priority of an event listener.
 *
 * Event listeners with higher priority are called before those with lower priority.
 * The order of priorities from lowest to highest is:
 * - LOWEST
 * - LOW
 * - NORMAL
 * - HIGH
 * - HIGHEST
 */
@Suppress("UNUSED")
enum class EventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST
}