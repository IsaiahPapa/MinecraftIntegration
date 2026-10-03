package com.isaiahcreati.creatibotintegration.integration.minigame;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.helpers.Chat;
import com.isaiahcreati.creatibotintegration.integration.TntRunArena;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TntRunMinigame extends Minigame {

    /** Tracks a scheduled removal and whether it is a visible random pothole. */
    private record DecayEntry(long startTick, long removalTick, boolean showCracks) {}

    private final TntRunArena arena = new TntRunArena();
    private final Map<BlockPos, DecayEntry> blocksToRemove = new ConcurrentHashMap<>();
    private final Set<BlockPos> activatedBlocks = new HashSet<>();
    private final Random random = new Random();

    // Passive decay: every PASSIVE_DECAY_INTERVAL_TICKS, a random block on the
    // player's current floor is scheduled for removal. This manufactures the
    // potholes/obstacles that a multi-player TNT Run would naturally create.
    private static final int PASSIVE_DECAY_INTERVAL_TICKS = 20;

    // How far above a floor's walking surface the player can be and still be
    // "on" it. Covers a full jump, but not dropping in from the floor above,
    // which would otherwise start removing the landing spot mid-fall.
    private static final double MAX_HEIGHT_ABOVE_FLOOR = 1.3;

    private static int getCrackAnimationId(BlockPos pos) {
        // Clients key crack overlays by breaker ID. A stable negative ID per
        // block allows several potholes to animate at the same time without
        // replacing one another's overlay.
        return -1 - (pos.hashCode() & Integer.MAX_VALUE);
    }

    /** Index of the highest floor below the player's feet, or -1 if they are below every floor. */
    private int getFloorIndexBelow(ServerPlayer player) {
        int[] floorYs = arena.getFloorYLevels();
        for (int i = 0; i < floorYs.length; i++) {
            if (player.getY() >= floorYs[i] + 1 - 0.01) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getId() { return "tntrun"; }

    @Override
    public Component getTitle() {
        return Component.literal("TNT Run").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555").getOrThrow()).withBold(true));
    }

    @Override
    public Component getSubtitle() {
        return Component.literal("Keep moving and survive " + getDurationSeconds() + " seconds!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF").getOrThrow()));
    }

    @Override
    public boolean isEnabled() { return Config.TNT_RUN_ENABLED.get(); }

    @Override
    public boolean hasTimer() { return true; }

    @Override
    public int getDurationSeconds() { return Config.TNT_RUN_DURATION_SECONDS.get(); }

    @Override
    public boolean isTimerSurvival() { return true; }

    @Override
    public int getGracePeriodSeconds() { return Config.TNT_RUN_GRACE_PERIOD_SECONDS.get(); }

    // Let the player pick a starting spot during the countdown; nothing
    // decays until it ends.
    @Override
    protected boolean freezeDuringGracePeriod() { return false; }

    // Fail as soon as the player drops out of the bottom floor, rather than
    // after a further ten-block fall to the shared void line.
    @Override
    protected double getVoidY() { return arena.getLowestFloorY() - 3; }

    @Override
    public BlockPos getStartPos() { return arena.getStartPosition(); }

    @Override
    public float getFailDamage() { return Config.TNT_RUN_FAIL_DAMAGE.get().floatValue(); }

    @Override
    public void buildArena(ServerLevel level) {
        arena.buildArena(level);
    }

    @Override
    protected String getStatusText(ServerPlayer player, int remainingSeconds) {
        int floorIndex = Math.max(0, getFloorIndexBelow(player));
        return super.getStatusText(player, remainingSeconds)
                + "  |  Floor " + (floorIndex + 1) + "/" + arena.getFloorCount();
    }

    private void clearDecay(ServerLevel level) {
        for (Map.Entry<BlockPos, DecayEntry> entry : blocksToRemove.entrySet()) {
            if (entry.getValue().showCracks()) {
                level.destroyBlockProgress(getCrackAnimationId(entry.getKey()), entry.getKey(), -1);
            }
        }
        blocksToRemove.clear();
        activatedBlocks.clear();
    }

    @Override
    public boolean checkWin(ServerPlayer player) { return false; }

    @Override
    public boolean checkLose(ServerPlayer player) { return false; }

    private void scheduleRemoval(ServerLevel level, BlockPos pos, long currentTick, int delayTicks, boolean showCracks) {
        if (level.getBlockState(pos).isAir() || !activatedBlocks.add(pos)) return;
        blocksToRemove.put(pos, new DecayEntry(currentTick, currentTick + delayTicks, showCracks));
    }

    @Override
    public void onTick(ServerPlayer player, long currentTick, long elapsedTicks) {
        ServerLevel level = (ServerLevel) player.level();
        int[] floorYs = arena.getFloorYLevels();
        int floorIndex = getFloorIndexBelow(player);

        if (floorIndex >= 0) {
            int floorY = floorYs[floorIndex];

            // 1. Direct decay: the block the player is standing (or hopping) on.
            //    When on the ground, use the block that is actually supporting
            //    them; otherwise a player balanced on the edge of a hole, with
            //    their center over air, would never lose their footing.
            if (player.getY() - (floorY + 1) <= MAX_HEIGHT_ABOVE_FLOOR) {
                BlockPos onPos = player.getOnPos();
                BlockPos standingOn = player.onGround() && onPos.getY() == floorY
                        ? onPos
                        : new BlockPos(player.getBlockX(), floorY, player.getBlockZ());
                scheduleRemoval(level, standingOn, currentTick, Config.TNT_RUN_DECAY_DELAY_TICKS.get(), false);
            }

            // 2. Passive decay: every N ticks, pick a random walkable block on
            //    the player's current floor and schedule it for removal. This
            //    creates potholes the player has to navigate around.
            if (elapsedTicks % PASSIVE_DECAY_INTERVAL_TICKS == 0) {
                int radius = arena.getWalkableRadius();
                BlockPos start = arena.getStartPosition();
                for (int attempt = 0; attempt < 6; attempt++) {
                    int rx = start.getX() - radius + random.nextInt(radius * 2 + 1);
                    int rz = start.getZ() - radius + random.nextInt(radius * 2 + 1);
                    if (!arena.isInCircle(rx, rz, radius)) continue;
                    BlockPos pos = new BlockPos(rx, floorY, rz);
                    if (!level.getBlockState(pos).isAir() && !activatedBlocks.contains(pos)) {
                        scheduleRemoval(level, pos, currentTick, Config.TNT_RUN_POTHOLE_WARNING_TICKS.get(), true);
                        break;
                    }
                }
            }
        }

        // 3. Update visual crack stages and process scheduled removals.
        //    The crack stage goes 0 -> 9 as the block approaches removal, so
        //    the player can see potholes forming (lightly broken -> about to
        //    break -> gone).
        Iterator<Map.Entry<BlockPos, DecayEntry>> it = blocksToRemove.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, DecayEntry> entry = it.next();
            BlockPos pos = entry.getKey();
            DecayEntry decay = entry.getValue();

            if (currentTick >= decay.removalTick()) {
                if (decay.showCracks()) {
                    level.destroyBlockProgress(getCrackAnimationId(pos), pos, -1);
                }
                // Break particles and sound make the floor visibly crumble.
                level.destroyBlock(pos, false);
                it.remove();
                activatedBlocks.remove(pos);
            } else if (decay.showCracks()) {
                long total = decay.removalTick() - decay.startTick();
                long elapsed = currentTick - decay.startTick();
                int stage = total > 0 ? (int) Math.min(9, Math.max(0, (elapsed * 10L) / total)) : 0;
                level.destroyBlockProgress(getCrackAnimationId(pos), pos, stage);
            }
        }
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
            Chat.SendAlert(player, "&aYou survived the TNT Run!");
        } else {
            Chat.SendAlert(player, "&7You fell into the void! TNT Run failed!");
        }
    }

    @Override
    protected void onSessionEnd(ServerPlayer player) {
        // The floors are consumed during play, so rebuild before the next run.
        ServerLevel level = MinigameDimension.getMinigameLevel(player);
        if (level != null) {
            clearDecay(level);
        }
        if (activeSessions.isEmpty()) {
            arenaBuilt = false;
        }
    }
}
