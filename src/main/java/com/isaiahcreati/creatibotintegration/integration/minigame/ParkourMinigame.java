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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ParkourMinigame extends Minigame {

    private static final int V3_DURATION_SECONDS = 30;
    private static final int CHECKPOINT_BONUS_SECONDS = 7;
    private final ParkourCourse course = new ParkourCourse();
    private final Map<UUID, BlockPos> checkpointRespawns = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> checkpointBonusTicks = new ConcurrentHashMap<>();
    private final Set<UUID> announcedCheckpoints = ConcurrentHashMap.newKeySet();

    @Override
    public void enterPlayer(ServerPlayer player, String redeemerName) {
        if (isInMinigame(player)) return;
        checkpointRespawns.remove(player.getUUID());
        checkpointBonusTicks.remove(player.getUUID());
        announcedCheckpoints.remove(player.getUUID());
        super.enterPlayer(player, redeemerName);
        if (isInActiveMinigame(player)) {
            course.reconcileFloatingText((ServerLevel) player.level());
        }
    }

    @Override
    public String getId() { return "parkour"; }

    @Override
    public Component getTitle() {
        return Component.literal("Parkour").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFAA00").getOrThrow()).withBold(true));
    }

    @Override
    public Component getSubtitle() {
        return Component.literal("Complete the course in " + getDurationSeconds() + " seconds!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF").getOrThrow()));
    }

    @Override
    public boolean isEnabled() { return Config.PARKOUR_ENABLED.get(); }

    @Override
    public boolean hasTimer() { return true; }

    @Override
    public int getDurationSeconds() {
        return course.getArenaVersion() == 3
                ? V3_DURATION_SECONDS
                : Config.PARKOUR_DURATION_SECONDS.get();
    }

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
    public boolean checkWin(ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();
        BlockPos endPos = course.getEndPosition();

        if (player.level().getBlockState(playerPos).is(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)
                && Math.abs(playerPos.getX() - endPos.getX()) <= 2
                && Math.abs(playerPos.getZ() - endPos.getZ()) <= 2
                && Math.abs(playerPos.getY() - endPos.getY()) <= 2) {
            return true;
        }

        return false;
    }

    @Override
    public boolean checkLose(ServerPlayer player) { return false; }

    @Override
    public void onTick(ServerPlayer player, long currentTick, long elapsedTicks) {
        if (currentTick % 10 == 0) {
            course.reconcileFloatingText((ServerLevel) player.level());
        }

        if (course.hasCheckpoint() && course.isInCheckpointArea(player.blockPosition())) {
            checkpointRespawns.put(player.getUUID(), course.getCheckpointRespawnPosition());
            if (announcedCheckpoints.add(player.getUUID())) {
                checkpointBonusTicks.put(player.getUUID(), CHECKPOINT_BONUS_SECONDS * 20);
                player.sendSystemMessage(Component.literal(
                                "Checkpoint reached! +" + CHECKPOINT_BONUS_SECONDS + " seconds")
                        .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55").getOrThrow()).withBold(true)), true);
            }
        }

        // Falling into the water hazard resets the player to their latest checkpoint (same as
        // a void fall, but caught earlier so the player doesn't sink).
        if (player.level().getBlockState(player.blockPosition()).is(Blocks.WATER)) {
            resetToCheckpoint(player);
            return;
        }

        // The modern basin frames are solid decoration; landing on their low walls or
        // supports should reset the run instead of leaving the player stranded.
        if (course.getArenaVersion() >= 2 && player.getY() < 60) {
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
        player.teleportTo(respawnPos.getX() + 0.5, (double) respawnPos.getY(), respawnPos.getZ() + 0.5);
        player.setDeltaMovement(0, 0, 0);
        player.fallDistance = 0;
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
        announcedCheckpoints.remove(player.getUUID());
    }
}
