@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import com.github.mikecraft1224.simplecore.input.internal.ScreenTracker
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack

/** The currently open container's menu, or `null` if no container GUI (chest, anvil, etc.) is open. */
val openContainerMenu: AbstractContainerMenu?
    get() = (ScreenTracker.currentScreen as? AbstractContainerScreen<*>)?.menu

/** The item in slot [index] of the currently open container, or [ItemStack.EMPTY] if none/closed. */
fun openContainerItem(index: Int): ItemStack =
    openContainerMenu?.slots?.getOrNull(index)?.item ?: ItemStack.EMPTY

/**
 * All non-empty items in the currently open container's slot list - this includes the player's
 * own inventory/hotbar slots, since vanilla appends those to the same [openContainerMenu.slots]
 * list. Use [ItemStack.customNameOrNull]/[ItemStack.loreLines] on the results to filter further.
 */
fun openContainerItems(): List<ItemStack> =
    openContainerMenu?.slots?.map { it.item }?.filterNot { it.isEmpty } ?: emptyList()
