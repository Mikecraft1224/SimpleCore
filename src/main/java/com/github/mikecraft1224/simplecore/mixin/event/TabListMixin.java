package com.github.mikecraft1224.simplecore.mixin.event;

import com.github.mikecraft1224.simplecore.SimpleCore;
import com.github.mikecraft1224.simplecore.bus.EventRegistry;
import com.github.mikecraft1224.simplecore.bus.events.TabListEvent;
import com.github.mikecraft1224.simplecore.utils.internal.TabListCache;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class TabListMixin {

    @Inject(method = "handleTabListCustomisation", at = @At("HEAD"))
    private void onTabListCustomisation(ClientboundTabListPacket packet, CallbackInfo ci) {
        TabListCache.header = packet.header();
        TabListCache.footer = packet.footer();

        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(TabListEvent.class)) return;
        EventRegistry.INSTANCE.post(() -> new TabListEvent(packet.header(), packet.footer()));
    }
}
