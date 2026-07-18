package com.github.mikecraft1224.simplecore.examples.render

import com.github.mikecraft1224.simplecore.SimpleCore
import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.events.RenderEntityOutlineEvent
import com.github.mikecraft1224.simplecore.bus.events.RenderWorldEvent
import com.github.mikecraft1224.simplecore.overlay.HudManager

/**
 * Loads the render examples when `SimpleCore.examples.render` is true.
 *
 * Activates three examples:
 * - [WorldRenderExample] - draws boxes, lines, text, and shapes around the player each frame
 * - [EntityTargetExample] - glows, boxes, and traces the nearest mob within 32 blocks
 * - [HudRenderExample] - a HUD overlay showing player health/food/FPS
 *
 * Note: [RenderWorldEvent] and [RenderEntityOutlineEvent] require explicit bus registration and
 * Fabric hook setup (see inline comments). This differs from events like
 * [com.github.mikecraft1224.simplecore.bus.events.ClientTickEvent] that lazy-register their hooks
 * automatically.
 */
object WorldRenderExampleLoader {

    fun register() {
        // Examples are opt-in, so these objects are NOT annotated @Feature and excluded from
        // auto-loading. Manual registerFeature() is the activation gate. In a real mod, annotate
        // with @Feature and rely on FeatureAutoLoader instead.
        SimpleCore.EVENTBUS.registerFeature(WorldRenderExample)
        SimpleCore.EVENTBUS.registerFeature(EntityTargetExample)

        // RenderWorldEvent/RenderEntityOutlineEvent do not lazy-register their Fabric hooks the
        // way ClientTickEvent does. Consumer mods must do both of these steps once per bus that
        // wants either event:
        //   1. addBus() - tells EventRegistry which bus receives the event's dispatches.
        //   2. registerEvents() - wires the single Fabric LevelRenderEvents.BEFORE_GIZMOS callback
        //      that drives all dispatches. Safe to call multiple times; only registers once.
        EventRegistry.addBus(RenderWorldEvent::class, SimpleCore.EVENTBUS)
        RenderWorldEvent.registerEvents()
        EventRegistry.addBus(RenderEntityOutlineEvent::class, SimpleCore.EVENTBUS)
        RenderEntityOutlineEvent.registerEvents()

        // HudRenderExample is a HudElement — HudManager dispatches all registered elements.
        // RenderHudEvent is auto-hooked by FeatureAutoLoader when HudManager (@Feature) is scanned.
        HudManager.register(HudRenderExample)
    }
}
