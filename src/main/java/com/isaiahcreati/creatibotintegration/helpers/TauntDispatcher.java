package com.isaiahcreati.creatibotintegration.helpers;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.integration.Taunts;
import com.isaiahcreati.creatibotintegration.network.ClientboundTauntEffectPacket;
import com.isaiahcreati.creatibotintegration.network.PacketHandler;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class TauntDispatcher {

    private static final Taunts taunts = new Taunts();

    public static Taunts getTaunts() {
        return taunts;
    }

    public static boolean dispatchTaunt(ServerPlayer player, String tauntId) {
        return dispatchTaunt(player, tauntId, 15);
    }

    public static boolean dispatchTaunt(ServerPlayer player, String tauntId, int clientEffectDuration) {
        ServerLevel overworld = player.server.overworld();

        switch (tauntId) {
            case "tnt" -> Taunts.spawnPrimedTntOnPlayer(overworld, player);
            case "shuffle" -> Taunts.ShuffleInventory(player);
            case "punch" -> Taunts.smackPlayer(player);
            case "noise" -> Utils.playSoundByName(player, "minecraft:entity.creeper.primed");
            case "strike" -> Taunts.strikeDownPlayer(player);
            case "break" -> Taunts.breakBlockUnderPlayer(player);
            case "wild" -> Taunts.teleportPlayerToRandomLocation(player);
            case "drop" -> Taunts.dropHand(player);
            case "cobweb" -> Taunts.webBlockPlayer(player);
            case "parkour" -> CreatiIntegration.getParkourMinigame().enterPlayer(player, "Dev");
            case "tntrun" -> CreatiIntegration.getTntRunMinigame().enterPlayer(player, "Dev");
            case "drop_all" -> Taunts.dropAllInventory(player);
            case "half_heart" -> Taunts.setHalfHeart(player);
            case "hungry" -> Taunts.drainHunger(player);
            case "sky" -> Taunts.launchToSky(player);
            case "fake_tp" -> Taunts.fakeTeleport(player);
            case "jumpscare" -> player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT, 1.0f));
            case "chicken_rain" -> Taunts.chickenRain(player);
            case "meteor_rain" -> Taunts.meteorRain(player);
            case "raid"           -> Taunts.triggerRaid(player);
            case "fire_trail"     -> Taunts.startFireTrail(player);
            case "downgrade_gear" -> Taunts.downgradeGear(player);
            case "anvil"          -> Taunts.dropAnvilOnPlayer(player);
            case "bury"           -> Taunts.buryPlayer(player);
            case "curse_gear"     -> Taunts.curseGear(player);
            case "stack_one"      -> Taunts.reduceStacksToOne(player);
            case "fov_quake", "fov_zoom", "upside_down", "rolling_camera", "camera_tilt",
                 "pumpkin_view", "dvd",
                 "inverted_controls", "mouse_drifting",
                 "blur", "inverted_colors", "black_and_white", "lsd", "crt" ->
                PacketHandler.sendToPlayer(player, new ClientboundTauntEffectPacket(tauntId, clientEffectDuration));
            default -> {
                return false;
            }
        }
        return true;
    }
}