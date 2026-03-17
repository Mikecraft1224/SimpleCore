package com.github.mikecraft1224.bus

import com.github.mikecraft1224.Logger
import com.github.mikecraft1224.bus.api.ConditionalFeature
import com.github.mikecraft1224.bus.api.EventCompanion
import com.github.mikecraft1224.bus.api.Feature
import com.github.mikecraft1224.bus.api.FeatureCondition
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfoList
import net.fabricmc.loader.api.FabricLoader
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

/**
 * Utility to automatically load and register all classes annotated with [Feature] (or a
 * custom annotation) from packages declared by [ScanEntrypoint] Fabric entrypoints.
 *
 * Key behaviors:
 * - Classes annotated with [@ConditionalFeature][ConditionalFeature] are only registered when
 *   their [FeatureCondition] returns `true`.
 * - [FeatureScanRequest.rejectedPackages] lets mods exclude internal sub-packages from the scan.
 * - [FeatureScanRequest.annotationClasses] lets mods use their own marker annotation instead of
 *   the shared [Feature] annotation.
 */
object FeatureAutoLoader {

    /**
     * Resolves all [ScanEntrypoint] implementations registered under [entrypointKey] in
     * `fabric.mod.json` and delegates to [scanAndRegister].
     *
     * @param bus The bus to register discovered features onto.
     * @param entrypointKey The Fabric entrypoint key to query. Defaults to `simplecore:feature_scan`.
     */
    fun loadOptInPackages(bus: EventBus, entrypointKey: String = "simplecore:feature_scan") {
        val entrypoints = runCatching {
            FabricLoader.getInstance().getEntrypoints(entrypointKey, ScanEntrypoint::class.java)
        }.getOrElse { e ->
            Logger.error("Error loading SimpleCore scan entrypoints: ${e.message}", e)
            return
        }

        if (entrypoints.isEmpty()) {
            Logger.info("No SimpleCore scan entrypoints found for key '$entrypointKey'. Skipping feature auto-loading.")
            return
        }

        val requests = mutableListOf<FeatureScanRequest>()
        for (ep in entrypoints) {
            runCatching { requests.add(ep.scanRequest()) }.onFailure { e ->
                Logger.error("Error obtaining scan request from ${ep.javaClass.name}: ${e.message}", e)
            }
        }

        if (requests.isEmpty()) return

        // Merge all requests into a single scan to avoid redundant ClassGraph passes
        val packages = requests.flatMap { it.packages }.distinct()
        val rejected = requests.flatMap { it.rejectedPackages }.distinct()
        val annotations = requests.flatMap { it.annotationClasses }.distinct()

        scanAndRegister(bus, packages, rejected, annotations)
    }

    /**
     * Performs a ClassGraph scan over [acceptPackages] (minus [rejectPackages]), discovers
     * classes carrying any annotation in [annotationClasses], evaluates any
     * [@ConditionalFeature][ConditionalFeature] guard, instantiates eligible classes, and
     * registers them on [bus].
     *
     * After registration, lazily activates Fabric-side event hooks for every event type that
     * now has at least one handler (via [EventCompanion.registerEvents]).
     *
     * @param bus The bus to register discovered features onto.
     * @param acceptPackages Packages to scan.
     * @param rejectPackages Packages to exclude from the scan. Defaults to empty.
     * @param annotationClasses Annotation types that mark a class as a feature.
     *   Defaults to [[Feature]].
     */
    fun scanAndRegister(
        bus: EventBus,
        acceptPackages: List<String>,
        rejectPackages: List<String> = emptyList(),
        annotationClasses: List<KClass<out Annotation>> = listOf(Feature::class)
    ) {
        val graph = ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .ignoreClassVisibility()
            .acceptPackages(*acceptPackages.toTypedArray())

        if (rejectPackages.isNotEmpty()) {
            graph.rejectPackages(*rejectPackages.toTypedArray())
        }

        graph.scan().use { scanResult ->
            // Union results across all requested annotation types
            val featureClasses: ClassInfoList = annotationClasses
                .map { scanResult.getClassesWithAnnotation(it.java.name) }
                .fold(ClassInfoList.emptyList()) { acc, list -> acc.union(list) }

            for (ci in featureClasses) {
                try {
                    val cls = ci.loadClass()

                    if (!checkCondition(cls)) {
                        Logger.debug("[FeatureAutoLoader] Skipping ${cls.name} - condition returned false.")
                        continue
                    }

                    val instance = tryInstantiate(cls) ?: run {
                        Logger.warn(
                            "Could not instantiate feature class ${cls.name}. " +
                            "Ensure it has a no-arg constructor or is a Kotlin object/singleton."
                        )
                        continue
                    }

                    bus.registerFeature(instance)
                } catch (e: Exception) {
                    Logger.error("Error loading feature class ${ci.name}: ${e.message}", e)
                }
            }
        }

        // Lazily register Fabric-side event hooks for every event class that now has handlers
        bus.getRegisteredEventClasses().forEach { eventClass ->
            EventRegistry.addBus(eventClass, bus)
            (eventClass.companionObjectInstance as? EventCompanion)?.registerEvents()
        }
    }

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    /**
     * Evaluates the [@ConditionalFeature][ConditionalFeature] guard on [cls], if present.
     * Returns `true` if there is no guard or if the condition's [FeatureCondition.shouldLoad]
     * returns `true`.
     */
    private fun checkCondition(cls: Class<*>): Boolean {
        val annotation = cls.getAnnotation(ConditionalFeature::class.java) ?: return true
        val conditionClass = annotation.condition

        val condition: FeatureCondition? = run {
            // Try Kotlin object (INSTANCE field)
            runCatching {
                val field = conditionClass.java.getDeclaredField("INSTANCE")
                field.isAccessible = true
                field.get(null) as? FeatureCondition
            }.getOrNull()
            ?: runCatching {
                val ctor = conditionClass.java.getDeclaredConstructor()
                ctor.isAccessible = true
                ctor.newInstance() as FeatureCondition
            }.getOrElse { e ->
                Logger.warn(
                    "Could not instantiate FeatureCondition ${conditionClass.qualifiedName} for " +
                    "${cls.name}. Feature will be skipped. Cause: ${e.message}"
                )
                null
            }
        }

        return condition?.shouldLoad() ?: false
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