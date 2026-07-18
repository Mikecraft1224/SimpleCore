@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerScoreEntry

/**
 * Reads the current sidebar scoreboard (the objective displayed in [DisplaySlot.SIDEBAR]).
 *
 * Pure live polling, no caching or event needed - vanilla's own [net.minecraft.world.scores.Scoreboard]
 * is already kept up to date from server packets for its own sidebar rendering.
 */
object ScoreboardUtils {
    private val sidebarObjective get() = McUtils.world?.scoreboard?.getDisplayObjective(DisplaySlot.SIDEBAR)

    /** The sidebar objective's title, or `null` if no sidebar is displayed. */
    val title: String? get() = sidebarObjective?.displayName?.string

    /** Sidebar rows sorted by score, descending (highest first) - the order vanilla renders them in. */
    val lines: List<PlayerScoreEntry> get() {
        val objective = sidebarObjective ?: return emptyList()
        val scoreboard = McUtils.world?.scoreboard ?: return emptyList()
        return scoreboard.listPlayerScores(objective)
            .filterNot { it.isHidden }
            .sortedByDescending { it.value() }
    }

    /** [lines]' raw text (the "owner" field, which most servers use as the actual line content) with color codes stripped. */
    val lineTexts: List<String> get() = lines.map { it.owner().removeColorCodes() }
}
