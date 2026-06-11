package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatiIntegration.MODID, value = net.minecraftforge.api.distmarker.Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEventHandler {

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("pumpkin_overlay", ClientEffectOverlayRenderer.PUMPKIN_OVERLAY);
        event.registerAboveAll("dvd_overlay", ClientEffectOverlayRenderer.DVD_OVERLAY);
    }
}