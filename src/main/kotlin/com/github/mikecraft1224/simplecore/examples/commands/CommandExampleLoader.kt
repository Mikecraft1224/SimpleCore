package com.github.mikecraft1224.simplecore.examples.commands

import com.github.mikecraft1224.simplecore.BuildConfig
import com.github.mikecraft1224.simplecore.command.CommandRegistry
import com.github.mikecraft1224.simplecore.examples.commands.sub.TestCommand

/**
 * Registers example commands when [com.github.mikecraft1224.simplecore.SimpleCore.examples.command] is true.
 *
 * Demonstrates how a large command tree is split across multiple files using `SubCommand`:
 * each subcommand group lives in its own object under `sub/`, and this file wires them
 * together via [install][com.github.mikecraft1224.simplecore.command.CommandBuilder.install].
 *
 * ```
 * /sc                       - prints mod version
 * /sc help                  - lists subcommands
 * /sc test echo <msg>       - echoes a message          <- TestCommand
 * /sc test repeat <n> <msg> - repeats a message n times <- TestCommand
 * /sc test mode <value>     - picks a mode (suggestStatic)  <- TestCommand
 * /sc test speed <speed>    - picks a speed (enumArg)       <- TestCommand
 * ```
 */
object CommandExampleLoader {

    fun register() {
        CommandRegistry.client("sc", "simplecore") {
            description("SimpleCore developer commands")

            executes {
                sendFeedback("§6SimpleCore§r ${BuildConfig.MOD_VERSION}")
            }

            literalCallback("help") {
                sendFeedback("§6SimpleCore commands§r")
                sendFeedback("  §7/sc§r             - show version")
                sendFeedback("  §7/sc help§r        - this message")
                sendFeedback("  §7/sc test§r        - developer test commands")
            }

            // Each SubCommand is defined in its own file under sub/.
            // Adding more commands means adding more install() calls here
            // and a new object in sub/ - no changes to this file's logic.
            install(TestCommand)
        }
    }
}
