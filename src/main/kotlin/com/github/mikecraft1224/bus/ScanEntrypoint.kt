package com.github.mikecraft1224.bus

import com.github.mikecraft1224.bus.api.Feature
import kotlin.reflect.KClass

/**
 * Describes the classpath scan that a mod wishes [FeatureAutoLoader] to perform when it opts
 * into the global (or a custom) event bus.
 *
 * @property packages Packages to include in the scan. Required, must be non-empty.
 * @property rejectedPackages Sub-packages to exclude from the scan. Useful to hide internal
 *   or test-only sub-packages that happen to reside inside a scanned package.
 * @property annotationClasses The annotation(s) used to mark feature classes. Defaults to
 *   [Feature]. Override to use a mod-local annotation instead of the shared [Feature] marker.
 *   All listed annotations are unioned — a class carrying any one of them is considered a feature.
 */
data class FeatureScanRequest(
    val packages: List<String>,
    val rejectedPackages: List<String> = emptyList(),
    val annotationClasses: List<KClass<out Annotation>> = listOf(Feature::class)
)

/**
 * Functional interface representing an entry point for opting into an EventBus.
 * For the global event bus add this entry point as `simplecore:feature_scan`.
 */
fun interface ScanEntrypoint {
    /**
     * Provides the [FeatureScanRequest] specifying which packages to scan for features.
     */
    fun scanRequest(): FeatureScanRequest
}