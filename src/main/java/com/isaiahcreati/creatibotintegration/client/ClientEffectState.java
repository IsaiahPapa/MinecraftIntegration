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

    private static final Map<String, Long> activeEffects = new HashMap<>();

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

    public static void clearAll() {
        activeEffects.clear();
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
        activeShaderId = null;
        shaderExpiryTick = 0;
        invertedControls = false;
        invertedControlsExpiryTick = 0;
        mouseDrifting = false;
        mouseDriftX = 0f;
        mouseDriftY = 0f;
        mouseDriftingExpiryTick = 0;
    }
}