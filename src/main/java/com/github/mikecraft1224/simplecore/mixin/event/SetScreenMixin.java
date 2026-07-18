package com.github.mikecraft1224.simplecore.mixin.event;

import com.github.mikecraft1224.simplecore.SimpleCore;
import com.github.mikecraft1224.simplecore.bus.EventRegistry;
import com.github.mikecraft1224.simplecore.bus.events.GuiScreenOpenEvent;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets {@link Gui#setScreen}, not {@code Minecraft.setScreenAndShow} - the latter just
 * delegates to this method internally, so hooking here also catches direct {@code gui.setScreen}
 * calls (used to close screens, since {@code setScreenAndShow}'s parameter is non-null).
 */
@Mixin(Gui.class)
public class SetScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void setScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(GuiScreenOpenEvent.class)) return;
        boolean cancelled = EventRegistry.INSTANCE.post(() -> new GuiScreenOpenEvent(screen));
        if (cancelled) ci.cancel();
    }
}
