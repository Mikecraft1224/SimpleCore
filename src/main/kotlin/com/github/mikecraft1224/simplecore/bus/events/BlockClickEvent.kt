@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.CancellableEvent
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack

/** Fired when the player clicks on a block (left or right click). Cancellable. */
class BlockClickEvent(
    val pos: BlockPos,
    val side: Direction,
    val clickType: ClickType,
    val itemInHand: ItemStack?,
) : CancellableEvent() {

    enum class ClickType { LEFT, RIGHT }

    companion object : EventCompanion {
        private var registered = false

        override fun registerEvents() {
            if (registered) return

            AttackBlockCallback.EVENT.register { player, world, hand, pos, direction ->
                if (!world.isClientSide) return@register InteractionResult.PASS
                val event = BlockClickEvent(pos, direction, ClickType.LEFT, player.getItemInHand(hand))
                EventRegistry.post(event)
                if (event.isCancelled) InteractionResult.FAIL else InteractionResult.PASS
            }

            UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
                if (!world.isClientSide) return@register InteractionResult.PASS
                val event = BlockClickEvent(hitResult.blockPos, hitResult.direction, ClickType.RIGHT, player.getItemInHand(hand))
                EventRegistry.post(event)
                if (event.isCancelled) InteractionResult.FAIL else InteractionResult.PASS
            }

            registered = true
        }
    }
}
