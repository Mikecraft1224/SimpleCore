@file:Suppress("DEPRECATION", "unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.Event
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager

/** Fired synchronously when client resources are reloaded (resource packs, F3+T). */
class ResourceReloadEvent(val manager: ResourceManager) : Event() {
    companion object : EventCompanion {
        private var registered = false

        override fun registerEvents() {
            if (registered) return
            // TODO: SimpleSynchronousResourceReloadListener is deprecated. Migrate to implementing
            //  SynchronousResourceReloadListener directly with the appropriate ID registration
            //  overload when bumping Fabric API.
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(object : SimpleSynchronousResourceReloadListener {
                    override fun getFabricId(): Identifier =
                        Identifier.fromNamespaceAndPath("simplecore", "resource_reload_event")

                    override fun onResourceManagerReload(manager: ResourceManager) {
                        EventRegistry.post { ResourceReloadEvent(manager) }
                    }
                })
            registered = true
        }
    }
}
