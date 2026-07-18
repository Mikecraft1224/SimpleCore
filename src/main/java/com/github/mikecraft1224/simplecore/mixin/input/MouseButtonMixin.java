package com.github.mikecraft1224.simplecore.mixin.input;

import com.github.mikecraft1224.simplecore.SimpleCore;
import com.github.mikecraft1224.simplecore.bus.EventRegistry;
import com.github.mikecraft1224.simplecore.bus.events.HudMouseClickEvent;
import com.github.mikecraft1224.simplecore.input.internal.ScreenTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseButtonMixin {

    @Shadow private double xpos;
    @Shadow private double ypos;

    @Inject(method = "onButton", at = @At("HEAD"))
    private void onButtonPre(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;
        if (ScreenTracker.INSTANCE.getCurrentScreen() != null) return;
        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(HudMouseClickEvent.class)) return;
        Minecraft client = Minecraft.getInstance();
        int mx = (int) MouseHandler.getScaledXPos(client.getWindow(), xpos);
        int my = (int) MouseHandler.getScaledYPos(client.getWindow(), ypos);
        EventRegistry.INSTANCE.post(() -> new HudMouseClickEvent(mx, my, buttonInfo.button()));
    }
}
