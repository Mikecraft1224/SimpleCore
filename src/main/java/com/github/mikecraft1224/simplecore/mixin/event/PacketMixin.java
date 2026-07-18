package com.github.mikecraft1224.simplecore.mixin.event;

import com.github.mikecraft1224.simplecore.SimpleCore;
import com.github.mikecraft1224.simplecore.bus.EventRegistry;
import com.github.mikecraft1224.simplecore.bus.events.PacketReceiveEvent;
import com.github.mikecraft1224.simplecore.bus.events.PacketSendEvent;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Generic packet interception - both hooks target the single choke point every packet actually
 * flows through ({@code channelRead0} for inbound, {@code doSendPacket} for outbound), rather
 * than any one of the several public {@code send} overloads, so every packet is only observed once.
 */
@Mixin(Connection.class)
public class PacketMixin {

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void onPacketReceive(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(PacketReceiveEvent.class)) return;
        boolean cancelled = EventRegistry.INSTANCE.post(() -> new PacketReceiveEvent(packet));
        if (cancelled) ci.cancel();
    }

    @Inject(method = "doSendPacket", at = @At("HEAD"), cancellable = true)
    private void onPacketSend(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(PacketSendEvent.class)) return;
        boolean cancelled = EventRegistry.INSTANCE.post(() -> new PacketSendEvent(packet));
        if (cancelled) ci.cancel();
    }
}
