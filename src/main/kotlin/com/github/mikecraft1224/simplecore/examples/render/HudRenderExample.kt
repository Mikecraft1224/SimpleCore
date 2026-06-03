package com.github.mikecraft1224.simplecore.examples.render

import com.github.mikecraft1224.simplecore.overlay.api.HudElement
import com.github.mikecraft1224.simplecore.overlay.api.HudRenderable
import com.github.mikecraft1224.simplecore.overlay.api.OverlayPosition
import net.minecraft.client.MinecraftClient

object HudRenderExample : HudElement("Player Stats", OverlayPosition(10f, 10f)) {

    override fun isEnabled(): Boolean = MinecraftClient.getInstance().player != null

    override fun buildContent(): List<HudRenderable> {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return emptyList()

        val hp     = player.health
        val maxHp  = player.maxHealth
        val hpFrac = (hp / maxHp).coerceIn(0f, 1f)
        val hpColor = when {
            hpFrac > 0.5f -> "§a"
            hpFrac > 0.2f -> "§e"
            else          -> "§c"
        }

        val food = player.hungerManager.foodLevel
        val foodColor = when {
            food > 14 -> "§a"
            food > 6  -> "§e"
            else      -> "§c"
        }

        val barW = 80

        return listOf(
            HudRenderable.text("§6§lPlayer Stats"),
            HudRenderable.text("§7Health: $hpColor${hp.toInt()}§7/§a${maxHp.toInt()}"),
            // custom: filled health bar — demonstrates HudRenderable.custom()
            HudRenderable.custom(barW, 4) { ctx, lx, ly ->
                ctx.fill(lx, ly, lx + barW, ly + 4, 0xFF333333.toInt())
                val barColor = when {
                    hpFrac > 0.5f -> 0xFF55FF55.toInt()
                    hpFrac > 0.2f -> 0xFFFFFF55.toInt()
                    else          -> 0xFFFF5555.toInt()
                }
                ctx.fill(lx, ly, lx + (barW * hpFrac).toInt(), ly + 4, barColor)
            },
            HudRenderable.text("§7Food:   $foodColor$food§7/20"),
            HudRenderable.text("§7FPS:    §e${mc.currentFps}"),
        )
    }
}
