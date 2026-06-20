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
    public static final ModConfigSpec.ConfigValue<Integer> TNT_RUN_FLOOR_SIZE;
    public static final ModConfigSpec.ConfigValue<Integer> TNT_RUN_FLOOR_COUNT;
    public static final ModConfigSpec.ConfigValue<Boolean> DROPPER_ENABLED;
    public static final ModConfigSpec.ConfigValue<Integer> DROPPER_FAIL_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Integer> DROPPER_WATER_SIZE;
    public static final ModConfigSpec.ConfigValue<Boolean> SUMO_ENABLED;
    public static final ModConfigSpec.ConfigValue<Integer> SUMO_FAIL_DAMAGE;
    public static final ModConfigSpec.ConfigValue<Integer> SUMO_ARENA_RADIUS;
    public static final ModConfigSpec.ConfigValue<Integer> SUMO_MOB_MIN_COUNT;
    public static final ModConfigSpec.ConfigValue<Integer> SUMO_MOB_MAX_COUNT;
    public static final ModConfigSpec.ConfigValue<Boolean> QUEUE_ENABLED;
    public static final ModConfigSpec.ConfigValue<Boolean> SIDEBAR_VISIBLE;
    public static final ModConfigSpec.ConfigValue<Boolean> ACTIVITY_FEED_VISIBLE;
    public static final ModConfigSpec.ConfigValue<Integer> STAGGER_DELAY_TICKS;
    public static final ModConfigSpec.ConfigValue<Boolean> AUTO_CONNECT;
    public static final ModConfigSpec.ConfigValue<Boolean> ONBOARDED;
    public static final ModConfigSpec.ConfigValue<Boolean> SKIP_ACCESSIBILITY_ONBOARDING;
    public static final ModConfigSpec.ConfigValue<Boolean> REMOVE_REALMS_BUTTON;
    public static final ModConfigSpec.IntValue CONFIG_VERSION;
    public static final String CATEGORY_GENERAL = "General";
    public static final String CATEGORY_CHAT_ALERTS = "Alerts";
    public static final String CATEGORY_QUEUE = "Queue";
    public static final int CURRENT_CONFIG_VERSION = 11;

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
                .defineInRange("parkour.duration_seconds", 25, 5, 120);

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

        TNT_RUN_FLOOR_SIZE = builder
                .comment("Size of each TNT Run floor (e.g. 16 = 16x16). Smaller = harder.")
                .defineInRange("tntrun.floor_size", 16, 8, 32);

        TNT_RUN_FLOOR_COUNT = builder
                .comment("Number of stacked floors (1-3). Fewer = harder.")
                .defineInRange("tntrun.floor_count", 2, 1, 3);

        builder.pop();

        builder.comment("Dropper Settings").push("Dropper");

        DROPPER_ENABLED = builder
                .comment("Enable the Dropper taunt")
                .define("dropper.enabled", true);

        DROPPER_FAIL_DAMAGE = builder
                .comment("Damage dealt on missing the water landing (2 damage = 1 heart)")
                .defineInRange("dropper.fail_damage", 8, 0, 40);

        DROPPER_WATER_SIZE = builder
                .comment("Size of the water landing pad (1 = 1x1, 2 = 2x2, 3 = 3x3). Smaller = harder.")
                .defineInRange("dropper.water_size", 2, 1, 3);

        builder.pop();

        builder.comment("Sumo Arena Settings").push("Sumo");

        SUMO_ENABLED = builder
                .comment("Enable the Sumo Arena minigame")
                .define("sumo.enabled", true);

        SUMO_FAIL_DAMAGE = builder
                .comment("Damage dealt on being knocked off the arena (2 damage = 1 heart)")
                .defineInRange("sumo.fail_damage", 8, 0, 40);

        SUMO_ARENA_RADIUS = builder
                .comment("Radius of the sumo platform (blocks)")
                .defineInRange("sumo.arena_radius", 8, 4, 12);

        SUMO_MOB_MIN_COUNT = builder
                .comment("Minimum number of mobs spawned in the arena ring")
                .defineInRange("sumo.mob_min_count", 3, 1, 30);

        SUMO_MOB_MAX_COUNT = builder
                .comment("Maximum number of mobs spawned in the arena ring")
                .defineInRange("sumo.mob_max_count", 5, 1, 40);

        builder.pop();

        builder.comment("Queue Settings").push(CATEGORY_QUEUE);

        QUEUE_ENABLED = builder
                .comment("Enable the queue system for minigames and visual effects")
                .define("queue.enabled", true);

        SIDEBAR_VISIBLE = builder
                .comment("Show the queue sidebar on screen")
                .define("queue.sidebar_visible", true);

        ACTIVITY_FEED_VISIBLE = builder
                .comment("Show the activity feed toast notifications on screen")
                .define("queue.activity_feed_visible", true);

        STAGGER_DELAY_TICKS = builder
                .comment("Delay in ticks between releasing pending taunts after a minigame ends (20 ticks = 1 second)")
                .defineInRange("queue.stagger_delay_ticks", 20, 0, 100);

        builder.pop();

        AUTO_CONNECT = builder
                .comment("Automatically connect to SocketIO when the server starts if an alert key is configured")
                .define("auto_connect", true);

        ONBOARDED = builder
                .comment("Whether the onboarding setup has been completed")
                .define("onboarded", false);

        SKIP_ACCESSIBILITY_ONBOARDING = builder
                .comment("Skip the Minecraft accessibility onboarding screen on first launch")
                .define("ui.skip_accessibility_onboarding", true);

        REMOVE_REALMS_BUTTON = builder
                .comment("Remove the Realms button from the main menu")
                .define("ui.remove_realms_button", true);

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
        TNT_RUN_FLOOR_SIZE.set(16);
        TNT_RUN_FLOOR_COUNT.set(2);
        PARKOUR_ENABLED.set(true);
        PARKOUR_DURATION_SECONDS.set(25);
        PARKOUR_FAIL_DAMAGE.set(8);
        DROPPER_ENABLED.set(true);
        DROPPER_FAIL_DAMAGE.set(8);
        DROPPER_WATER_SIZE.set(2);
        SUMO_ENABLED.set(true);
        SUMO_FAIL_DAMAGE.set(8);
        SUMO_ARENA_RADIUS.set(8);
        SUMO_MOB_MIN_COUNT.set(3);
        SUMO_MOB_MAX_COUNT.set(5);
        QUEUE_ENABLED.set(true);
        SIDEBAR_VISIBLE.set(true);
        ACTIVITY_FEED_VISIBLE.set(true);
        STAGGER_DELAY_TICKS.set(20);
        AUTO_CONNECT.set(true);
        ONBOARDED.set(false);
        SKIP_ACCESSIBILITY_ONBOARDING.set(true);
        REMOVE_REALMS_BUTTON.set(true);
        CONFIG_VERSION.set(CURRENT_CONFIG_VERSION);
    }
}