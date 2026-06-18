package com.isaiahcreati.creatibotintegration.client;

import java.util.HashMap;
import java.util.Map;

public class ClientEffectState {
    public static float fovOverride = 0f;
    public static long fovExpiryTick = 0;

    public static float cameraRoll = 0f;
    public static float cameraRollSpeed = 0f;
    public static long cameraRollExpiryTick = 0;

    public static boolean pumpkinViewActive = false;
    public static long pumpkinExpiryTick = 0;

    public static boolean dvdActive = false;
    public static long dvdExpiryTick = 0;
    public static float dvdX = 0f;
    public static float dvdY = 0f;
    public static float dvdVelX = 1.5f;
    public static float dvdVelY = 1.0f;
    public static final int DVD_WIDTH = 80;
    public static final int DVD_HEIGHT = 60;

    public static boolean jumpscareActive = false;
    public static long jumpscareExpiryTick = 0;
    public static final int JUMPSCARE_DURATION_TICKS = 40;

    public static String activeShaderId = null;
    public static long shaderExpiryTick = 0;

    public static boolean invertedControls = false;
    public static long invertedControlsExpiryTick = 0;

    public static boolean mouseDrifting = false;
    public static float mouseDriftX = 0f;
    public static float mouseDriftY = 0f;
    public static long mouseDriftingExpiryTick = 0;

    public static boolean drunkActive = false;
    public static long drunkExpiryTick = 0;
    public static float drunkPhase = 0f;

    public static boolean vignetteHeartbeatActive = false;
    public static long vignetteHeartbeatExpiryTick = 0;
    public static float vignetteHeartbeatPhase = 0f;

    private static final Map<String, Long> activeEffects = new HashMap<>();

    private static final Map<String, Long> pausedEffects = new HashMap<>();
    private static boolean effectsPaused = false;

    public static void activateEffect(String effectId, long expiryTick) {
        activeEffects.put(effectId, expiryTick);
    }

    public static boolean isEffectActive(String effectId, long currentTick) {
        Long expiry = activeEffects.get(effectId);
        if (expiry == null) return false;
        if (currentTick >= expiry) {
            activeEffects.remove(effectId);
            return false;
        }
        return true;
    }

    public static void deactivateEffect(String effectId) {
        activeEffects.remove(effectId);
    }

    public static void pauseAllVisualEffects(long currentTick) {
        effectsPaused = true;
        for (Map.Entry<String, Long> entry : activeEffects.entrySet()) {
            long remaining = entry.getValue() - currentTick;
            if (remaining > 0) {
                pausedEffects.put(entry.getKey(), remaining);
            }
        }
        ShaderManager.deactivateShader();
        activeShaderId = null;
        pumpkinViewActive = false;
        dvdActive = false;
        fovOverride = 0f;
        cameraRoll = 0f;
        cameraRollSpeed = 0f;
        invertedControls = false;
        mouseDrifting = false;
        drunkActive = false;
        vignetteHeartbeatActive = false;
        activeEffects.clear();
    }

    public static Map<String, Long> resumeAndGetPausedEffects() {
        effectsPaused = false;
        Map<String, Long> result = new HashMap<>(pausedEffects);
        pausedEffects.clear();
        return result;
    }

    public static boolean isEffectsPaused() {
        return effectsPaused;
    }

    public static void clearAll() {
        activeEffects.clear();
        pausedEffects.clear();
        effectsPaused = false;
        fovOverride = 0f;
        fovExpiryTick = 0;
        cameraRoll = 0f;
        cameraRollSpeed = 0f;
        cameraRollExpiryTick = 0;
        pumpkinViewActive = false;
        pumpkinExpiryTick = 0;
        dvdActive = false;
        dvdExpiryTick = 0;
        jumpscareActive = false;
        jumpscareExpiryTick = 0;
        if (activeShaderId != null) {
            ShaderManager.deactivateShader();
        }
        invertedControls = false;
        invertedControlsExpiryTick = 0;
        mouseDrifting = false;
        mouseDriftX = 0f;
        mouseDriftY = 0f;
        mouseDriftingExpiryTick = 0;
        drunkActive = false;
        drunkExpiryTick = 0;
        drunkPhase = 0f;
        vignetteHeartbeatActive = false;
        vignetteHeartbeatExpiryTick = 0;
        vignetteHeartbeatPhase = 0f;
    }
}