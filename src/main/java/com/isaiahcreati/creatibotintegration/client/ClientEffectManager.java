package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.client.Minecraft;

import java.util.Random;

public class ClientEffectManager {

    private static final Random random = new Random();

    public static void activateEffect(String effectId, int durationSeconds) {
        Minecraft mc = Minecraft.getInstance();
        long expiryTick = mc.level.getGameTime() + (durationSeconds * 20L);

        switch (effectId) {
            case "fov_quake" -> {
                ClientEffectState.fovOverride = 170f;
                ClientEffectState.fovExpiryTick = expiryTick;
            }
            case "fov_zoom" -> {
                ClientEffectState.fovOverride = 10f;
                ClientEffectState.fovExpiryTick = expiryTick;
            }
            case "upside_down" -> {
                ClientEffectState.cameraRoll = 180f;
                ClientEffectState.cameraRollSpeed = 0f;
                ClientEffectState.cameraRollExpiryTick = expiryTick;
            }
            case "rolling_camera" -> {
                ClientEffectState.cameraRoll = 0f;
                ClientEffectState.cameraRollSpeed = 1.25f;
                ClientEffectState.cameraRollExpiryTick = expiryTick;
            }
            case "camera_tilt" -> {
                ClientEffectState.cameraRoll = random.nextFloat() * 60f - 30f;
                ClientEffectState.cameraRollSpeed = 0f;
                ClientEffectState.cameraRollExpiryTick = expiryTick;
            }
            case "pumpkin_view" -> {
                ClientEffectState.pumpkinViewActive = true;
                ClientEffectState.pumpkinExpiryTick = expiryTick;
            }
            case "dvd" -> {
                ClientEffectState.dvdActive = true;
                ClientEffectState.dvdExpiryTick = expiryTick;
                ClientEffectState.dvdX = 50f;
                ClientEffectState.dvdY = 50f;
                ClientEffectState.dvdVelX = random.nextBoolean() ? 1.5f : -1.5f;
                ClientEffectState.dvdVelY = random.nextBoolean() ? 1.0f : -1.0f;
            }
            case "blur" -> {
                ClientEffectState.activeShaderId = "blur";
                ClientEffectState.shaderExpiryTick = expiryTick;
                ShaderManager.activateShader(ShaderManager.BLUR);
            }
            case "inverted_colors" -> {
                ClientEffectState.activeShaderId = "inverted_colors";
                ClientEffectState.shaderExpiryTick = expiryTick;
                ShaderManager.activateShader(ShaderManager.INVERT);
            }
            case "black_and_white" -> {
                ClientEffectState.activeShaderId = "black_and_white";
                ClientEffectState.shaderExpiryTick = expiryTick;
                ShaderManager.activateShader(ShaderManager.BLACK_AND_WHITE);
            }
            case "lsd" -> {
                ClientEffectState.activeShaderId = "lsd";
                ClientEffectState.shaderExpiryTick = expiryTick;
                ShaderManager.activateShader(ShaderManager.INVERT);
            }
            case "crt" -> {
                ClientEffectState.activeShaderId = "crt";
                ClientEffectState.shaderExpiryTick = expiryTick;
                ShaderManager.activateShader(ShaderManager.CRT);
            }
            case "inverted_controls" -> {
                ClientEffectState.invertedControls = true;
                ClientEffectState.invertedControlsExpiryTick = expiryTick;
            }
            case "mouse_drifting" -> {
                ClientEffectState.mouseDrifting = true;
                ClientEffectState.mouseDriftX = (random.nextBoolean() ? 1 : -1) * 1.5f;
                ClientEffectState.mouseDriftY = (random.nextBoolean() ? 1 : -1) * 0.1f;
                ClientEffectState.mouseDriftingExpiryTick = expiryTick;
            }
            default -> CreatiIntegration.LOGGER.warn("Unknown client effect: {}", effectId);
        }

        ClientEffectState.activateEffect(effectId, expiryTick);
        CreatiIntegration.LOGGER.info("Activated client effect '{}' for {} seconds", effectId, durationSeconds);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long currentTick = mc.level.getGameTime();

        if (ClientEffectState.fovExpiryTick > 0 && currentTick >= ClientEffectState.fovExpiryTick) {
            ClientEffectState.fovOverride = 0f;
            ClientEffectState.fovExpiryTick = 0;
        }

        if (ClientEffectState.cameraRollExpiryTick > 0 && currentTick >= ClientEffectState.cameraRollExpiryTick) {
            ClientEffectState.cameraRoll = 0f;
            ClientEffectState.cameraRollSpeed = 0f;
            ClientEffectState.cameraRollExpiryTick = 0;
        }

        if (ClientEffectState.pumpkinExpiryTick > 0 && currentTick >= ClientEffectState.pumpkinExpiryTick) {
            ClientEffectState.pumpkinViewActive = false;
            ClientEffectState.pumpkinExpiryTick = 0;
        }

        if (ClientEffectState.dvdExpiryTick > 0 && currentTick >= ClientEffectState.dvdExpiryTick) {
            ClientEffectState.dvdActive = false;
            ClientEffectState.dvdExpiryTick = 0;
        }

        if (ClientEffectState.shaderExpiryTick > 0 && currentTick >= ClientEffectState.shaderExpiryTick) {
            ClientEffectState.activeShaderId = null;
            ClientEffectState.shaderExpiryTick = 0;
            ShaderManager.deactivateShader();
        }

        if (ClientEffectState.invertedControlsExpiryTick > 0 && currentTick >= ClientEffectState.invertedControlsExpiryTick) {
            ClientEffectState.invertedControls = false;
            ClientEffectState.invertedControlsExpiryTick = 0;
        }

        if (ClientEffectState.mouseDriftingExpiryTick > 0 && currentTick >= ClientEffectState.mouseDriftingExpiryTick) {
            ClientEffectState.mouseDrifting = false;
            ClientEffectState.mouseDriftX = 0f;
            ClientEffectState.mouseDriftY = 0f;
            ClientEffectState.mouseDriftingExpiryTick = 0;
        }

        if (ClientEffectState.cameraRollSpeed != 0f) {
            ClientEffectState.cameraRoll = (ClientEffectState.cameraRoll + ClientEffectState.cameraRollSpeed) % 360f;
        }

        if (ClientEffectState.dvdActive) {
            ClientEffectState.dvdX += ClientEffectState.dvdVelX;
            ClientEffectState.dvdY += ClientEffectState.dvdVelY;
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            if (ClientEffectState.dvdX <= 0 || ClientEffectState.dvdX + ClientEffectState.DVD_WIDTH >= screenWidth) {
                ClientEffectState.dvdVelX = -ClientEffectState.dvdVelX;
            }
            if (ClientEffectState.dvdY <= 0 || ClientEffectState.dvdY + ClientEffectState.DVD_HEIGHT >= screenHeight) {
                ClientEffectState.dvdVelY = -ClientEffectState.dvdVelY;
            }
            ClientEffectState.dvdX = Math.max(0, Math.min(ClientEffectState.dvdX, screenWidth - ClientEffectState.DVD_WIDTH));
            ClientEffectState.dvdY = Math.max(0, Math.min(ClientEffectState.dvdY, screenHeight - ClientEffectState.DVD_HEIGHT));
        }
    }
}