package com.isaiahcreati.creatiintegration;

import net.minecraftforge.common.ForgeConfigSpec;
import oshi.util.tuples.Pair;

public class Config {
    public static ForgeConfigSpec.ConfigValue<String> ALERT_KEY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ALERT_KEY = builder
                .comment("Your Alert Key from Creati's Bot")
                .define("alert_key", ""); // Default value

        SPEC = builder.build();
    }
    public static final ForgeConfigSpec SPEC;


}
