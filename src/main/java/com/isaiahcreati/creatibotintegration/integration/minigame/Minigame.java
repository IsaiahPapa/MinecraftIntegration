package com.isaiahcreati.creatibotintegration.integration.minigame;

import com.isaiahcreati.creatibotintegration.helpers.Chat;
import com.isaiahcreati.creatibotintegration.integration.QueueManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Relative;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Minigame {

    protected final Map<UUID, MinigamePlayerState> activeSessions = new ConcurrentHashMap<>();
    protected final Map<UUID, MinigamePlayerState> disconnectedSessions = new ConcurrentHashMap<>();
    protected boolean arenaBuilt = false;

    public abstract String getId();
    public abstract Component getTitle();
    public abstract Component getSubtitle();
    public abstract boolean isEnabled();
    public abstract boolean hasTimer();
    public abstract int getDurationSeconds();
    public abstract boolean isTimerSurvival();
    public abstract BlockPos getStartPos();
    public abstract float getFailDamage();
    public abstract void buildArena(ServerLevel level);
    public abstract void resetArena(ServerLevel level);
    public abstract boolean checkWin(ServerPlayer player);
    public abstract boolean checkLose(ServerPlayer player);
    public abstract void onTick(ServerPlayer player, long currentTick, long elapsedTicks);
    public abstract void onPlayerFall(ServerPlayer player);
    protected abstract AABB getArenaBounds();

    public float getStartYaw() { return 0f; }
    public boolean hasGracePeriod() { return false; }
    public int getGracePeriodSeconds() { return 0; }
    protected int getAdditionalDurationTicks(ServerPlayer player) { return 0; }
    protected void onGracePeriodCountdown(ServerPlayer player, int secondsRemaining) {}

    public boolean isInMinigame(ServerPlayer player) {
        return activeSessions.containsKey(player.getUUID()) || disconnectedSessions.containsKey(player.getUUID());
    }

    public boolean isInActiveMinigame(ServerPlayer player) {
        return activeSessions.containsKey(player.getUUID());
    }

    public void enterPlayer(ServerPlayer player, String redeemerName) {
        if (!isEnabled()) {
            return;
        }

        if (isInMinigame(player)) {
            return;
        }

        ServerLevel minigameLevel = MinigameDimension.getMinigameLevel(player);
        if (minigameLevel == null) {
            return;
        }

        clearDroppedItems(minigameLevel);

        if (!arenaBuilt) {
            buildArena(minigameLevel);
            arenaBuilt = true;
            clearDroppedItems(minigameLevel);
        }

        long currentTick = player.level().getServer().getTickCount();
        MinigamePlayerState state = new MinigamePlayerState(player, currentTick);
        activeSessions.put(player.getUUID(), state);

        // Minigames use an isolated health pool. The overworld snapshot is
        // restored when the player leaves.
        player.setHealth(player.getMaxHealth());
        player.setAbsorptionAmount(0.0F);
        player.stopRiding();
        player.setDeltaMovement(0, 0, 0);
        player.fallDistance = 0;

        FoodData foodData = player.getFoodData();
        if (foodData.getFoodLevel() < 7) {
            foodData.setFoodLevel(7);
        }

        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 4));

        player.setGameMode(GameType.ADVENTURE);

        BlockPos startPos = getStartPos();
        player.teleportTo(minigameLevel, startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5, Set.<Relative>of(), getStartYaw(), player.getXRot(), false);
        clearDroppedItems(minigameLevel);

        minigameLevel.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (hasTimer()) {
            showTimerBar(player);
        }

        Chat.SendAlert(player, "&b" + redeemerName + "&7 sent you to " + getTitle().getString() + "!");
    }

    public void exitPlayer(ServerPlayer player, boolean success) {
        MinigamePlayerState state = activeSessions.remove(player.getUUID());
        if (state == null) return;

        removeTimerBar(player);

        ServerLevel originalLevel = player.level().getServer().getLevel(state.getOriginalDimension());
        if (originalLevel == null) {
            originalLevel = player.level().getServer().overworld();
        }

        player.teleportTo(originalLevel, state.getOriginalX(), state.getOriginalY(), state.getOriginalZ(), Set.<Relative>of(), state.getOriginalYRot(), state.getOriginalXRot(), false);
        originalLevel.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.setGameMode(state.getOriginalGameMode());

        player.removeEffect(MobEffects.RESISTANCE);
        player.removeEffect(MobEffects.SLOW_FALLING);
        restoreHealth(player, state);

        if (!success) {
            float damage = getFailDamage();
            if (damage > 0) {
                player.hurt(player.level().damageSources().generic(), damage);
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

        onExit(player, success);

        ServerLevel minigameLevel = MinigameDimension.getMinigameLevel(player);
        if (minigameLevel != null) {
            clearDroppedItems(minigameLevel);
        }

        if (!arenaBuilt) {
            if (minigameLevel != null) {
                resetArena(minigameLevel);
                arenaBuilt = false;
            }
        }

        QueueManager.onMinigameEnd(player);
    }

    protected void onExit(ServerPlayer player, boolean success) {}

    public void onServerTick(ServerTickEvent.Post event) {
        long currentTick = event.getServer().getTickCount();
        int baseDurationTicks = getDurationSeconds() * 20;
        int gracePeriodTicks = getGracePeriodSeconds() * 20;

        Iterator<Map.Entry<UUID, MinigamePlayerState>> it = activeSessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, MinigamePlayerState> entry = it.next();
            UUID uuid = entry.getKey();
            MinigamePlayerState state = entry.getValue();

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player == null) {
                it.remove();
                continue;
            }

            if (!MinigameDimension.isMinigameDimension(player.level())) {
                it.remove();
                continue;
            }

            long elapsedTicks = currentTick - state.getStartTick();

            if (hasGracePeriod() && elapsedTicks < gracePeriodTicks) {
                int graceRemaining = (int) Math.ceil((gracePeriodTicks - elapsedTicks) / 20.0);
                onGracePeriodCountdown(player, graceRemaining);
                continue;
            }

            long gameElapsedTicks = hasGracePeriod() ? elapsedTicks - gracePeriodTicks : elapsedTicks;

            if (hasTimer()) {
                int durationTicks = baseDurationTicks + getAdditionalDurationTicks(player);
                updateTimerBar(player, durationTicks, (int) gameElapsedTicks);

                if (!isTimerSurvival() && gameElapsedTicks >= durationTicks) {
                    exitPlayer(player, false);
                    continue;
                }

                if (isTimerSurvival() && gameElapsedTicks >= durationTicks) {
                    exitPlayer(player, true);
                    continue;
                }
            }

            if (checkWin(player)) {
                exitPlayer(player, true);
                continue;
            }

            if (checkLose(player)) {
                exitPlayer(player, false);
                continue;
            }

            if (player.getY() < 49) {
                onPlayerFall(player);
            }

            onTick(player, currentTick, gameElapsedTicks);
        }
    }

    public void handlePlayerDeath(ServerPlayer player) {
        if (!isInActiveMinigame(player)) return;
        if (!MinigameDimension.isMinigameDimension(player.level())) return;

        MinigamePlayerState state = activeSessions.remove(player.getUUID());
        if (state == null) return;

        removeTimerBar(player);

        ServerLevel minigameLevel = MinigameDimension.getMinigameLevel(player);
        if (minigameLevel != null) {
            for (ItemEntity itemEntity : minigameLevel.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(10))) {
                ServerLevel originalLevel = player.level().getServer().getLevel(state.getOriginalDimension());
                if (originalLevel != null) {
                    ItemEntity transferredItem = new ItemEntity(
                            originalLevel,
                            state.getOriginalX(),
                            state.getOriginalY(),
                            state.getOriginalZ(),
                            itemEntity.getItem()
                    );
                    originalLevel.addFreshEntity(transferredItem);
                }
                itemEntity.discard();
            }
        }

        player.setGameMode(state.getOriginalGameMode());
        restoreHealth(player, state);

        if (minigameLevel != null) {
            clearDroppedItems(minigameLevel);
        }
    }

    public void handlePlayerDisconnect(UUID uuid) {
        MinigamePlayerState state = activeSessions.get(uuid);
        if (state == null) return;

        disconnectedSessions.put(uuid, state);
        activeSessions.remove(uuid);
    }

    public void handlePlayerReconnect(ServerPlayer player) {
        MinigamePlayerState state = disconnectedSessions.remove(player.getUUID());
        if (state == null) {
            state = activeSessions.remove(player.getUUID());
        }
        if (state == null) return;

        removeTimerBar(player);

        ServerLevel originalLevel = player.level().getServer().getLevel(state.getOriginalDimension());
        if (originalLevel == null) originalLevel = player.level().getServer().overworld();

        player.teleportTo(originalLevel, state.getOriginalX(), state.getOriginalY(), state.getOriginalZ(), Set.<Relative>of(), state.getOriginalYRot(), state.getOriginalXRot(), false);
        player.setGameMode(state.getOriginalGameMode());
        player.removeEffect(MobEffects.RESISTANCE);
        player.removeEffect(MobEffects.SLOW_FALLING);
        restoreHealth(player, state);

        ServerLevel minigameLevel = MinigameDimension.getMinigameLevel(player);
        if (minigameLevel != null) {
            clearDroppedItems(minigameLevel);
        }

        // Clean up the queue so future taunts aren't blocked.
        QueueManager.onMinigameEnd(player);

        Chat.SendAlert(player, "&7You were returned from " + getTitle().getString() + " after reconnecting.");
    }

    protected void showTimerBar(ServerPlayer player) {
        String timerText = isTimerSurvival() ? "Survive: " : "Timer: ";
        player.sendSystemMessage(Component.literal(timerText + getDurationSeconds() + "s").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555").getOrThrow())), true);
    }

    protected void updateTimerBar(ServerPlayer player, int durationTicks, int elapsedTicks) {
        int remainingSeconds = Math.max(0, (durationTicks - elapsedTicks) / 20);
        String colorHex = remainingSeconds <= 5 ? "#FF5555" : "#FFFF55";
        String timerText = isTimerSurvival() ? "Survive: " : "Timer: ";
        player.sendSystemMessage(Component.literal(timerText + remainingSeconds + "s").setStyle(Style.EMPTY.withColor(TextColor.parseColor(colorHex).getOrThrow())), true);
    }

    protected void removeTimerBar(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(""), true);
    }

    private void restoreHealth(ServerPlayer player, MinigamePlayerState state) {
        player.setHealth(Math.min(state.getOriginalHealth(), player.getMaxHealth()));
        player.setAbsorptionAmount(state.getOriginalAbsorption());
    }

    private void clearDroppedItems(ServerLevel level) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, getArenaBounds())) {
            item.discard();
        }
    }

    protected void markArenaNeedsRebuild() {
        arenaBuilt = false;
    }
}
