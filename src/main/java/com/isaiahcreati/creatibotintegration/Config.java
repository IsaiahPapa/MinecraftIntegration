package com.isaiahcreati.creatibotintegration;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static ForgeConfigSpec.ConfigValue<String> ALERT_KEY;
    public static ForgeConfigSpec.ConfigValue<Boolean> CHAT_ALERTS;
    public static ForgeConfigSpec.ConfigValue<Boolean> CONNECT_ON_LOAD;
    public static final String CATEGORY_GENERAL = "General";
    public static final String CATEGORY_CHAT_ALERTS = "Alerts";
    public static final ForgeConfigSpec CLIENT_CONFIG;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("General Settings").push(CATEGORY_GENERAL);

        ALERT_KEY = builder
                .comment("Your Alert Key from Creati's Bot")
                .define("alert_key", "");

        builder.comment("Chat Alerts").push(CATEGORY_CHAT_ALERTS);

        CHAT_ALERTS = builder
                .comment("Do you want any chat alerts to show?")
                .define("chat_alerts.enabled", true);


        CLIENT_CONFIG = builder.build();
    }



}
