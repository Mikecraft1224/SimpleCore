@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.Event
import net.minecraft.entity.Entity

/** Fired when an entity is added to the client world (via mixin on ClientWorld.addEntity). */
class EntityEnterWorldEvent(val entity: Entity) : Event()

/** Fired when an entity is removed from the client world (via mixin on ClientWorld.removeEntity). */
class EntityLeaveWorldEvent(val entity: Entity) : Event()
