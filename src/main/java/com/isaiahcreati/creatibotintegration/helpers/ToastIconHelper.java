package com.isaiahcreati.creatibotintegration.helpers;

import java.util.Map;

public class ToastIconHelper {

    private static final Map<String, String> TAUNT_ICONS = Map.ofEntries(
            Map.entry("tnt", "item:minecraft:tnt"),
            Map.entry("shuffle", "item:minecraft:compass"),
            Map.entry("punch", "item:minecraft:stick"),
            Map.entry("noise", "item:minecraft:note_block"),
            Map.entry("strike", "item:minecraft:lightning_rod"),
            Map.entry("break", "item:minecraft:wooden_pickaxe"),
            Map.entry("wild", "item:minecraft:ender_pearl"),
            Map.entry("drop", "item:minecraft:feather"),
            Map.entry("cobweb", "item:minecraft:cobweb"),
            Map.entry("drop_all", "item:minecraft:barrel"),
            Map.entry("half_heart", "item:minecraft:poisonous_potato"),
            Map.entry("hungry", "item:minecraft:rotten_flesh"),
            Map.entry("sky", "item:minecraft:firework_rocket"),
            Map.entry("fake_tp", "item:minecraft:ender_pearl"),
            Map.entry("jumpscare", "item:minecraft:warden_spawn_egg"),
            Map.entry("chicken_rain", "item:minecraft:egg"),
            Map.entry("meteor_rain", "item:minecraft:magma_block"),
            Map.entry("raid", "item:minecraft:ominous_bottle"),
            Map.entry("fire_trail", "item:minecraft:blaze_rod"),
            Map.entry("downgrade_gear", "item:minecraft:grindstone"),
            Map.entry("anvil", "item:minecraft:anvil"),
            Map.entry("bury", "item:minecraft:dirt"),
            Map.entry("curse_gear", "item:minecraft:enchanted_golden_apple"),
            Map.entry("stack_one", "item:minecraft:paper"),
            Map.entry("gremlin", "item:minecraft:zombie_spawn_egg"),
            Map.entry("big_mob", "item:minecraft:beef"),
            Map.entry("tiny_mob", "item:minecraft:slime_ball"),
            Map.entry("anvil_rain", "item:minecraft:anvil"),
            Map.entry("blind_noise", "item:minecraft:spider_eye"),
            Map.entry("rename_chat", "item:minecraft:name_tag"),
            Map.entry("hot_potato", "item:minecraft:tnt"),
            Map.entry("lucky_block", "item:minecraft:sponge"),
            Map.entry("fov_quake", "item:minecraft:diamond_sword"),
            Map.entry("fov_zoom", "item:minecraft:spyglass"),
            Map.entry("upside_down", "item:minecraft:rotten_flesh"),
            Map.entry("rolling_camera", "item:minecraft:camera"),
            Map.entry("camera_tilt", "item:minecraft:leather"),
            Map.entry("pumpkin_view", "item:minecraft:carved_pumpkin"),
            Map.entry("dvd", "item:minecraft:red_dye"),
            Map.entry("inverted_controls", "item:minecraft:redstone"),
            Map.entry("mouse_drifting", "item:minecraft:string"),
            Map.entry("blur", "item:minecraft:spider_eye"),
            Map.entry("inverted_colors", "item:minecraft:magenta_dye"),
            Map.entry("black_and_white", "item:minecraft:gray_dye"),
            Map.entry("lsd", "item:minecraft:pink_dye"),
            Map.entry("crt", "item:minecraft:iron_bars"),
            Map.entry("drunk", "item:minecraft:potion"),
            Map.entry("vignette_heartbeat", "item:minecraft:redstone"),
            Map.entry("pixelate", "item:minecraft:crafting_table"),
            Map.entry("mirror", "item:minecraft:glass_pane"),
            Map.entry("fisheye", "item:minecraft:glass_bottle"),
            Map.entry("parkour", "item:minecraft:ladder"),
            Map.entry("tntrun", "item:minecraft:tnt"),
            Map.entry("dropper", "item:minecraft:water_bucket"),
            Map.entry("sumo", "item:minecraft:iron_sword")
    );

    public static String getIconForTaunt(String tauntId) {
        return TAUNT_ICONS.getOrDefault(tauntId, "");
    }

    public static String getIconForAction(String action, String detail) {
        return switch (action) {
            case "GIVE" -> "item:" + (detail.isEmpty() ? "minecraft:chest" : detail);
            case "SPAWN" -> "entity:" + (detail.isEmpty() ? "minecraft:zombie" : detail);
            case "EFFECT" -> "item:minecraft:potion";
            default -> "";
        };
    }

    public static String iconType(String combined) {
        if (combined == null || combined.isEmpty()) return "";
        int idx = combined.indexOf(':');
        if (idx < 0) return combined;
        return combined.substring(0, idx);
    }

    public static String iconId(String combined) {
        if (combined == null || combined.isEmpty()) return "";
        int idx = combined.indexOf(':');
        if (idx < 0) return "";
        return combined.substring(idx + 1);
    }
}