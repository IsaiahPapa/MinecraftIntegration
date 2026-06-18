package com.isaiahcreati.creatibotintegration.mixin;

import com.isaiahcreati.creatibotintegration.client.ClientEffectState;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 0)
    private double invertMouseX(double x) {
        if (ClientEffectState.invertedControls) return -x;
        return x;
    }

    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 1)
    private double invertMouseY(double y) {
        if (ClientEffectState.invertedControls) return -y;
        return y;
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void driftMouse(CallbackInfo ci) {
        if (ClientEffectState.mouseDrifting) {
            this.accumulatedDX += ClientEffectState.mouseDriftX;
            this.accumulatedDY += ClientEffectState.mouseDriftY;
        }
    }
}