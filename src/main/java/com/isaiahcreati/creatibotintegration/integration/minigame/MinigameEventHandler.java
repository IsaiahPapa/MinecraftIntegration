package com.isaiahcreati.creatibotintegration.integration.minigame;

import com.isaiahcreati.creatibotintegration.integration.Taunts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MinigameEventHandler {

    private static final List<Minigame> minigames = new ArrayList<>();

    public static void registerMinigame(Minigame game) {
        minigames.add(game);
    }

    private Minigame getMinigameForPlayer(ServerPlayer player) {
        for (Minigame game : minigames) {
            if (game.isInMinigame(player)) return game;
        }
        return null;
    }

    private Minigame getActiveMinigameForPlayer(ServerPlayer player) {
        for (Minigame game : minigames) {
            if (game.isInActiveMinigame(player)) return game;
        }
        return null;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        for (Minigame game : minigames) {
            game.onServerTick(event);
        }
        Taunts.tickFireTrails();
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!MinigameDimension.isMinigameDimension(player.level())) return;

        Minigame game = getActiveMinigameForPlayer(player);
        if (game == null) return;

        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        game.onPlayerFall(player);
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!MinigameDimension.isMinigameDimension(player.level())) return;

        Minigame game = getActiveMinigameForPlayer(player);
        if (game == null) return;

        if (event.getSource().is(DamageTypes.FALL) ||
            event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            event.setNewDamage(0);
        }
    }

    @SubscribeEvent
    public void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        for (Minigame game : minigames) {
            if (game.isInMinigame(player)) {
                game.handlePlayerDisconnect(player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerConnect(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        for (Minigame game : minigames) {
            if (game.isInMinigame(player)) {
                game.handlePlayerReconnect(player);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Taunts.updateFireTrail(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!MinigameDimension.isMinigameDimension(player.level())) return;

        for (Minigame game : minigames) {
            if (game.isInMinigame(player)) {
                game.handlePlayerReconnect(player);
            }
        }
    }
}