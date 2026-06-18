package com.isaiahcreati.creatibotintegration.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.gui.GuiLayer;

public class ClientEffectOverlayRenderer {

    private static final Identifier PUMPKIN_TEXTURE = Identifier.withDefaultNamespace("textures/misc/pumpkinblur.png");

    public static final Identifier PUMPKIN_OVERLAY_ID = Identifier.fromNamespaceAndPath("creatibotintegration", "pumpkin_overlay");
    public static final Identifier DVD_OVERLAY_ID = Identifier.fromNamespaceAndPath("creatibotintegration", "dvd_overlay");
    public static final Identifier VIGNETTE_HEARTBEAT_OVERLAY_ID = Identifier.fromNamespaceAndPath("creatibotintegration", "vignette_heartbeat");

    public static void renderPumpkinOverlay(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (ClientEffectState.pumpkinViewActive) {
            int width = guiGraphics.guiWidth();
            int height = guiGraphics.guiHeight();
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PUMPKIN_TEXTURE, 0, 0, 0.0f, 0.0f, width, height, width, height, width, height);
        }
    }

    public static void renderDvdOverlay(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (!ClientEffectState.dvdActive) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        int left = (int) ClientEffectState.dvdX;
        int top = (int) ClientEffectState.dvdY;
        int right = left + ClientEffectState.DVD_WIDTH;
        int bottom = top + ClientEffectState.DVD_HEIGHT;

        guiGraphics.fill(0, 0, width, top, 0xFF000000);
        guiGraphics.fill(0, 0, left, height, 0xFF000000);
        guiGraphics.fill(0, bottom, width, height, 0xFF000000);
        guiGraphics.fill(right, 0, width, height, 0xFF000000);

        int border = 2;
        int borderColor = 0xFF4040FF;
        guiGraphics.fill(left - border, top - border, right + border, top, borderColor);
        guiGraphics.fill(left - border, bottom, right + border, bottom + border, borderColor);
        guiGraphics.fill(left - border, top, left, bottom + border, borderColor);
        guiGraphics.fill(right, top - border, right + border, bottom + border, borderColor);
    }

    public static void renderVignetteHeartbeat(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (!ClientEffectState.vignetteHeartbeatActive) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int thickness = 30;
        float alpha = 0.30f + 0.40f * (0.5f + 0.5f * (float) Math.sin(ClientEffectState.vignetteHeartbeatPhase * 2.0f));
        int a = (int) (alpha * 255.0f) & 0xFF;
        int color = (a << 24) | 0xFF0000;

        guiGraphics.fill(0, 0, width, thickness, color);
        guiGraphics.fill(0, height - thickness, width, height, color);
        guiGraphics.fill(0, 0, thickness, height, color);
        guiGraphics.fill(width - thickness, 0, width, height, color);
    }
}