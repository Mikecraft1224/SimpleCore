package com.github.mikecraft1224.bus.api.conditions

import com.github.mikecraft1224.bus.api.FeatureCondition
import net.fabricmc.loader.api.FabricLoader

/**
 * A [FeatureCondition] that activates a feature only when a specific mod is present on the
 * classpath.
 *
 * Subclass this and override [modId] to specify the required mod:
 * ```kotlin
 * class RequiresMyMod : ModPresentCondition() {
 *     override val modId = "mymod"
 * }
 *
 * @Feature
 * @ConditionalFeature(RequiresMyMod::class)
 * object MyIntegrationFeature { ... }
 * ```
 */
abstract class ModPresentCondition : FeatureCondition {
    /** The mod ID that must be present for the feature to load. */
    abstract val modId: String

    override fun shouldLoad(): Boolean = FabricLoader.getInstance().isModLoaded(modId)
}