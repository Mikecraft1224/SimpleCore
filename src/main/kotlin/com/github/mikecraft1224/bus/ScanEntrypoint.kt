package com.github.mikecraft1224.bus

data class FeatureScanRequest(
    val packages: List<String>
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