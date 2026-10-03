package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.network.ClientboundQueueUpdatePacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientQueueState {

    public static List<QueueViewModel.Entry> minigameQueue = Collections.emptyList();
    public static List<QueueViewModel.Entry> visualEffectQueue = Collections.emptyList();
    public static List<QueueViewModel.Entry> pendingTaunts = Collections.emptyList();
    public static List<QueueViewModel.Entry> pendingBuffs = Collections.emptyList();

    public static String activeMinigameId = "";
    public static String activeMinigameRedeemer = "";
    public static String activeVisualEffectId = "";
    public static String activeVisualEffectRedeemer = "";
    public static String pausedVisualEffectId = "";
    public static String pausedVisualEffectRedeemer = "";
    public static int activeVisualEffectRemainingSeconds = 0;
    public static int activeVisualEffectDurationSeconds = 0;
    public static int pausedEffectRemainingSeconds = 0;
    public static int safeModeRemainingSeconds = 0;

    public static void updateFromPacket(ClientboundQueueUpdatePacket packet) {
        minigameQueue = mapEntries(packet.minigameQueue());
        visualEffectQueue = mapEntries(packet.visualEffectQueue());
        pendingTaunts = mapEntries(packet.pendingTaunts());
        pendingBuffs = mapEntries(packet.pendingBuffs());
        activeMinigameId = packet.activeMinigameId();
        activeMinigameRedeemer = packet.activeMinigameRedeemer();
        activeVisualEffectId = packet.activeVisualEffectId();
        activeVisualEffectRedeemer = packet.activeVisualEffectRedeemer();
        pausedVisualEffectId = packet.pausedVisualEffectId();
        pausedVisualEffectRedeemer = packet.pausedVisualEffectRedeemer();
        activeVisualEffectRemainingSeconds = packet.activeVisualEffectRemainingSeconds();
        activeVisualEffectDurationSeconds = packet.activeVisualEffectDurationSeconds();
        pausedEffectRemainingSeconds = packet.pausedEffectRemainingSeconds();
        safeModeRemainingSeconds = packet.safeModeRemainingSeconds();
    }

    public static void reset() {
        minigameQueue = Collections.emptyList();
        visualEffectQueue = Collections.emptyList();
        pendingTaunts = Collections.emptyList();
        pendingBuffs = Collections.emptyList();
        activeMinigameId = "";
        activeMinigameRedeemer = "";
        activeVisualEffectId = "";
        activeVisualEffectRedeemer = "";
        pausedVisualEffectId = "";
        pausedVisualEffectRedeemer = "";
        activeVisualEffectRemainingSeconds = 0;
        activeVisualEffectDurationSeconds = 0;
        pausedEffectRemainingSeconds = 0;
        safeModeRemainingSeconds = 0;
    }

    public static boolean hasAnythingQueued() {
        return !minigameQueue.isEmpty()
                || !visualEffectQueue.isEmpty()
                || !pendingTaunts.isEmpty()
                || !pendingBuffs.isEmpty()
                || !activeMinigameId.isEmpty()
                || !activeVisualEffectId.isEmpty()
                || !pausedVisualEffectId.isEmpty()
                || pausedEffectRemainingSeconds > 0;
    }

    public static int getTotalQueueSize() {
        return QueueViewModel.totalSize(minigameQueue, visualEffectQueue, pendingTaunts, pendingBuffs);
    }

    public static List<QueueViewModel.DisplayEntry> getVisibleQueueEntries(int limit) {
        return QueueViewModel.visibleEntries(
                limit, minigameQueue, visualEffectQueue, pendingTaunts, pendingBuffs);
    }

    private static List<QueueViewModel.Entry> mapEntries(
            List<ClientboundQueueUpdatePacket.QueueEntry> packetEntries) {
        List<QueueViewModel.Entry> entries = new ArrayList<>(packetEntries.size());
        for (ClientboundQueueUpdatePacket.QueueEntry entry : packetEntries) {
            entries.add(new QueueViewModel.Entry(
                    entry.tauntId(), entry.displayName(), entry.redeemerName(), entry.durationSeconds()));
        }
        return entries;
    }

    public static String getDisplayName(String tauntId) {
        return switch (tauntId) {
            case "parkour" -> "Parkour";
            case "tntrun" -> "TNT Run";
            case "dropper" -> "Dropper";
            case "sumo" -> "Arena";
            case "blur" -> "Blur";
            case "inverted_colors" -> "Inverted Colors";
            case "black_and_white" -> "1950s";
            case "lsd" -> "Lucy In The Sky";
            case "crt" -> "Monitor Downgrade";
            case "drunk" -> "Drunk Streamer";
            case "vignette_heartbeat" -> "Heartbeat";
            case "pixelate" -> "PS1 Aesthetic";
            case "mirror" -> "Mirror World";
            case "fisheye" -> "Fisheye";
            case "pumpkin_view" -> "Pumpkin View";
            case "dvd" -> "DVD Screensaver";
            case "fov_quake" -> "Quake FOV";
            case "fov_zoom" -> "Ultra Zoom";
            case "upside_down" -> "Upside Down";
            case "rolling_camera" -> "Rolling Camera";
            case "camera_tilt" -> "Tilted Camera";
            case "inverted_controls" -> "Inverted Controls";
            case "mouse_drifting" -> "Mouse Drifting";
            case "tnt" -> "TNT";
            case "shuffle" -> "Shuffle";
            case "punch" -> "Smack";
            case "noise" -> "Noise";
            case "strike" -> "Lightning";
            case "break" -> "Break Block";
            case "wild" -> "Wild TP";
            case "drop" -> "Drop Item";
            case "cobweb" -> "Cobweb";
            case "drop_all" -> "Drop All";
            case "half_heart" -> "Half Heart";
            case "hungry" -> "Starving";
            case "sky" -> "To The Moon";
            case "fake_tp" -> "Fake TP";
            case "jumpscare" -> "Jumpscare";
            case "chicken_rain" -> "Chicken Rain";
            case "meteor_rain" -> "Meteor Rain";
            case "raid" -> "Raid";
            case "fire_trail" -> "Fire Trail";
            case "downgrade_gear" -> "Downgrade";
            case "anvil" -> "Anvil Drop";
            case "bury" -> "Buried Alive";
            case "curse_gear" -> "Cursed";
            case "stack_one" -> "Stack of One";
            case "gremlin" -> "Gremlin";
            case "big_mob" -> "Big Mob";
            case "tiny_mob" -> "Tiny Mob";
            case "anvil_rain" -> "Anvil Rain";
            case "blind_noise" -> "Blind Panic";
            case "rename_chat" -> "Rename the Streamer";
            case "hot_potato" -> "Hot Potato";
            case "lucky_block" -> "Lucky Block";
            case "glass_cannon" -> "Glass Cannon";
            case "frostbite" -> "Frostbite";
            case "double_health" -> "Double Health";
            case "explosive_spawns" -> "Explosive Spawns";
            case "fire_mobs" -> "Fire Mobs";
            case "invisible_mobs" -> "Invisible Mobs";
            default -> tauntId;
        };
    }
}
