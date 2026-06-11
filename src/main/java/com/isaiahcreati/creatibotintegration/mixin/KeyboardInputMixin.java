package com.isaiahcreati.creatibotintegration.mixin;

import com.isaiahcreati.creatibotintegration.client.ClientEffectState;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Redirect(
        method = "tick",
        at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;")
    )
    private Input invertInput(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean shift, boolean sprint) {
        if (ClientEffectState.invertedControls) {
            return new Input(!forward, !backward, !left, !right, jump, shift, sprint);
        }
        return new Input(forward, backward, left, right, jump, shift, sprint);
    }
}