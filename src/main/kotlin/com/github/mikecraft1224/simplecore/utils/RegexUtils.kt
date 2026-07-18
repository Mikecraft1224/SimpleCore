@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.network.chat.Component
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Returns a [Matcher] positioned on the first match of this pattern in [input], or `null` if it
 * doesn't match anywhere - avoids the two-step "match then check `.matches()`/`.find()`" dance
 * that raw [java.util.regex] otherwise requires for every chat-parsing trigger:
 * ```kotlin
 * val pattern = Pattern.compile("Welcome, (?<name>\\w+)!")
 *
 * @Subscribe
 * fun onChat(event: ChatReceiveEvent) {
 *     val matcher = pattern.matchMatcher(event.message.string) ?: return
 *     ChatUtils.print("Hello, ${matcher.group("name")}")
 * }
 * ```
 */
fun Pattern.matchMatcher(input: String): Matcher? {
    val matcher = matcher(input)
    return if (matcher.find()) matcher else null
}

/** [matchMatcher] against a chat [Component]'s plain text. */
fun Pattern.matchMatcher(input: Component): Matcher? = matchMatcher(input.string)

/** `true` if this pattern matches anywhere in [input]. */
fun Pattern.find(input: String): Boolean = matcher(input).find()

/** `true` if this pattern matches any entry in [inputs]. */
fun Pattern.matchesAny(inputs: Collection<String>): Boolean = inputs.any { find(it) }

/** Returns named group [name], or `null` if it didn't participate in the match or doesn't exist. */
fun Matcher.groupOrNull(name: String): String? = try {
    group(name)
} catch (e: RuntimeException) {
    null
}
