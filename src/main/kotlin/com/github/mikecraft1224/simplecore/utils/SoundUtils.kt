@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.Holder
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.Vec3

/** Plays [sound] as a client-side-only UI sound, not tied to any world position. */
fun McUtils.playSound(sound: SoundEvent, volume: Float = 1f, pitch: Float = 1f) {
    mc.soundManager.play(SimpleSoundInstance.forUI(sound, pitch, volume))
}

/** Overload for sounds only exposed as a [Holder] (most vanilla [SoundEvents] constants). */
fun McUtils.playSound(sound: Holder<SoundEvent>, volume: Float = 1f, pitch: Float = 1f) {
    mc.soundManager.play(SimpleSoundInstance.forUI(sound.value(), pitch, volume))
}

/** Plays [sound] at a world-space [pos], attenuating with distance like a normal world sound. */
fun McUtils.playSoundAt(
    pos: Vec3,
    sound: SoundEvent,
    volume: Float = 1f,
    pitch: Float = 1f,
    category: SoundSource = SoundSource.PLAYERS,
) {
    mc.soundManager.play(SimpleSoundInstance(sound, category, volume, pitch, RandomSource.create(), pos.x, pos.y, pos.z))
}

/** A handful of named presets for common UI feedback cues, so callers don't need to remember which vanilla sound reads as "success" vs "error". */
object SoundUtils {
    /** A short click, e.g. for confirming a toggle in a custom HUD/GUI. */
    fun playClick(volume: Float = 1f) = McUtils.playSound(SoundEvents.UI_BUTTON_CLICK, volume)

    /** A negative "denied" cue - vanilla's villager-no sound. */
    fun playError(volume: Float = 1f) = McUtils.playSound(SoundEvents.VILLAGER_NO, volume)

    /** A positive confirmation cue - vanilla's experience-orb pickup sound. */
    fun playSuccess(volume: Float = 1f) = McUtils.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, volume)
}
