package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class ClientEffectOverlayRenderer {

    private static final ResourceLocation PUMPKIN_TEXTURE = new ResourceLocation("minecraft", "textures/misc/pumpkinblur.png");

    public static final IGuiOverlay PUMPKIN_OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
        if (ClientEffectState.pumpkinViewActive) {
            guiGraphics.blit(PUMPKIN_TEXTURE, 0, 0, 0.0F, 0.0F, width, height, width, height);
        }
    };

    public static final IGuiOverlay DVD_OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
        if (!ClientEffectState.dvdActive) return;

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
    };
}