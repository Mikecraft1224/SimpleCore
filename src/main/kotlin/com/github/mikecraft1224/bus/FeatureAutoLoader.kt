package com.github.mikecraft1224.bus

import com.github.mikecraft1224.Logger
import com.github.mikecraft1224.bus.api.Feature
import io.github.classgraph.ClassGraph
import net.fabricmc.loader.api.FabricLoader


/**
 * Utility to automatically load and register all classes annotated with [Feature] from specified packages.
 * This uses the ClassGraph library to perform classpath scanning based on entrypoints defined in the Fabric mod loader.
 * You can specify your own entrypoints for your own event bus systems if needed.
 */
object FeatureAutoLoader {
    fun loadOptInPackages(bus: EventBus, entrypointKey: String = "simplecore:feature_scan") {
        val entrypoints = runCatching {
            FabricLoader.getInstance().getEntrypoints(entrypointKey, ScanEntrypoint::class.java)
        }.getOrElse { e ->
            Logger.error("Error loading SimpleCore scan entrypoints: ${e.message}")
            e.printStackTrace()
            return
        }

        if (entrypoints.isEmpty()) {
            Logger.info("No SimpleCore scan entrypoints found for key '$entrypointKey'. Skipping feature auto-loading.")
            return
        }

        val packagesToScan = mutableListOf<String>()
        for (ep in entrypoints) {
            try {
                val req = ep.scanRequest()
                packagesToScan.addAll(req.packages)
            } catch (e: Exception) {
                Logger.error("Error obtaining scan request from entrypoint ${ep.javaClass.name}: ${e.message}")
                e.printStackTrace()
            }
        }

        scanAndRegister(bus, packagesToScan)

        FabricEventHookLoader.hookUsedEvents(bus)
    }

    private fun scanAndRegister(bus: EventBus, acceptPackages: List<String>) {
        ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .ignoreClassVisibility()
            .acceptPackages(*acceptPackages.toTypedArray())
            .scan().use { scanResult ->
                val featureClasses = scanResult.getClassesWithAnnotation(Feature::class.java.name)

                for (ci in featureClasses) {
                    try {
                        val cls = ci.loadClass()
                        val instance = tryInstantiate(cls) ?: run {
                            Logger.warn("Could not instantiate feature class ${cls.name}. Ensure it has a no-arg constructor or is an object/singleton.")
                            continue
                        }

                        bus.registerFeature(instance)
                    } catch (e: Exception) {
                        Logger.error("Error loading feature class ${ci.name}: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
    }

    private fun tryInstantiate(cls: Class<*>): Any? {
        runCatching {
            val field = cls.getDeclaredField("INSTANCE")
            field.isAccessible = true
            val obj = field.get(null)
            if (obj != null) return obj
        }

        return runCatching {
            val ctor = cls.getDeclaredConstructor()
            ctor.isAccessible = true
            ctor.newInstance()
        }.getOrNull()
    }
}
