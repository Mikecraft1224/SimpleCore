@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.examples.commands.sub

import com.github.mikecraft1224.simplecore.command.CommandBuilder
import com.github.mikecraft1224.simplecore.command.api.SubCommand
import com.github.mikecraft1224.simplecore.command.api.enumArg
import com.github.mikecraft1224.simplecore.command.api.greedyString
import com.github.mikecraft1224.simplecore.command.api.integer
import com.github.mikecraft1224.simplecore.command.api.word

/**
 * Demonstrates [SubCommand] - a self-contained subcommand group defined in its own file.
 *
 * Installed into the main `/sc` tree via `install(TestCommand)` in [CommandExampleLoader][com.github.mikecraft1224.simplecore.examples.commands.CommandExampleLoader].
 *
 * Commands exposed:
 * ```
 * /sc test echo <message>             - echoes a message back
 * /sc test repeat <count> <message>   - repeats a message n times
 * /sc test mode <value>               - picks a mode via suggestStatic
 * /sc test speed <fast|normal|slow>   - picks a speed via enumArg<Speed>
 * ```
 */
object TestCommand : SubCommand {

    private enum class Speed { FAST, NORMAL, SLOW }

    override fun CommandBuilder.register() {
        literal("test") {

            // Space decomposition: "echo message" -> literal("echo") > argument("message")
            argCallback("echo message", greedyString()) {
                sendFeedback(string("message"))
            }

            // Space decomposition on the outer argument creates literal("repeat") automatically;
            // argCallback on the inner argument is the shorthand for the execution leaf.
            argument("repeat count", integer(1, 20)) {
                argCallback("message", greedyString()) {
                    val n   = int("count")
                    val msg = string("message")
                    repeat(n) { i -> sendFeedback("§7[${i + 1}/$n]§r $msg") }
                }
            }

            // suggestStatic: fixed options offered without boilerplate forEach { suggest(it) }
            argument("mode value", word()) {
                suggestStatic("fast", "normal", "slow")
                executes { sendFeedback("§6Mode set:§r ${string("value")}") }
            }

            // enumArg: type-safe enum parsing + automatic tab completion from enum constants
            argCallback("speed", enumArg<Speed>()) {
                val speed = get<Speed>("speed")
                sendFeedback("§6Speed:§r $speed")
            }
        }
    }
}
