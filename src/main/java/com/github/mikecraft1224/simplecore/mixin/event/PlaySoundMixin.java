package com.github.mikecraft1224.simplecore.mixin.event;

import com.github.mikecraft1224.simplecore.SimpleCore;
import com.github.mikecraft1224.simplecore.bus.EventRegistry;
import com.github.mikecraft1224.simplecore.bus.events.PlaySoundEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class PlaySoundMixin {

    @Inject(method = "onPlaySound", at = @At("HEAD"), cancellable = true)
    private void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(PlaySoundEvent.class)) return;

        Identifier soundId = packet.getSound().getKey()
            .map(RegistryKey::getValue)
            .orElse(Identifier.of("unknown", "unknown"));

        Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());

        boolean cancelled = EventRegistry.INSTANCE.post(() ->
            new PlaySoundEvent(soundId, pos, packet.getVolume(), packet.getPitch()));

        if (cancelled) ci.cancel();
    }
}
