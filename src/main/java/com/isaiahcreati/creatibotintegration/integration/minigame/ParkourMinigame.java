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

public class ParkourMinigame extends Minigame {

    private final ParkourCourse course = new ParkourCourse();

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
    public int getDurationSeconds() { return Config.PARKOUR_DURATION_SECONDS.get(); }

    @Override
    public boolean isTimerSurvival() { return false; }

    @Override
    public BlockPos getStartPos() { return course.getStartPosition(); }

    @Override
    public float getFailDamage() { return Config.PARKOUR_FAIL_DAMAGE.get().floatValue(); }

    @Override
    public void buildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Building parkour course...");
        course.buildIfNeeded(level);
    }

    @Override
    public void resetArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Rebuilding parkour course...");
        course.forceRebuild(level);
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
    public void onTick(ServerPlayer player, long currentTick, long elapsedTicks) {}

    @Override
    public void onPlayerFall(ServerPlayer player) {
        BlockPos startPos = course.getStartPosition();
        player.teleportTo(startPos.getX() + 0.5, (double) startPos.getY(), startPos.getZ() + 0.5);
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
}