package com.github.mikecraft1224.simplecore.mixin.event;

import com.github.mikecraft1224.simplecore.SimpleCore;
import com.github.mikecraft1224.simplecore.bus.EventRegistry;
import com.github.mikecraft1224.simplecore.bus.events.ServerBlockChangeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ServerBlockChangeMixin {

    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(ServerBlockChangeEvent.class)) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        BlockPos pos = packet.getPos();
        BlockState oldState = client.level.getBlockState(pos);
        BlockState newState = packet.getBlockState();

        EventRegistry.INSTANCE.post(() -> new ServerBlockChangeEvent(pos, oldState, newState));
    }
}
