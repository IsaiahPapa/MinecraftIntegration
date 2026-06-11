package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public class ShaderManager {

    public static final ResourceLocation INVERT = new ResourceLocation("minecraft", "shaders/post/invert.json");
    public static final ResourceLocation BLUR = new ResourceLocation("minecraft", "shaders/post/blur.json");
    public static final ResourceLocation WOBBLE = new ResourceLocation("minecraft", "shaders/post/wobble.json");
    public static final ResourceLocation BLACK_AND_WHITE = new ResourceLocation(CreatiIntegration.MODID, "shaders/post/black_and_white.json");
    public static final ResourceLocation CRT = new ResourceLocation(CreatiIntegration.MODID, "shaders/post/crt.json");

    private static PostChain activeChain = null;
    private static ResourceLocation activeShaderId = null;

    public static void activateShader(ResourceLocation shaderId) {
        if (activeShaderId != null && activeShaderId.equals(shaderId)) return;
        deactivateShader();

        Minecraft mc = Minecraft.getInstance();
        try {
            PostChain chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), shaderId);
            chain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            activeChain = chain;
            activeShaderId = shaderId;
        } catch (IOException e) {
            CreatiIntegration.LOGGER.error("Failed to load shader: {}", shaderId, e);
        }
    }

    public static void deactivateShader() {
        if (activeChain != null) {
            activeChain.close();
            activeChain = null;
            activeShaderId = null;
        }
    }

    public static void render(float partialTick) {
        if (activeChain == null) return;
        Minecraft mc = Minecraft.getInstance();
        activeChain.process(partialTick);
        mc.getMainRenderTarget().bindWrite(true);
    }

    public static void onResolutionChanged(int width, int height) {
        if (activeChain != null) {
            activeChain.resize(width, height);
        }
    }

    public static boolean hasActiveShader() {
        return activeChain != null;
    }

    public static ResourceLocation getActiveShaderId() {
        return activeShaderId;
    }
}