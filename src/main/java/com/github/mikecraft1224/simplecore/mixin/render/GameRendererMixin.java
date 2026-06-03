package com.github.mikecraft1224.simplecore.mixin.render;

import com.github.mikecraft1224.simplecore.render.internal.PipelineRenderer;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "close", at = @At("RETURN"))
    private void onClose(CallbackInfo ci) {
        PipelineRenderer.INSTANCE.close();
    }
}
