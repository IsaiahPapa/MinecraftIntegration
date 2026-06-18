package com.isaiahcreati.creatibotintegration.mixin;

import com.isaiahcreati.creatibotintegration.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Function;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow
    @Final
    public Options options;

    @Inject(method = "addInitialScreens", at = @At("HEAD"))
    private void creatibotintegration$skipAccessibilityOnboarding(List<Function<Runnable, Screen>> screens, CallbackInfoReturnable<Boolean> cir) {
        if (Config.SKIP_ACCESSIBILITY_ONBOARDING.get()) {
            this.options.onboardAccessibility = false;
        }
    }
}
