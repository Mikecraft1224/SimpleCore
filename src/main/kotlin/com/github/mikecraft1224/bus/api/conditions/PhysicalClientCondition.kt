package com.github.mikecraft1224.bus.api.conditions

import com.github.mikecraft1224.bus.api.FeatureCondition
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader

/**
 * A [FeatureCondition] that activates a feature only when running on a physical client
 * (i.e. [EnvType.CLIENT]).
 *
 * Use this to guard features that reference client-only classes and would crash on a dedicated
 * server if loaded unconditionally.
 *
 * ```kotlin
 * @Feature
 * @ConditionalFeature(PhysicalClientCondition::class)
 * object MyHudFeature { ... }
 * ```
 */
object PhysicalClientCondition : FeatureCondition {
    override fun shouldLoad(): Boolean =
        FabricLoader.getInstance().environmentType == EnvType.CLIENT
}