package com.github.mikecraft1224.simplecore.command.api

import com.github.mikecraft1224.simplecore.command.CommandBuilder

/**
 * A self-contained command group that can be installed into any [CommandBuilder] node.
 *
 * Implement this interface - typically on a Kotlin `object` - to define a subcommand
 * or a collection of related subcommands in isolation, then wire them together at
 * registration time via [CommandBuilder.install].
 *
 * This lets you split a large command tree across as many files as you like without any
 * shared state or explicit coordination between them.
 *
 * ### Defining a subcommand
 * ```kotlin
 * object GiveCommand : SubCommand {
 *     override fun CommandBuilder.register() {
 *         literal("give") {
 *             argument("player", word()) {
 *                 argument("item", word()) {
 *                     argument("count", integer(1, 64)) {
 *                         executes {
 *                             sendFeedback("Giving ${int("count")}x ${string("item")} to ${string("player")}")
 *                         }
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ### Wiring multiple subcommands together
 * ```kotlin
 * // Each file defines its own SubCommand object.
 * // A single registration file installs them all:
 *
 * CommandRegistry.client("mymod") {
 *     install(GiveCommand)
 *     install(TeleportCommand)
 *     install(BanCommand)
 *     install(ConfigCommand)      // ConfigCommand can itself install sub-groups
 *     // ... as many as you need
 * }
 * ```
 *
 * ### Nesting SubCommands
 * A [SubCommand] can install other [SubCommand]s inside itself, allowing arbitrarily
 * deep command trees with each level in its own file:
 * ```kotlin
 * object ConfigCommand : SubCommand {
 *     override fun CommandBuilder.register() {
 *         literal("config") {
 *             install(ConfigGetCommand)
 *             install(ConfigSetCommand)
 *             install(ConfigResetCommand)
 *         }
 *     }
 * }
 * ```
 */
fun interface SubCommand {
    fun CommandBuilder.register()
}
