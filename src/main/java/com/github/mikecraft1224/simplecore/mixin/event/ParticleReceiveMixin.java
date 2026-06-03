package com.github.mikecraft1224.simplecore.mixin.event;

import com.github.mikecraft1224.simplecore.SimpleCore;
import com.github.mikecraft1224.simplecore.bus.EventRegistry;
import com.github.mikecraft1224.simplecore.bus.events.ReceiveParticleEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ParticleReceiveMixin {

    @Inject(method = "onParticle", at = @At("HEAD"), cancellable = true)
    private void onParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(ReceiveParticleEvent.class)) return;

        Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
        Vec3d offset = new Vec3d(packet.getOffsetX(), packet.getOffsetY(), packet.getOffsetZ());

        boolean cancelled = EventRegistry.INSTANCE.post(() ->
            new ReceiveParticleEvent(packet.getParameters(), pos, packet.getCount(), packet.getSpeed(), offset));

        if (cancelled) ci.cancel();
    }
}
