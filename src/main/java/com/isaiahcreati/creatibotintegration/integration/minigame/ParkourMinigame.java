package com.isaiahcreati.creatibotintegration.integration.minigame;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.helpers.Chat;
import com.isaiahcreati.creatibotintegration.integration.ParkourCourse;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ParkourMinigame extends Minigame {

    private static final int COUNTDOWN_SECONDS = 3;
    private static final int CHECKPOINT_BONUS_SECONDS = 7;

    // Every platform on every version is at Y=64 or above, so dropping below
    // this means the jump was missed. Catching it here returns the player a
    // second earlier than waiting for the shared void check at Y=49.
    private static final double MISSED_JUMP_Y = 60;

    private final ParkourCourse course = new ParkourCourse();
    private final Map<UUID, BlockPos> checkpointRespawns = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> checkpointBonusTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> fallCounts = new ConcurrentHashMap<>();

    @Override
    public String getId() { return "parkour"; }

    @Override
    public Component getTitle() {
        return Component.literal("Parkour").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFAA00").getOrThrow()).withBold(true));
    }

    @Override
    public Component getSubtitle() {
        return Component.literal("Reach the finish in " + getDurationSeconds() + " seconds!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF").getOrThrow()));
    }

    @Override
    public boolean isEnabled() { return Config.PARKOUR_ENABLED.get(); }

    @Override
    public boolean hasTimer() { return true; }

    @Override
    public int getDurationSeconds() { return Config.PARKOUR_DURATION_SECONDS.get(); }

    @Override
    public int getGracePeriodSeconds() { return COUNTDOWN_SECONDS; }

    @Override
    protected int getAdditionalDurationTicks(ServerPlayer player) {
        return checkpointBonusTicks.getOrDefault(player.getUUID(), 0);
    }

    @Override
    public boolean isTimerSurvival() { return false; }

    @Override
    public BlockPos getStartPos() { return course.getStartPosition(); }

    @Override
    public float getFailDamage() { return Config.PARKOUR_FAIL_DAMAGE.get().floatValue(); }

    @Override
    protected boolean arenaNeedsRebuild() {
        return super.arenaNeedsRebuild() || course.needsRebuild();
    }

    @Override
    public void buildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Building parkour course...");
        course.buildIfNeeded(level);
    }

    @Override
    protected void onSessionStart(ServerPlayer player, ServerLevel level, MinigamePlayerState state) {
        checkpointRespawns.remove(player.getUUID());
        checkpointBonusTicks.remove(player.getUUID());
        fallCounts.remove(player.getUUID());
    }

    @Override
    protected String getStatusText(ServerPlayer player, int remainingSeconds) {
        String status = super.getStatusText(player, remainingSeconds);
        int falls = fallCounts.getOrDefault(player.getUUID(), 0);
        return falls > 0 ? status + "  |  Falls: " + falls : status;
    }

    @Override
    public boolean checkWin(ServerPlayer player) {
        return course.isOnFinishPad(player.blockPosition());
    }

    @Override
    public boolean checkLose(ServerPlayer player) { return false; }

    @Override
    public void onTick(ServerPlayer player, long currentTick, long elapsedTicks) {
        if (course.hasCheckpoint() && course.isInCheckpointArea(player.blockPosition())
                && !checkpointRespawns.containsKey(player.getUUID())) {
            checkpointRespawns.put(player.getUUID(), course.getCheckpointRespawnPosition());
            checkpointBonusTicks.put(player.getUUID(), CHECKPOINT_BONUS_SECONDS * 20);
            // The action bar is owned by the timer, so announce this as a subtitle.
            sendTitle(player, Component.empty(), Component.literal(
                            "Checkpoint! +" + CHECKPOINT_BONUS_SECONDS + " seconds")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55").getOrThrow()).withBold(true)));
            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        }

        // Missing a jump (into the water basin, onto the basin walls, or
        // straight down) sends the player back to their latest checkpoint.
        if (player.getY() < MISSED_JUMP_Y
                || player.level().getBlockState(player.blockPosition()).is(Blocks.WATER)) {
            resetToCheckpoint(player);
        }
    }

    @Override
    public void onPlayerFall(ServerPlayer player) {
        resetToCheckpoint(player);
    }

    @Override
    protected AABB getArenaBounds() {
        return course.getBounds();
    }

    private void resetToCheckpoint(ServerPlayer player) {
        BlockPos respawnPos = checkpointRespawns.getOrDefault(player.getUUID(), course.getStartPosition());
        // Face back down the course so the player can retry immediately.
        player.teleportTo((ServerLevel) player.level(), respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5,
                Set.<Relative>of(), 0f, player.getXRot(), false);
        resetMotion(player);
        fallCounts.merge(player.getUUID(), 1, Integer::sum);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    @Override
    protected void onExit(ServerPlayer player, boolean success) {
        if (success) {
            Chat.SendAlert(player, "&aYou escaped the Parkour Course!");
        } else {
            Chat.SendAlert(player, "&7You failed to complete the Parkour Course in time!");
        }
    }

    @Override
    protected void onSessionEnd(ServerPlayer player) {
        checkpointRespawns.remove(player.getUUID());
        checkpointBonusTicks.remove(player.getUUID());
        fallCounts.remove(player.getUUID());
    }
}
