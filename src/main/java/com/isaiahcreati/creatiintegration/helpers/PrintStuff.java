package com.isaiahcreati.creatiintegration.helpers;

import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public class PrintStuff {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static void PrintEntities(){
        JsonArray array = new JsonArray();
        for (String effectName : ForgeRegistries.MOB_EFFECTS.getKeys().stream().map(val -> val.getPath()).toList()) {
            array.add(effectName);
        }
        LOGGER.info("Types of effects: " + array);
    }
}
