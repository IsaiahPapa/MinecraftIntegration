package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = CreatiIntegration.MODID, value = Dist.CLIENT)
public class ClientModEventHandler {

    private static boolean registered = false;

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        if (registered) return;
        registered = true;
        event.registerAbove(VanillaGuiLayers.SLEEP_OVERLAY, ClientEffectOverlayRenderer.PUMPKIN_OVERLAY_ID, ClientEffectOverlayRenderer::renderPumpkinOverlay);
        event.registerAbove(VanillaGuiLayers.SLEEP_OVERLAY, ClientEffectOverlayRenderer.DVD_OVERLAY_ID, ClientEffectOverlayRenderer::renderDvdOverlay);
        event.registerAbove(VanillaGuiLayers.SLEEP_OVERLAY, ScoreboardOverlayRenderer.QUEUE_OVERLAY_ID, ScoreboardOverlayRenderer::render);
        event.registerAbove(VanillaGuiLayers.SLEEP_OVERLAY, ActivityFeedRenderer.ACTIVITY_FEED_ID, ActivityFeedRenderer::render);
    }
}