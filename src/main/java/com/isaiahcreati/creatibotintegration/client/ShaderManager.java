package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class ShaderManager {

    public static final Identifier INVERT = Identifier.withDefaultNamespace("invert");
    public static final Identifier BLUR = Identifier.withDefaultNamespace("blur");
    public static final Identifier BLACK_AND_WHITE = Identifier.fromNamespaceAndPath(CreatiIntegration.MODID, "black_and_white");
    public static final Identifier CRT = Identifier.fromNamespaceAndPath(CreatiIntegration.MODID, "crt");

    private static Identifier activeShaderId = null;

    public static void activateShader(Identifier shaderId) {
        if (activeShaderId != null && activeShaderId.equals(shaderId)) return;
        deactivateShader();

        Minecraft.getInstance().gameRenderer.setPostEffect(shaderId);
        activeShaderId = shaderId;
        CreatiIntegration.LOGGER.info("Shader activated: {}", shaderId);
    }

    public static void deactivateShader() {
        if (activeShaderId != null) {
            Minecraft.getInstance().gameRenderer.clearPostEffect();
            activeShaderId = null;
        }
    }

    public static boolean hasActiveShader() {
        return activeShaderId != null;
    }

    public static Identifier getActiveShaderId() {
        return activeShaderId;
    }
}