package com.github.mikecraft1224.bus.api

interface EventCompanion<T : Event<*>> {
    fun registerEvents()
}