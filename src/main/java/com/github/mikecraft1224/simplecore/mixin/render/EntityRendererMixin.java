package com.github.mikecraft1224.simplecore.mixin.render;

import com.github.mikecraft1224.simplecore.render.internal.EntityOutlineRegistry;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "finalizeRenderState", at = @At("RETURN"))
    private void onFinalizeRenderState(Entity entity, EntityRenderState state, CallbackInfo ci) {
        Integer color = EntityOutlineRegistry.INSTANCE.colorFor(entity);
        if (color != null) {
            state.outlineColor = color;
        }
    }
}
