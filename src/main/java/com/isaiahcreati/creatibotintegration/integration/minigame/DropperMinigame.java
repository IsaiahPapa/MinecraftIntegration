package com.isaiahcreati.creatibotintegration.integration.minigame;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.helpers.Chat;
import com.isaiahcreati.creatibotintegration.integration.DropperArena;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public class DropperMinigame extends Minigame {

    private final DropperArena arena = new DropperArena();

    @Override
    public String getId() { return "dropper"; }

    @Override
    public Component getTitle() {
        return Component.literal("Dropper").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#5555FF").getOrThrow()).withBold(true));
    }

    @Override
    public Component getSubtitle() {
        return Component.literal("Land in the water!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF").getOrThrow()));
    }

    @Override
    public boolean isEnabled() { return Config.DROPPER_ENABLED.get(); }

    @Override
    public boolean hasTimer() { return false; }

    @Override
    public int getDurationSeconds() { return 0; }

    @Override
    public boolean isTimerSurvival() { return false; }

    @Override
    public BlockPos getStartPos() { return arena.getStartPosition(); }

    @Override
    public float getStartYaw() { return arena.getStartYaw(); }

    @Override
    public float getFailDamage() { return Config.DROPPER_FAIL_DAMAGE.get().floatValue(); }

    @Override
    public void buildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Building Dropper course...");
        arena.buildArena(level);
    }

    @Override
    public boolean checkWin(ServerPlayer player) {
        return isTouchingWaterTarget(player);
    }

    @Override
    public boolean checkLose(ServerPlayer player) {
        if (player.getY() > arena.getMaxTerrainY() + 2) return false;
        if (isTouchingWaterTarget(player)) return false;

        // The landing environment is intentionally uneven, so failure is
        // based on contacting any solid terrain rather than one fixed floor Y.
        return !player.level().getBlockState(player.getOnPos()).isAir()
                && !player.level().getBlockState(player.getOnPos()).is(Blocks.WATER);
    }

    @Override
    public void onTick(ServerPlayer player, long currentTick, long elapsedTicks) {}

    @Override
    public void onPlayerFall(ServerPlayer player) {
        exitPlayer(player, false);
    }

    @Override
    protected AABB getArenaBounds() {
        return arena.getBounds();
    }

    private boolean isTouchingWaterTarget(ServerPlayer player) {
        return player.getBoundingBox().intersects(arena.getWaterTargetBounds());
    }

    @Override
    protected void onExit(ServerPlayer player, boolean success) {
        if (success) {
            Chat.SendAlert(player, "&aYou landed the Dropper!");
        } else {
            Chat.SendAlert(player, "&7You missed the water! Dropper failed!");
        }
    }
}
