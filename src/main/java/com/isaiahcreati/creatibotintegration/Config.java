package com.isaiahcreati.creatibotintegration;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec CLIENT_CONFIG;

    public static final ModConfigSpec.ConfigValue<String> ALERT_KEY;
    public static final ModConfigSpec.ConfigValue<Boolean> CHAT_ALERTS;
    public static final ModConfigSpec.ConfigValue<Boolean> PARKOUR_ENABLED;
    public static final ModConfigSpec.ConfigValue<Integer> PARKOUR_DURATION_SECONDS;
    public static final ModConfigSpec.ConfigValue<Integer> PARKOUR_FAIL_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Boolean> TNT_RUN_ENABLED;
    public static final ModConfigSpec.ConfigValue<Integer> TNT_RUN_DURATION_SECONDS;
    public static final ModConfigSpec.ConfigValue<Integer> TNT_RUN_FAIL_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Integer> TNT_RUN_DECAY_DELAY_TICKS;
    public static final ModConfigSpec.ConfigValue<Integer> TNT_RUN_GRACE_PERIOD_SECONDS;
    public static final ModConfigSpec.ConfigValue<Boolean> DROPPER_ENABLED;
    public static final ModConfigSpec.ConfigValue<Integer> DROPPER_FAIL_DAMAGE;
    public static final ModConfigSpec.IntValue CONFIG_VERSION;
    public static final String CATEGORY_GENERAL = "General";
    public static final String CATEGORY_CHAT_ALERTS = "Alerts";
    public static final int CURRENT_CONFIG_VERSION = 3;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("General Settings").push(CATEGORY_GENERAL);

        CONFIG_VERSION = builder
                .comment("Config version - do not edit")
                .defineInRange("config_version", CURRENT_CONFIG_VERSION, 1, 100);

        ALERT_KEY = builder
                .comment("Your Alert Key from Creati's Bot")
                .define("alert_key", "");

        builder.comment("Chat Alerts").push(CATEGORY_CHAT_ALERTS);

        CHAT_ALERTS = builder
                .comment("Do you want any chat alerts to show?")
                .define("chat_alerts.enabled", true);

        builder.pop();

        builder.comment("Parkour Course Settings").push("Parkour");

        PARKOUR_ENABLED = builder
                .comment("Enable the parkour course taunt")
                .define("parkour.enabled", true);

        PARKOUR_DURATION_SECONDS = builder
                .comment("Time limit in seconds to complete the parkour course")
                .defineInRange("parkour.duration_seconds", 15, 5, 120);

        PARKOUR_FAIL_DAMAGE = builder
                .comment("Damage dealt on failing to complete the parkour course (2 damage = 1 heart)")
                .defineInRange("parkour.fail_damage", 8, 0, 40);

        builder.pop();

        builder.comment("TNT Run Settings").push("TntRun");

        TNT_RUN_ENABLED = builder
                .comment("Enable the TNT Run taunt")
                .define("tntrun.enabled", true);

        TNT_RUN_DURATION_SECONDS = builder
                .comment("Time in seconds the player must survive")
                .defineInRange("tntrun.duration_seconds", 30, 5, 120);

        TNT_RUN_FAIL_DAMAGE = builder
                .comment("Damage dealt on falling into the void (2 damage = 1 heart)")
                .defineInRange("tntrun.fail_damage", 8, 0, 40);

        TNT_RUN_DECAY_DELAY_TICKS = builder
                .comment("Delay in ticks before a stepped-on block disappears (20 ticks = 1 second)")
                .defineInRange("tntrun.decay_delay_ticks", 8, 2, 100);

        TNT_RUN_GRACE_PERIOD_SECONDS = builder
                .comment("Grace period in seconds before blocks start decaying (countdown: 3, 2, 1, GO!)")
                .defineInRange("tntrun.grace_period_seconds", 3, 0, 10);

        builder.pop();

        builder.comment("Dropper Settings").push("Dropper");

        DROPPER_ENABLED = builder
                .comment("Enable the Dropper taunt")
                .define("dropper.enabled", true);

        DROPPER_FAIL_DAMAGE = builder
                .comment("Damage dealt on missing the water landing (2 damage = 1 heart)")
                .defineInRange("dropper.fail_damage", 8, 0, 40);

        builder.pop();

        CLIENT_CONFIG = builder.build();
    }

    public static boolean needsReset() {
        return CONFIG_VERSION.get() < CURRENT_CONFIG_VERSION;
    }

    public static void resetToDefaults() {
        TNT_RUN_DECAY_DELAY_TICKS.set(8);
        TNT_RUN_GRACE_PERIOD_SECONDS.set(3);
        TNT_RUN_DURATION_SECONDS.set(30);
        TNT_RUN_FAIL_DAMAGE.set(8);
        TNT_RUN_ENABLED.set(true);
        PARKOUR_ENABLED.set(true);
        PARKOUR_DURATION_SECONDS.set(15);
        PARKOUR_FAIL_DAMAGE.set(8);
        DROPPER_ENABLED.set(true);
        DROPPER_FAIL_DAMAGE.set(8);
        CONFIG_VERSION.set(CURRENT_CONFIG_VERSION);
    }
}