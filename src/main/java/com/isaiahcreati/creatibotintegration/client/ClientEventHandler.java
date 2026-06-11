package com.isaiahcreati.creatibotintegration.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.isaiahcreati.creatibotintegration.CreatiIntegration.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (ClientEffectState.fovOverride != 0f) {
            event.setFOV(ClientEffectState.fovOverride);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ClientEffectState.cameraRoll != 0f) {
            event.setRoll(ClientEffectState.cameraRoll);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientEffectManager.tick();
    }
}