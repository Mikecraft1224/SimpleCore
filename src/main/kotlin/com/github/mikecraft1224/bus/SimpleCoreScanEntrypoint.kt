package com.github.mikecraft1224.bus

@Suppress("UNUSED")
class SimpleCoreScanEntrypoint : ScanEntrypoint {
    override fun scanRequest(): FeatureScanRequest {
        return FeatureScanRequest(
            packages = listOf("com.github.mikecraft1224.bus.features")
        )
    }
}