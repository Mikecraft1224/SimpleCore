@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.Event
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

/**
 * Fired when an item tooltip is being built. Mutate [lines] to add, remove, or
 * replace tooltip entries; the changes are reflected in the rendered tooltip.
 */
class ItemTooltipEvent(
    val stack: ItemStack,
    val lines: MutableList<Component>,
) : Event() {
    companion object : EventCompanion {
        private var registered = false

        override fun registerEvents() {
            if (registered) return
            ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
                EventRegistry.post { ItemTooltipEvent(stack, lines) }
            }
            registered = true
        }
    }
}
