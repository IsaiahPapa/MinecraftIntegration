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
import net.minecraft.world.phys.AABB;

public class DropperMinigame extends Minigame {

    private static final int COUNTDOWN_SECONDS = 3;

    // Once the countdown ends the player has this long to jump before the
    // launch block crumbles under them, so nobody can stall the queue.
    private static final int LAUNCH_WINDOW_SECONDS = 10;

    // A drop takes about three seconds. Anything still running well past the
    // launch window is stuck somehow, so end it rather than block the queue.
    private static final int MAX_GAME_SECONDS = LAUNCH_WINDOW_SECONDS + 20;

    private final DropperArena arena = new DropperArena();

    @Override
    public String getId() { return "dropper"; }

    @Override
    public Component getTitle() {
        return Component.literal("Dropper").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#5555FF").getOrThrow()).withBold(true));
    }

    @Override
    public Component getSubtitle() {
        return Component.literal("Land in the glowing pool!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF").getOrThrow()));
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
    public int getGracePeriodSeconds() { return COUNTDOWN_SECONDS; }

    @Override
    public BlockPos getStartPos() { return arena.getStartPosition(); }

    @Override
    public float getStartYaw() { return arena.getStartYaw(); }

    @Override
    public float getStartPitch() { return arena.getStartPitch(); }

    @Override
    public float getFailDamage() { return Config.DROPPER_FAIL_DAMAGE.get().floatValue(); }

    @Override
    protected boolean arenaNeedsRebuild() {
        return super.arenaNeedsRebuild() || !arena.isBuiltForCurrentConfig();
    }

    @Override
    public void buildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Building Dropper course...");
        arena.buildArena(level);
    }

    @Override
    protected void onSessionStart(ServerPlayer player, ServerLevel level, MinigamePlayerState state) {
        arena.placeLaunchBlock(level);
    }

    @Override
    public boolean checkWin(ServerPlayer player) {
        return player.getBoundingBox().intersects(arena.getWaterTargetBounds());
    }

    @Override
    public boolean checkLose(ServerPlayer player) {
        // Win is checked first, so any landing below the launch block that
        // isn't in the pool (terrain or the glowing rim) is a miss.
        return player.onGround() && player.getY() < arena.getTopY();
    }

    @Override
    public void onTick(ServerPlayer player, long currentTick, long elapsedTicks) {
        if (elapsedTicks >= MAX_GAME_SECONDS * 20L) {
            exitPlayer(player, false);
            return;
        }

        if (player.getY() <= arena.getTopY()) {
            // Falling: clear the launch prompt so it doesn't linger mid-drop.
            player.sendSystemMessage(Component.empty(), true);
            return;
        }

        long ticksLeft = LAUNCH_WINDOW_SECONDS * 20L - elapsedTicks;
        if (ticksLeft <= 0) {
            ((ServerLevel) player.level()).destroyBlock(arena.getLaunchBlockPos(), false);
            return;
        }

        int secondsLeft = (int) ((ticksLeft + 19) / 20);
        String colorHex = secondsLeft <= 3 ? "#FF5555" : "#FFFF55";
        player.sendSystemMessage(Component.literal("Jump! The platform crumbles in " + secondsLeft + "s")
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor(colorHex).getOrThrow())), true);
    }

    @Override
    public void onPlayerFall(ServerPlayer player) {
        exitPlayer(player, false);
    }

    @Override
    protected AABB getArenaBounds() {
        return arena.getBounds();
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
