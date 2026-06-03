@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.CancellableEvent
import net.minecraft.screen.slot.Slot

class InventoryKeyPressEvent(
    val keyCode: Int,
    val scanCode: Int,
    val modifiers: Int,
    val hoveredSlot: Slot?
) : CancellableEvent()