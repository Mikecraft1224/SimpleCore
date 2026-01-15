package com.github.mikecraft1224.mixin;

import com.github.mikecraft1224.SimpleCore;
import com.github.mikecraft1224.bus.events.InventoryKeyPressEvent;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
	@Shadow
	protected Slot focusedSlot;

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void keyPressedPre(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(InventoryKeyPressEvent.class)) return;

		InventoryKeyPressEvent event = new InventoryKeyPressEvent(keyCode, scanCode, modifiers, this.focusedSlot).post();

		if (event.isCancelled()) {
			cir.setReturnValue(true);
			cir.cancel();
		}
	}
}