package com.isaiahcreati.creatibotintegration.mixin;

import com.isaiahcreati.creatibotintegration.client.ClientEffectState;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(boolean sneaking, float sneakSpeed, CallbackInfo ci) {
        if (ClientEffectState.invertedControls) {
            KeyboardInput self = (KeyboardInput) (Object) this;
            self.leftImpulse = -self.leftImpulse;
            self.forwardImpulse = -self.forwardImpulse;

            boolean tmpUp = self.up;
            self.up = self.down;
            self.down = tmpUp;

            boolean tmpLeft = self.left;
            self.left = self.right;
            self.right = tmpLeft;
        }
    }
}