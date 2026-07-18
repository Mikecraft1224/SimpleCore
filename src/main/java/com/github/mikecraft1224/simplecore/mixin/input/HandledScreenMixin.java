package com.github.mikecraft1224.simplecore.mixin.input;

import com.github.mikecraft1224.simplecore.SimpleCore;
import com.github.mikecraft1224.simplecore.bus.EventRegistry;
import com.github.mikecraft1224.simplecore.bus.events.InventoryKeyPressEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class HandledScreenMixin {
    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressedPre(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(InventoryKeyPressEvent.class)) return;

        boolean cancelled = EventRegistry.INSTANCE.post(() -> new InventoryKeyPressEvent(input.key(), input.scancode(), input.modifiers(), hoveredSlot));

        if (cancelled) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
