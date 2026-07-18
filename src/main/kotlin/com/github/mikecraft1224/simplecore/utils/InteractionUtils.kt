@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3

/**
 * Simulates the player's own left-click, right-click, and hotbar actions - the same
 * client-to-server calls vanilla's own mouse/key handling makes, just triggered
 * programmatically instead of from a real input event.
 *
 * Every function is a no-op (returns `null`/`false`) if the player or game mode isn't loaded.
 */
object InteractionUtils {
    private val gameMode: MultiPlayerGameMode? get() = McUtils.mc.gameMode

    /** Left-click attacks [entity] - the same call a real left-click on a targeted entity makes. */
    fun attack(entity: Entity) {
        val player = McUtils.player ?: return
        gameMode?.attack(player, entity)
    }

    /** Right-click interacts with [entity] (trading with a villager, mounting a horse, etc). */
    fun interact(entity: Entity, hand: InteractionHand = InteractionHand.MAIN_HAND): InteractionResult? {
        val player = McUtils.player ?: return null
        return gameMode?.interact(player, entity, EntityHitResult(entity), hand)
    }

    /** Right-clicks with the item in [hand] without targeting anything (eating, drinking, opening a book). */
    fun useItem(hand: InteractionHand = InteractionHand.MAIN_HAND): InteractionResult? {
        val player = McUtils.player ?: return null
        return gameMode?.useItem(player, hand)
    }

    /** Right-clicks the item in [hand] against a specific block hit (placing a block, opening a door, etc). */
    fun useItemOnBlock(hit: BlockHitResult, hand: InteractionHand = InteractionHand.MAIN_HAND): InteractionResult? {
        val player = McUtils.player ?: return null
        return gameMode?.useItemOn(player, hand, hit)
    }

    /** Convenience overload of [useItemOnBlock] that builds the [BlockHitResult] from a block position and face. */
    fun useItemOnBlock(pos: BlockPos, face: Direction = Direction.UP, hand: InteractionHand = InteractionHand.MAIN_HAND): InteractionResult? {
        val hitPos = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        return useItemOnBlock(BlockHitResult(hitPos, face, pos, false), hand)
    }

    /** Instantly breaks the block at [pos] - creative mode only, has no effect in survival (use [startBreaking]/[continueBreaking]/[stopBreaking] there). */
    fun breakBlockInstant(pos: BlockPos): Boolean = gameMode?.destroyBlock(pos) ?: false

    /** Starts the survival mining sequence on the block at [pos]. Call [continueBreaking] every subsequent tick and [stopBreaking] when done or cancelled. */
    fun startBreaking(pos: BlockPos, face: Direction = Direction.UP): Boolean = gameMode?.startDestroyBlock(pos, face) ?: false

    /** Continues an in-progress mining sequence started with [startBreaking]. Call once per tick while mining. */
    fun continueBreaking(pos: BlockPos, face: Direction = Direction.UP): Boolean = gameMode?.continueDestroyBlock(pos, face) ?: false

    /** Cancels an in-progress mining sequence started with [startBreaking]. */
    fun stopBreaking() {
        gameMode?.stopDestroyBlock()
    }

    /** Selects hotbar slot [slot] (0-8) - the same effect as pressing the matching number key. */
    fun selectHotbarSlot(slot: Int) {
        require(slot in 0..8) { "Hotbar slot must be 0..8, got $slot" }
        McUtils.player?.inventory?.setSelectedSlot(slot)
    }

    /**
     * Swaps [containerSlot] (the currently open menu's own slot index - see
     * [net.minecraft.world.inventory.Slot.index]) with hotbar slot [hotbarSlot] (0-8) - the same
     * effect as hovering a slot and pressing the matching number key. Works against the player's
     * own inventory too (nothing else needs to be open).
     */
    fun swapToHotbar(containerSlot: Int, hotbarSlot: Int) {
        require(hotbarSlot in 0..8) { "Hotbar slot must be 0..8, got $hotbarSlot" }
        val player = McUtils.player ?: return
        gameMode?.handleContainerInput(player.containerMenu.containerId, containerSlot, hotbarSlot, ContainerInput.SWAP, player)
    }

    /** Middle-click "pick block" on the block at [pos] - copies it into the hotbar like vanilla's pick-block key. */
    fun pickBlock(pos: BlockPos, includeData: Boolean = false) {
        gameMode?.handlePickItemFromBlock(pos, includeData)
    }

    /** Middle-click "pick block" on [entity] (e.g. a spawn egg from a mob) - copies it into the hotbar. */
    fun pickEntity(entity: Entity, includeData: Boolean = false) {
        gameMode?.handlePickItemFromEntity(entity, includeData)
    }
}
