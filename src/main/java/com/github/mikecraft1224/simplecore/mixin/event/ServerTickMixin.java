package com.github.mikecraft1224.simplecore.mixin.event;

import com.github.mikecraft1224.simplecore.SimpleCore;
import com.github.mikecraft1224.simplecore.bus.EventRegistry;
import com.github.mikecraft1224.simplecore.bus.events.ServerTickEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ServerTickMixin {

    @Inject(method = "handleSetTime", at = @At("RETURN"))
    private void onWorldTimeUpdate(ClientboundSetTimePacket packet, CallbackInfo ci) {
        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(ServerTickEvent.class)) return;
        EventRegistry.INSTANCE.post(() -> new ServerTickEvent(packet.gameTime()));
    }
}
