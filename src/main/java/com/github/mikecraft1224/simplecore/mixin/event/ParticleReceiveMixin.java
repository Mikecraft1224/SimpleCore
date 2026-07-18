package com.github.mikecraft1224.simplecore.mixin.event;

import com.github.mikecraft1224.simplecore.SimpleCore;
import com.github.mikecraft1224.simplecore.bus.EventRegistry;
import com.github.mikecraft1224.simplecore.bus.events.ReceiveParticleEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ParticleReceiveMixin {

    @Inject(method = "handleParticleEvent", at = @At("HEAD"), cancellable = true)
    private void onParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(ReceiveParticleEvent.class)) return;

        Vec3 pos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        Vec3 offset = new Vec3(packet.getXDist(), packet.getYDist(), packet.getZDist());

        boolean cancelled = EventRegistry.INSTANCE.post(() ->
            new ReceiveParticleEvent(packet.getParticle(), pos, packet.getCount(), packet.getMaxSpeed(), offset));

        if (cancelled) ci.cancel();
    }
}
