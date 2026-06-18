package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = CreatiIntegration.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (ClientEffectState.fovOverride != 0f) {
            event.setNewFovModifier(event.getFovModifier() * (ClientEffectState.fovOverride / 70f));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientEffectManager.tick();
        ActivityFeedState.tick();
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ClientEffectState.cameraRollExpiryTick > 0 || ClientEffectState.drunkActive) {
            event.setRoll(ClientEffectState.cameraRoll);
        }
    }
}