package com.isaiahcreati.creatibotintegration.integration.minigame;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.integration.QueueManager;
import com.isaiahcreati.creatibotintegration.integration.Taunts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
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
        Taunts.tickRenames();
        Taunts.tickHotPotatoes();
        Taunts.tickLuckyBlocks();
        Taunts.tickGremlins();
        if (Config.QUEUE_ENABLED.get()) {
            QueueManager.tick(event);
        }
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

        net.minecraft.world.entity.Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity == null) sourceEntity = event.getSource().getDirectEntity();

        if (MinigameDimension.isMinigameDimension(player.level())) {
            Minigame game = getActiveMinigameForPlayer(player);
            if (game != null) {
                if (event.getSource().is(DamageTypes.FALL) ||
                    event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
                    event.setNewDamage(0);
                }
            }
        }

        if (sourceEntity != null) {
            Taunts.onPlayerHurtByGremlin(player, sourceEntity);
        }
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
            Taunts.onGremlinDeath(living);
        }
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!MinigameDimension.isMinigameDimension(event.getLevel())) return;
        // Prevent explosions (e.g. creepers) from destroying arena blocks.
        // Entity damage is preserved; only block removal is blocked.
        event.getAffectedBlocks().clear();
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
        boolean wasInMinigame = false;
        for (Minigame game : minigames) {
            if (game.isInMinigame(player)) {
                game.handlePlayerReconnect(player);
                wasInMinigame = true;
            }
        }

        // Safety net: if the player is in the minigame dimension but not tracked
        // by any minigame (e.g. after a mod version change wiped the session
        // state), teleport them to the overworld and restore survival mode.
        if (!wasInMinigame && MinigameDimension.isMinigameDimension(player.level())) {
            net.minecraft.server.level.ServerLevel overworld = player.level().getServer().overworld();
            player.teleportTo(overworld, 0.5, 100, 0.5, java.util.Set.of(), 0f, 0f, false);
            player.setGameMode(GameType.SURVIVAL);
            player.removeEffect(net.minecraft.world.effect.MobEffects.RESISTANCE);
            player.removeEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);
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