package com.github.mikecraft1224.simplecore.examples.render

import com.github.mikecraft1224.simplecore.SimpleCore
import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.events.RenderWorldEvent
import com.github.mikecraft1224.simplecore.overlay.HudManager

object WorldRenderExampleLoader {

    fun register() {
        SimpleCore.EVENTBUS.registerFeature(WorldRenderExample)
        SimpleCore.EVENTBUS.registerFeature(EntityTargetExample)
        EventRegistry.addBus(RenderWorldEvent::class, SimpleCore.EVENTBUS)
        RenderWorldEvent.registerEvents()

        // HudRenderExample is a HudElement — HudManager dispatches all registered elements.
        // RenderHudEvent is auto-hooked by FeatureAutoLoader when HudManager (@Feature) is scanned.
        HudManager.register(HudRenderExample)
    }
}
