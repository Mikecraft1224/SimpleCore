package com.github.mikecraft1224.simplecore.mixin.event;

import com.github.mikecraft1224.simplecore.SimpleCore;
import com.github.mikecraft1224.simplecore.bus.EventRegistry;
import com.github.mikecraft1224.simplecore.bus.events.PlaySoundEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class PlaySoundMixin {

    @Inject(method = "handleSoundEvent", at = @At("HEAD"), cancellable = true)
    private void onPlaySound(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(PlaySoundEvent.class)) return;

        Identifier soundId = packet.getSound().unwrapKey()
            .map(ResourceKey::identifier)
            .orElse(Identifier.fromNamespaceAndPath("unknown", "unknown"));

        Vec3 pos = new Vec3(packet.getX(), packet.getY(), packet.getZ());

        boolean cancelled = EventRegistry.INSTANCE.post(() ->
            new PlaySoundEvent(soundId, pos, packet.getVolume(), packet.getPitch()));

        if (cancelled) ci.cancel();
    }
}
