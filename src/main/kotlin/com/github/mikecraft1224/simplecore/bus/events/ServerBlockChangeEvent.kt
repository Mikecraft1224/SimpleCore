@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.Event
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

/**
 * Fired when the server sends a block update for a single block.
 *
 * [oldState] reflects the client's state **before** the packet is applied;
 * [newState] is the state reported by the server.
 */
class ServerBlockChangeEvent(
    val pos: BlockPos,
    val oldState: BlockState,
    val newState: BlockState,
) : Event()
