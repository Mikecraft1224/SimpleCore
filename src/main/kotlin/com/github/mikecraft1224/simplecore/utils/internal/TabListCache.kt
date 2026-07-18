package com.github.mikecraft1224.simplecore.utils.internal

import net.minecraft.network.chat.Component

/**
 * Backing cache for [com.github.mikecraft1224.simplecore.utils.TabListUtils]. Written directly
 * from [com.github.mikecraft1224.simplecore.mixin.event.TabListMixin] (Java) via [JvmField]
 * so the mixin can assign it as a plain field with no getter/setter ceremony.
 */
object TabListCache {
    @JvmField var header: Component? = null
    @JvmField var footer: Component? = null
}
