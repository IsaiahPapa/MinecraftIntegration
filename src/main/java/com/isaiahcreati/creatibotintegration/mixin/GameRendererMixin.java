package com.isaiahcreati.creatibotintegration.mixin;

import com.isaiahcreati.creatibotintegration.client.ClientEffectState;
import com.isaiahcreati.creatibotintegration.client.ShaderManager;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(float partialTick, long nanoTime, boolean renderLevelIn, CallbackInfo ci) {
        if (ClientEffectState.activeShaderId != null && !ClientEffectState.activeShaderId.isEmpty()) {
            ShaderManager.render(partialTick);
        }
    }

    @Inject(method = "resize", at = @At("TAIL"))
    private void onResize(int width, int height, CallbackInfo ci) {
        ShaderManager.onResolutionChanged(width, height);
    }
}