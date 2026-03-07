package com.github.mikecraft1224.bus.api

/**
 * An opaque token representing a single registered event handler method.
 *
 * Obtained from [com.github.mikecraft1224.bus.EventBus.registerFeatureWithHandles].
 * Call [unregister] to remove exactly this handler without touching any other handlers
 * belonging to the same feature instance.
 */
interface RegistrationHandle {
    /**
     * Removes this handler from its event bus list.
     * Calling this more than once is safe and has no effect after the first call.
     */
    fun unregister()

    /** `true` until [unregister] has been called. */
    val isRegistered: Boolean
}