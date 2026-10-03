package com.isaiahcreati.creatibotintegration.integration.minigame;

import com.isaiahcreati.creatibotintegration.helpers.Chat;
import com.isaiahcreati.creatibotintegration.integration.QueueManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Minigame {

    /** Key in the player's persistent data holding their pre-minigame state. */
    private static final String RETURN_STATE_KEY = "creatibotintegration:minigame_return";

    private static final AttributeModifier COUNTDOWN_FREEZE_SPEED = new AttributeModifier(
            Identifier.fromNamespaceAndPath("creatibotintegration", "minigame_countdown_speed"),
            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final AttributeModifier COUNTDOWN_FREEZE_JUMP = new AttributeModifier(
            Identifier.fromNamespaceAndPath("creatibotintegration", "minigame_countdown_jump"),
            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    protected final Map<UUID, MinigamePlayerState> activeSessions = new ConcurrentHashMap<>();
    protected final Map<UUID, MinigamePlayerState> disconnectedSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastCountdownShown = new ConcurrentHashMap<>();
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
    public abstract boolean checkWin(ServerPlayer player);
    public abstract boolean checkLose(ServerPlayer player);
    public abstract void onTick(ServerPlayer player, long currentTick, long elapsedTicks);
    public abstract void onPlayerFall(ServerPlayer player);
    protected abstract AABB getArenaBounds();

    public float getStartYaw() { return 0f; }
    public float getStartPitch() { return 0f; }

    /**
     * Seconds of "3, 2, 1, GO!" before the game logic and timer start. This
     * also gives slower clients time to load the arena after the teleport.
     */
    public int getGracePeriodSeconds() { return 0; }

    /** Whether the player is held in place during the countdown. */
    protected boolean freezeDuringGracePeriod() { return true; }

    /** Players below this Y are treated as having fallen out of the arena. */
    protected double getVoidY() { return 49; }

    protected int getAdditionalDurationTicks(ServerPlayer player) { return 0; }

    /** Whether the arena must be (re)built before the next player enters. */
    protected boolean arenaNeedsRebuild() { return !arenaBuilt; }

    /** Return false (after telling the player why) to skip this minigame. */
    protected boolean canStart(ServerPlayer player) { return true; }

    /** Called after the player has been teleported in, before the countdown. */
    protected void onSessionStart(ServerPlayer player, ServerLevel level, MinigamePlayerState state) {}

    /** Called once when the countdown finishes and the game begins. */
    protected void onGameStart(ServerPlayer player) {}

    /** Called when a game finishes normally (win, loss, or timer). */
    protected void onExit(ServerPlayer player, boolean success) {}

    /** Called after every session ends, however it ended, to release game state. */
    protected void onSessionEnd(ServerPlayer player) {}

    /** Action-bar text shown while the game runs. */
    protected String getStatusText(ServerPlayer player, int remainingSeconds) {
        return (isTimerSurvival() ? "Survive: " : "Timer: ") + remainingSeconds + "s";
    }

    public boolean isInMinigame(ServerPlayer player) {
        return activeSessions.containsKey(player.getUUID()) || disconnectedSessions.containsKey(player.getUUID());
    }

    public boolean isInActiveMinigame(ServerPlayer player) {
        return activeSessions.containsKey(player.getUUID());
    }

    public void enterPlayer(ServerPlayer player, String redeemerName) {
        if (!isEnabled() || isInMinigame(player) || !canStart(player)) {
            return;
        }

        ServerLevel minigameLevel = MinigameDimension.getMinigameLevel(player);
        if (minigameLevel == null) {
            return;
        }

        if (arenaNeedsRebuild()) {
            buildArena(minigameLevel);
            arenaBuilt = true;
        }
        clearDroppedItems(minigameLevel);

        long currentTick = player.level().getServer().getTickCount();
        MinigamePlayerState state = new MinigamePlayerState(player, currentTick);
        activeSessions.put(player.getUUID(), state);

        // Minigames use an isolated health, hunger, and effect pool. Everything
        // is restored from the snapshot when the player leaves, so effects a
        // viewer applied just before (blindness, levitation, ...) can't ruin
        // the run and the player's own potions aren't lost.
        player.stopRiding();
        player.removeAllEffects();
        player.clearFire();
        player.setHealth(player.getMaxHealth());
        player.setAbsorptionAmount(0.0F);
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.setGameMode(GameType.ADVENTURE);

        BlockPos startPos = getStartPos();
        player.teleportTo(minigameLevel, startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5,
                Set.<Relative>of(), getStartYaw(), getStartPitch(), false);
        resetMotion(player);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 4, false, false));

        minigameLevel.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        onSessionStart(player, minigameLevel, state);
        player.getPersistentData().put(RETURN_STATE_KEY, state.toTag(player.registryAccess()));

        if (getGracePeriodSeconds() > 0) {
            if (freezeDuringGracePeriod()) {
                setFrozen(player, true);
            }
        } else {
            sendTitle(player, getTitle(), getSubtitle());
        }

        Chat.SendAlert(player, "&b" + redeemerName + "&7 sent you to " + getTitle().getString() + "!");
    }

    public void exitPlayer(ServerPlayer player, boolean success) {
        MinigamePlayerState state = activeSessions.remove(player.getUUID());
        if (state == null) return;

        restorePlayer(player, state, true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (!success) {
            float damage = getFailDamage();
            if (damage > 0) {
                player.hurt(player.level().damageSources().generic(), damage);
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

        onExit(player, success);
        endSession(player);
    }

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
                // Keep the snapshot so the player is restored when they return.
                it.remove();
                disconnectedSessions.put(uuid, state);
                continue;
            }

            if (!MinigameDimension.isMinigameDimension(player.level())) {
                // The player left by some other means (command, cross-dimension
                // ender pearl, another mod). Undo the session where they are.
                it.remove();
                abandonSession(player, state);
                continue;
            }

            long elapsedTicks = currentTick - state.getStartTick();

            if (elapsedTicks < gracePeriodTicks) {
                showCountdown(player, (int) Math.ceil((gracePeriodTicks - elapsedTicks) / 20.0));
                if (player.getY() < getVoidY()) {
                    onPlayerFall(player);
                }
                continue;
            }

            if (!state.isGameStarted()) {
                state.setGameStarted(true);
                startGame(player);
                if (!activeSessions.containsKey(uuid)) continue;
            }

            long gameElapsedTicks = elapsedTicks - gracePeriodTicks;

            // Elytra flight trivializes every course; keep players on foot.
            if (player.isFallFlying()) {
                player.stopFallFlying();
            }

            if (hasTimer()) {
                int durationTicks = baseDurationTicks + getAdditionalDurationTicks(player);
                updateTimerBar(player, durationTicks, (int) gameElapsedTicks);

                if (gameElapsedTicks >= durationTicks) {
                    exitPlayer(player, isTimerSurvival());
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

            if (player.getY() < getVoidY()) {
                onPlayerFall(player);
                if (!activeSessions.containsKey(uuid)) continue;
            }

            onTick(player, currentTick, gameElapsedTicks);
        }
    }

    public void handlePlayerDisconnect(UUID uuid) {
        MinigamePlayerState state = activeSessions.remove(uuid);
        if (state == null) return;
        disconnectedSessions.put(uuid, state);
    }

    /**
     * Ends the session without a win or loss and returns the player to where
     * they came from. Used on reconnect, respawn, and the forceexit command.
     */
    public void handlePlayerReconnect(ServerPlayer player) {
        MinigamePlayerState state = disconnectedSessions.remove(player.getUUID());
        if (state == null) {
            state = activeSessions.remove(player.getUUID());
        }
        if (state == null) return;

        restorePlayer(player, state, true);
        endSession(player);

        Chat.SendAlert(player, "&7You were returned from " + getTitle().getString() + ".");
    }

    private void abandonSession(ServerPlayer player, MinigamePlayerState state) {
        restorePlayer(player, state, false);
        endSession(player);
        Chat.SendAlert(player, "&7You left " + getTitle().getString() + " early.");
    }

    private void endSession(ServerPlayer player) {
        lastCountdownShown.remove(player.getUUID());
        onSessionEnd(player);

        ServerLevel minigameLevel = MinigameDimension.getMinigameLevel(player);
        if (minigameLevel != null) {
            clearDroppedItems(minigameLevel);
        }

        QueueManager.onMinigameEnd(player);
    }

    private void startGame(ServerPlayer player) {
        lastCountdownShown.remove(player.getUUID());
        setFrozen(player, false);
        sendTitle(player,
                Component.literal("GO!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#55FF55").getOrThrow()).withBold(true)),
                getSubtitle());
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
        onGameStart(player);
    }

    private void showCountdown(ServerPlayer player, int secondsRemaining) {
        Integer lastShown = lastCountdownShown.put(player.getUUID(), secondsRemaining);
        if (lastShown != null && lastShown == secondsRemaining) return;

        String colorHex = secondsRemaining >= 3 ? "#FF5555" : secondsRemaining == 2 ? "#FFAA00" : "#FFFF55";
        sendTitle(player,
                Component.literal(String.valueOf(secondsRemaining)).setStyle(Style.EMPTY.withColor(TextColor.parseColor(colorHex).getOrThrow()).withBold(true)),
                getSubtitle());
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void sendTitle(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 15, 5));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    protected void updateTimerBar(ServerPlayer player, int durationTicks, int elapsedTicks) {
        int remainingSeconds = Math.max(0, (durationTicks - elapsedTicks + 19) / 20);
        String colorHex = remainingSeconds <= 5 ? "#FF5555" : "#FFFF55";
        player.sendSystemMessage(Component.literal(getStatusText(player, remainingSeconds))
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor(colorHex).getOrThrow())), true);
    }

    private static void removeTimerBar(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(""), true);
    }

    private static void setFrozen(ServerPlayer player, boolean frozen) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (frozen) {
            if (speed != null) speed.addOrUpdateTransientModifier(COUNTDOWN_FREEZE_SPEED);
            if (jump != null) jump.addOrUpdateTransientModifier(COUNTDOWN_FREEZE_JUMP);
        } else {
            if (speed != null) speed.removeModifier(COUNTDOWN_FREEZE_SPEED.id());
            if (jump != null) jump.removeModifier(COUNTDOWN_FREEZE_JUMP.id());
        }
    }

    protected static void resetMotion(ServerPlayer player) {
        player.setDeltaMovement(0, 0, 0);
        player.fallDistance = 0;
    }

    /** Puts everything from the snapshot back and forgets the persisted copy. */
    private static void restorePlayer(ServerPlayer player, MinigamePlayerState state, boolean teleport) {
        removeTimerBar(player);
        setFrozen(player, false);
        player.stopRiding();

        if (teleport) {
            ServerLevel originalLevel = player.level().getServer().getLevel(state.getOriginalDimension());
            if (originalLevel != null) {
                player.teleportTo(originalLevel, state.getOriginalX(), state.getOriginalY(), state.getOriginalZ(),
                        Set.<Relative>of(), state.getOriginalYRot(), state.getOriginalXRot(), false);
            } else {
                teleportToWorldSpawn(player);
            }
        }

        player.setGameMode(state.getOriginalGameMode());

        player.removeAllEffects();
        for (MobEffectInstance effect : state.getOriginalEffects()) {
            player.addEffect(new MobEffectInstance(effect));
        }

        player.setHealth(Math.max(1.0F, Math.min(state.getOriginalHealth(), player.getMaxHealth())));
        player.setAbsorptionAmount(state.getOriginalAbsorption());
        player.getFoodData().setFoodLevel(state.getOriginalFoodLevel());
        player.getFoodData().setSaturation(state.getOriginalSaturation());
        player.clearFire();

        List<ItemStack> savedInventory = state.getSavedInventory();
        if (savedInventory != null) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                player.getInventory().setItem(i, i < savedInventory.size() ? savedInventory.get(i) : ItemStack.EMPTY);
            }
            player.inventoryMenu.broadcastChanges();
        }

        resetMotion(player);
        player.getPersistentData().remove(RETURN_STATE_KEY);
    }

    /**
     * Restores a player whose session was lost from memory, e.g. the game was
     * closed or crashed mid-minigame. Returns true if a saved session existed.
     */
    public static boolean recoverPersistedSession(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        Optional<CompoundTag> tag = data.getCompound(RETURN_STATE_KEY);
        if (tag.isEmpty()) return false;
        data.remove(RETURN_STATE_KEY);

        Optional<MinigamePlayerState> state = MinigamePlayerState.fromTag(tag.get(), player.registryAccess());
        if (state.isEmpty()) return false;

        restorePlayer(player, state.get(), MinigameDimension.isMinigameDimension(player.level()));
        Chat.SendAlert(player, "&7You were returned from an unfinished minigame.");
        return true;
    }

    /** Last-resort exit for a player stranded in the minigame dimension with no saved state. */
    public static void rescueStrandedPlayer(ServerPlayer player) {
        removeTimerBar(player);
        setFrozen(player, false);
        teleportToWorldSpawn(player);
        player.setGameMode(GameType.SURVIVAL);
        player.removeEffect(MobEffects.RESISTANCE);
        player.removeEffect(MobEffects.SLOW_FALLING);
        resetMotion(player);
    }

    private static void teleportToWorldSpawn(ServerPlayer player) {
        LevelData.RespawnData respawn = player.level().getServer().overworld().getRespawnData();
        GlobalPos spawn = respawn.globalPos();
        ServerLevel level = player.level().getServer().getLevel(spawn.dimension());
        if (level == null) level = player.level().getServer().overworld();
        BlockPos pos = spawn.pos();
        player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                Set.<Relative>of(), respawn.yaw(), respawn.pitch(), false);
    }

    private void clearDroppedItems(ServerLevel level) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, getArenaBounds())) {
            item.discard();
        }
    }
}
