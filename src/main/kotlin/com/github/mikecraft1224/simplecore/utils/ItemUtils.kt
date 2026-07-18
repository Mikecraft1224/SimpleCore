@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.ItemEnchantments

/** Lore lines as plain strings (color codes preserved), or an empty list if this item has none. */
fun ItemStack.loreLines(): List<String> = get(DataComponents.LORE)?.lines()?.map { it.string } ?: emptyList()

/** This item's custom name if set (anvil rename, `minecraft:custom_name` component), or `null` otherwise. */
fun ItemStack.customNameOrNull(): String? = get(DataComponents.CUSTOM_NAME)?.string

/** The item's full display name as plain text (custom name if set, otherwise its default translated name). */
fun ItemStack.displayNameString(): String = hoverName.string

/** Raw custom NBT data attached to this item (the `minecraft:custom_data` component), or an empty tag if none. */
fun ItemStack.customData(): CompoundTag = get(DataComponents.CUSTOM_DATA)?.copyTag() ?: CompoundTag()

/** This item's enchantments - regular enchantments if present, otherwise stored ones (e.g. on an enchanted book). */
fun ItemStack.enchantments(): ItemEnchantments {
    val regular = get(DataComponents.ENCHANTMENTS)
    if (regular != null && !regular.isEmpty) return regular
    return get(DataComponents.STORED_ENCHANTMENTS) ?: ItemEnchantments.EMPTY
}

/** `true` if [text] appears in this item's lore (case-insensitive, color codes ignored). */
fun ItemStack.loreContains(text: String): Boolean =
    loreLines().any { it.removeColorCodes().contains(text, ignoreCase = true) }
