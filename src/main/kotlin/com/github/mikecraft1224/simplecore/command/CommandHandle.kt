package com.github.mikecraft1224.simplecore.command

/**
 * Runtime handle for a registered command returned by [CommandRegistry.client].
 *
 * Disabling a handle hides the command from tab completion and blocks execution.
 * Because the handle is shared across all aliases, disabling one disables all of them.
 *
 * ```kotlin
 * val handle = CommandRegistry.client("mymod", "mm") { ... }
 *
 * // Disable while a screen is open:
 * handle.disable()
 *
 * // Re-enable afterwards:
 * handle.enable()
 * ```
 */
class CommandHandle internal constructor(
    /** The primary command name this handle was created for. */
    val name: String,
) {
    /**
     * Whether this command is currently active.
     *
     * When `false`, the command is hidden from tab completion and execution is blocked.
     * Volatile so changes are immediately visible across threads.
     */
    @Volatile
    var isEnabled: Boolean = true
        private set

    /**
     * Human-readable description of what this command does.
     * Set via [CommandBuilder.description] in the DSL block.
     * Available for use by a help command or command listing UI.
     */
    var description: String = ""
        internal set

    /** Disables this command. Call [enable] to restore it. */
    fun disable() {
        isEnabled = false
    }

    /** Re-enables this command after [disable]. */
    fun enable() {
        isEnabled = true
    }
}
