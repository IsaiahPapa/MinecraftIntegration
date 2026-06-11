package com.isaiahcreati.creatibotintegration.helpers;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

public class PrintStuff {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static void PrintEntities(){
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (String effectName : BuiltInRegistries.MOB_EFFECT.keySet().stream().map(val -> val.getPath()).toList()) {
            array.add(effectName);
        }
        LOGGER.info("Types of effects: " + array);
    }
}