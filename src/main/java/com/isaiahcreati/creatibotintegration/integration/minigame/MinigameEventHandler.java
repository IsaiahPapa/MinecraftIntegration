package com.isaiahcreati.creatibotintegration.integration.minigame;

import com.isaiahcreati.creatibotintegration.helpers.Buffs;
import com.isaiahcreati.creatibotintegration.integration.QueueManager;
import com.isaiahcreati.creatibotintegration.integration.Taunts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

public class MinigameEventHandler {

    private static final List<Minigame> minigames = new ArrayList<>();

    public static void registerMinigame(Minigame game) {
        minigames.add(game);
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
        Buffs.tick(event);
        QueueManager.tick(event);
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
            if (Buffs.isFrostbiteMob(sourceEntity)) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
            }
        }
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (getActiveMinigameForPlayer(player) == null) return;

        // Arena item entities are wiped between sessions, so a dropped item
        // would be lost for good. Put it straight back into the inventory.
        event.setCanceled(true);
        player.getInventory().placeItemBackInInventory(event.getEntity().getItem().copy());
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (getActiveMinigameForPlayer(player) == null) return;

        // Ender pearls, wind charges, rockets, tridents, and potions all let a
        // player skip or escape the arena, so held items stay inert here.
        event.setCanceled(true);
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

        // Always send a complete snapshot, including the empty state, so a
        // client can never carry sidebar data over from another world.
        QueueManager.sendQueueUpdate(player);

        boolean wasInMinigame = false;
        for (Minigame game : minigames) {
            if (game.isInMinigame(player)) {
                game.handlePlayerReconnect(player);
                wasInMinigame = true;
            }
        }

        // Sessions only live in memory, so after a restart or crash fall back
        // to the snapshot saved on the player. If even that is missing (e.g. a
        // save from an older mod version), send them to the world spawn.
        if (!wasInMinigame && !Minigame.recoverPersistedSession(player)
                && MinigameDimension.isMinigameDimension(player.level())) {
            Minigame.rescueStrandedPlayer(player);
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
