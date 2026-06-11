package com.isaiahcreati.creatibotintegration.integration.minigame;

import com.isaiahcreati.creatibotintegration.integration.Taunts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!MinigameDimension.isMinigameDimension(player.level())) return;

        Minigame game = getActiveMinigameForPlayer(player);
        if (game == null) return;

        if (event.getSource().is(DamageTypes.FALL) ||
            event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            event.setCanceled(true);
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
    public void onPlayerTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
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