package com.github.mikecraft1224.simplecore.mixin.input;

import com.github.mikecraft1224.simplecore.SimpleCore;
import com.github.mikecraft1224.simplecore.bus.EventRegistry;
import com.github.mikecraft1224.simplecore.bus.events.HudMouseClickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseButtonMixin {

    @Shadow private double x;
    @Shadow private double y;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButtonPre(long window, MouseInput input, int action, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) return;
        if (!SimpleCore.INSTANCE.getEVENTBUS().existHandlers(HudMouseClickEvent.class)) return;
        double scale = client.getWindow().getScaleFactor();
        int mx = (int) (x / scale);
        int my = (int) (y / scale);
        EventRegistry.INSTANCE.post(() -> new HudMouseClickEvent(mx, my, input.button()));
    }
}
