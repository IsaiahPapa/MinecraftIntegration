package com.isaiahcreati.creatibotintegration.integration.minigame;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.helpers.Chat;
import com.isaiahcreati.creatibotintegration.integration.TntRunArena;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TntRunMinigame extends Minigame {

    /** Tracks a scheduled block removal with its start and removal ticks. */
    private record DecayEntry(long startTick, long removalTick) {}

    private final TntRunArena arena = new TntRunArena();
    private final Map<BlockPos, DecayEntry> blocksToRemove = new ConcurrentHashMap<>();
    private final Set<BlockPos> activatedBlocks = new HashSet<>();
    private final Map<UUID, Integer> lastCountdownShown = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static final int[][] CROSS_OFFSETS = {{0, 0}};

    // Passive decay: every PASSIVE_DECAY_INTERVAL_TICKS, a random block on the
    // player's current floor is scheduled for removal. This manufactures the
    // potholes/obstacles that a multi-player TNT Run would naturally create.
    private static final int PASSIVE_DECAY_INTERVAL_TICKS = 16;
    private static final int PASSIVE_DECAY_DELAY_TICKS = 12;

    private int getClosestFloorY(ServerPlayer player) {
        int playerY = (int) Math.floor(player.getY());
        int[] floorYs = arena.getFloorYLevels();
        int closestFloor = floorYs[0];
        int minDist = Math.abs(playerY - closestFloor);
        for (int i = 1; i < floorYs.length; i++) {
            int dist = Math.abs(playerY - floorYs[i]);
            if (dist < minDist) {
                minDist = dist;
                closestFloor = floorYs[i];
            }
        }
        return closestFloor;
    }

    @Override
    public String getId() { return "tntrun"; }

    @Override
    public Component getTitle() {
        return Component.literal("TNT Run").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555").getOrThrow()).withBold(true));
    }

    @Override
    public Component getSubtitle() {
        return Component.literal("Survive for " + getDurationSeconds() + " seconds!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF").getOrThrow()));
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
    public boolean hasGracePeriod() { return true; }

    @Override
    public int getGracePeriodSeconds() { return Config.TNT_RUN_GRACE_PERIOD_SECONDS.get(); }

    @Override
    protected void onGracePeriodCountdown(ServerPlayer player, int secondsRemaining) {
        Integer lastShown = lastCountdownShown.get(player.getUUID());
        if (lastShown != null && lastShown == secondsRemaining) return;

        lastCountdownShown.put(player.getUUID(), secondsRemaining);

        String text;
        String colorHex;
        if (secondsRemaining > 0) {
            text = String.valueOf(secondsRemaining);
            colorHex = secondsRemaining == 3 ? "#FF5555" : secondsRemaining == 2 ? "#FFAA00" : "#55FF55";
        } else {
            text = "GO!";
            colorHex = "#55FF55";
        }

        player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 15, 5));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.parseColor(colorHex).getOrThrow()).withBold(true))
        ));

        if (secondsRemaining <= 0) {
            lastCountdownShown.remove(player.getUUID());
        }
    }

    @Override
    public BlockPos getStartPos() { return arena.getStartPosition(); }

    @Override
    public float getFailDamage() { return Config.TNT_RUN_FAIL_DAMAGE.get().floatValue(); }

    @Override
    public void buildArena(ServerLevel level) {
        arena.buildArena(level);
    }

    @Override
    public void resetArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Resetting TNT Run arena for rebuild...");
        blocksToRemove.clear();
        activatedBlocks.clear();
    }

    @Override
    public boolean checkWin(ServerPlayer player) { return false; }

    @Override
    public boolean checkLose(ServerPlayer player) {
        return player.getY() < 49;
    }

    @Override
    public void onTick(ServerPlayer player, long currentTick, long elapsedTicks) {
        if (!MinigameDimension.isMinigameDimension(player.level())) return;

        ServerLevel level = (ServerLevel) player.level();
        BlockPos standingOn = player.getOnPos();

        int cx = standingOn.getX();
        int cz = standingOn.getZ();

        int playerFloorY = getClosestFloorY(player);

        // 1. Direct decay: the block the player is standing on.
        for (int[] offset : CROSS_OFFSETS) {
            int checkX = cx + offset[0];
            int checkZ = cz + offset[1];

            if (checkX < arena.getMinX() || checkX > arena.getMaxX()
                    || checkZ < arena.getMinZ() || checkZ > arena.getMaxZ()) continue;

            BlockPos checkPos = new BlockPos(checkX, playerFloorY, checkZ);
            if (!level.getBlockState(checkPos).isAir() && !activatedBlocks.contains(checkPos)) {
                activatedBlocks.add(checkPos);
                int decayDelay = Config.TNT_RUN_DECAY_DELAY_TICKS.get();
                blocksToRemove.put(checkPos, new DecayEntry(currentTick, currentTick + decayDelay));
            }
        }

        // 2. Passive decay: every N ticks, pick a random block on the player's
        //    current floor and schedule it for removal. This creates potholes
        //    that the player has to navigate around — manufacturing the
        //    obstacles that a multi-player TNT Run naturally generates.
        if (elapsedTicks % PASSIVE_DECAY_INTERVAL_TICKS == 0) {
            int minX = arena.getMinX(), maxX = arena.getMaxX();
            int minZ = arena.getMinZ(), maxZ = arena.getMaxZ();
            int rangeX = maxX - minX + 1;
            int rangeZ = maxZ - minZ + 1;

            // Try a few times to find an un-activated solid block within the
            // circular floor.
            for (int attempt = 0; attempt < 6; attempt++) {
                int rx = minX + random.nextInt(rangeX);
                int rz = minZ + random.nextInt(rangeZ);
                if (!arena.isInCircle(rx, rz, arena.getHalf())) continue;
                BlockPos checkPos = new BlockPos(rx, playerFloorY, rz);
                if (!level.getBlockState(checkPos).isAir() && !activatedBlocks.contains(checkPos)) {
                    activatedBlocks.add(checkPos);
                    blocksToRemove.put(checkPos, new DecayEntry(currentTick, currentTick + PASSIVE_DECAY_DELAY_TICKS));
                    break;
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
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.5F, 1.0F);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    level.destroyBlockProgress(-1, pos, -1);
                }
                it.remove();
                activatedBlocks.remove(pos);
            } else {
                long total = decay.removalTick() - decay.startTick();
                long elapsed = currentTick - decay.startTick();
                int stage = total > 0 ? (int) Math.min(9, Math.max(0, (elapsed * 10L) / total)) : 0;
                level.destroyBlockProgress(-1, pos, stage);
            }
        }
    }

    @Override
    public void onPlayerFall(ServerPlayer player) {
        exitPlayer(player, false);
    }

    @Override
    protected void onExit(ServerPlayer player, boolean success) {
        lastCountdownShown.remove(player.getUUID());
        if (success) {
            Chat.SendAlert(player, "&aYou survived the TNT Run!");
        } else {
            Chat.SendAlert(player, "&7You fell into the void! TNT Run failed!");
        }
        if (activeSessions.isEmpty()) {
            markArenaNeedsRebuild();
        }
    }
}