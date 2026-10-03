package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.integration.arena.ArenaCanvas;
import com.isaiahcreati.creatibotintegration.integration.arena.ArenaLabel;
import com.isaiahcreati.creatibotintegration.integration.arena.ArenaLabels;
import com.isaiahcreati.creatibotintegration.integration.arena.LevelCanvas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ParkourCourse {

    /** Inclusive block area a player stands in (feet level) to finish. */
    private record FinishPad(int minX, int maxX, int feetY, int minZ, int maxZ) {
        boolean contains(BlockPos feet) {
            return feet.getY() == feetY
                    && feet.getX() >= minX && feet.getX() <= maxX
                    && feet.getZ() >= minZ && feet.getZ() <= maxZ;
        }
    }

    /** Materials for the gate the player starts behind. */
    private record GateStyle(Block pillar, Block cap, Block lintel, Block keystone) {}

    private static final BlockPos V1_START = new BlockPos(0, 65, 0);
    private static final FinishPad V1_FINISH = new FinishPad(-1, 1, 67, 41, 43);
    private static final BlockPos V2_START = new BlockPos(0, 65, 0);
    private static final FinishPad V2_FINISH = new FinishPad(-2, 2, 70, 46, 50);
    private static final BlockPos V2_CHECKPOINT_PLATE = new BlockPos(0, 67, 23);
    private static final BlockPos V2_CHECKPOINT_RESPAWN = new BlockPos(0, 67, 22);
    private static final BlockPos V3_START = new BlockPos(0, 65, 0);
    private static final FinishPad V3_FINISH = new FinishPad(0, 4, 70, 51, 55);
    private static final BlockPos V3_CHECKPOINT_PLATE = new BlockPos(-3, 67, 24);
    private static final BlockPos V3_CHECKPOINT_RESPAWN = new BlockPos(-3, 67, 23);

    private boolean courseBuilt = false;
    private int builtVersion = 0;

    public int getArenaVersion() {
        return courseBuilt ? builtVersion : getConfiguredArenaVersion();
    }

    private int getConfiguredArenaVersion() {
        return Math.max(1, Math.min(3, Config.PARKOUR_ARENA_VERSION.get()));
    }

    public BlockPos getStartPosition() {
        return switch (getArenaVersion()) {
            case 1 -> V1_START;
            case 3 -> V3_START;
            default -> V2_START;
        };
    }

    /**
     * Landing anywhere on the finish pad completes the run; the pressure plate
     * only marks the middle. Requiring the exact plate made players who stuck
     * the final jump still lose seconds shuffling onto one block.
     */
    public boolean isOnFinishPad(BlockPos feet) {
        FinishPad pad = switch (getArenaVersion()) {
            case 1 -> V1_FINISH;
            case 3 -> V3_FINISH;
            default -> V2_FINISH;
        };
        return pad.contains(feet);
    }

    public boolean hasCheckpoint() {
        return getArenaVersion() >= 2;
    }

    public boolean isInCheckpointArea(BlockPos pos) {
        if (pos.getY() != 67) return false;
        if (getArenaVersion() == 2) {
            return pos.getX() >= -1 && pos.getX() <= 1
                    && pos.getZ() >= 22 && pos.getZ() <= 24;
        }
        if (getArenaVersion() == 3) {
            return pos.getX() >= -4 && pos.getX() <= -2
                    && pos.getZ() >= 23 && pos.getZ() <= 25;
        }
        return false;
    }

    public BlockPos getCheckpointRespawnPosition() {
        return getArenaVersion() == 3 ? V3_CHECKPOINT_RESPAWN : V2_CHECKPOINT_RESPAWN;
    }

    public AABB getBounds() {
        return new AABB(-12, 48, -8, 13, 96, 61);
    }

    public boolean needsRebuild() {
        return !courseBuilt || builtVersion != getConfiguredArenaVersion();
    }

    public void buildIfNeeded(ServerLevel parkourLevel) {
        if (!needsRebuild()) return;
        int version = getConfiguredArenaVersion();
        CreatiIntegration.LOGGER.info("Building parkour course version {}...", version);
        build(new LevelCanvas(parkourLevel), version);
        ArenaLabels.replace(parkourLevel, getBounds(), getLabels(version));
        courseBuilt = true;
        builtVersion = version;
        CreatiIntegration.LOGGER.info("Parkour course version {} built!", version);
    }

    /** Places every block of the course. Separate from the level so tests can build it. */
    public static void build(ArenaCanvas canvas, int version) {
        // Clear the full course envelope so layout changes never leave stale
        // platforms, frames, or water behind when switching versions.
        canvas.clear(-12, 48, -8, 12, 78, 60);

        switch (version) {
            case 1 -> buildLegacyCourse(canvas);
            case 3 -> buildPrismRelay(canvas);
            default -> buildFoundrySprint(canvas);
        }
    }

    private static void buildLegacyCourse(ArenaCanvas canvas) {
        buildStartGate(canvas, 1, 2,
                new GateStyle(Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.GOLD_BLOCK));

        // 1. Start platform (3x3 gold)
        buildPlatform(canvas, 0, 64, 0, 3, Blocks.GOLD_BLOCK);

        // 2. Flat warmup gap (1x1 stone)
        buildPlatform(canvas, 0, 64, 3, 1, Blocks.STONE);

        // 3. Lateral movement (1x1 stone)
        buildPlatform(canvas, 1, 64, 6, 1, Blocks.STONE);

        // 4. Step up +1 (1x1 andesite)
        buildPlatform(canvas, 0, 65, 9, 1, Blocks.POLISHED_ANDESITE);

        // 5. Slime block bounce (1x1) — landing on slime bounces the player;
        //    they must control the bounce to reach the next platform.
        buildPlatform(canvas, 0, 65, 13, 1, Blocks.SLIME_BLOCK);

        // 6. Landing after slime bounce (1x1 stone)
        buildPlatform(canvas, 0, 65, 16, 1, Blocks.STONE);

        // 7. Packed ice slide platform (3x3) — the player must control their
        //    momentum to avoid sliding off the edge.
        buildPlatform(canvas, 0, 65, 19, 3, Blocks.PACKED_ICE);

        // 8. Step up +1 from ice (1x1 cobblestone)
        buildPlatform(canvas, 0, 66, 22, 1, Blocks.COBBLESTONE);

        // 9. Ladder climb segment.
        //    Cobblestone pillar (support) with ladders on the -Z face.
        int ladderZ = 25;
        for (int dy = 1; dy <= 3; dy++) {
            canvas.set(0, 66 + dy, ladderZ, Blocks.COBBLESTONE);
            canvas.set(0, 66 + dy, ladderZ - 1,
                    Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        }
        // Top of the pillar — landing before the next jump.
        buildPlatform(canvas, 0, 70, 25, 1, Blocks.STONE_BRICKS);

        // 10. Jump from ladder top (1x1 granite)
        buildPlatform(canvas, 0, 70, 28, 1, Blocks.POLISHED_GRANITE);

        // 11. Step down -1 (1x1 smooth stone)
        buildPlatform(canvas, 0, 69, 31, 1, Blocks.SMOOTH_STONE);

        // 12. Slime block bounce landing (1x1) — the player bounces on landing,
        //     making the takeoff for the next jump require timing.
        buildPlatform(canvas, 0, 69, 34, 1, Blocks.SLIME_BLOCK);

        // 13. Jump from honey — harder because honey slows momentum.
        buildPlatform(canvas, 0, 69, 37, 1, Blocks.HONEY_BLOCK);

        // 14. Water hazard gap (1x1 iron) — a 3-block gap over a water pit.
        //     Falling into the water resets the player to the start.
        buildPlatform(canvas, 0, 69, 40, 1, Blocks.IRON_BLOCK);
        // Water pit below the gap (visual hazard + early-reset trigger), on a
        // solid bed so it can't flow away into the void below the course.
        for (int dx = -1; dx <= 1; dx++) {
            for (int z = 38; z <= 39; z++) {
                canvas.set(dx, 62, z, Blocks.STONE);
                canvas.set(dx, 63, z, Blocks.WATER);
            }
        }

        // 15. End platform (3x3 diamond) with pressure plate — step down -3.
        buildPlatform(canvas, 0, 66, 42, 3, Blocks.DIAMOND_BLOCK);
        buildFinishBeacon(canvas, 0, 66, 42);
    }

    /**
     * Version 2: a compact industrial sprint with readable jumps, one narrow
     * balance section, a midpoint checkpoint, an ascending diagonal, and a
     * momentum finish. A water basin returns misses quickly so a paid Twitch
     * interaction stays tense without becoming a long punishment.
     */
    private static void buildFoundrySprint(ArenaCanvas canvas) {
        buildResetBasin(canvas);

        for (int frameZ : new int[]{-3, 13, 27, 41, 54}) {
            buildFoundryFrame(canvas, frameZ);
        }

        // Start deck: enough room to orient, but the route immediately narrows.
        buildRect(canvas, -2, 2, 64, -2, 2, Blocks.POLISHED_BLACKSTONE_BRICKS);
        buildRect(canvas, -1, 1, 64, -1, 1, Blocks.GOLD_BLOCK);
        canvas.set(0, 64, 2, Blocks.LIME_CONCRETE);
        buildStartGate(canvas, 2, 3,
                new GateStyle(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.COPPER_BLOCK, Blocks.CUT_COPPER, Blocks.SEA_LANTERN));

        // First half: diagonal changes and narrow landings demand deliberate
        // movement without turning the route into frame-perfect jumps.
        buildRect(canvas, 0, 1, 64, 5, 6, Blocks.CUT_COPPER);
        buildRect(canvas, -2, -2, 65, 9, 10, Blocks.EXPOSED_CUT_COPPER);
        buildRect(canvas, -3, -3, 65, 13, 15, Blocks.WEATHERED_CUT_COPPER);
        buildRect(canvas, -1, -1, 66, 18, 19, Blocks.OXIDIZED_CUT_COPPER);

        // Midpoint checkpoint. Missing after this point returns here rather
        // than invalidating the entire run.
        buildRect(canvas, -1, 1, 66, 22, 24, Blocks.POLISHED_BLACKSTONE_BRICKS);
        canvas.set(0, 66, 23, Blocks.GOLD_BLOCK);
        set(canvas, V2_CHECKPOINT_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);

        // Second half: compact 1x2 landings make the rising diagonal less
        // automatic while preserving a readable sprint rhythm.
        buildRect(canvas, 3, 3, 67, 27, 28, Blocks.CUT_COPPER);
        buildRect(canvas, 5, 6, 68, 31, 31, Blocks.EXPOSED_CUT_COPPER);
        // Kept within a 1x2 diagonal of the previous pad: the old position
        // asked for a three-block gap while climbing, the only near-maximum
        // jump on an otherwise rhythm-focused course.
        buildRect(canvas, 3, 3, 69, 34, 35, Blocks.WEATHERED_CUT_COPPER);

        // Ice supplies momentum for the final three-block gap. The finish is
        // 5x5, so the last jump has a generous landing instead of a tiny pad.
        buildRect(canvas, 0, 0, 69, 38, 42, Blocks.PACKED_ICE);
        canvas.set(0, 69, 42, Blocks.LIME_CONCRETE);
        buildRect(canvas, -2, 2, 69, 46, 50, Blocks.DIAMOND_BLOCK);
        buildRect(canvas, -1, 1, 69, 47, 49, Blocks.EMERALD_BLOCK);
        buildFinishBeacon(canvas, 0, 69, 48);
    }

    /**
     * Version 3: a harder zig-zag course through bright arcane ruins. The
     * route alternates thin 1x2 pads, height changes, and diagonal jumps so it
     * feels distinct from the mostly forward Foundry Sprint. One checkpoint
     * keeps the Twitch interaction quick even when the player misses late.
     */
    private static void buildPrismRelay(ArenaCanvas canvas) {
        buildRelayBasin(canvas);

        for (int archZ : new int[]{-3, 10, 24, 38, 56}) {
            buildRelayArch(canvas, archZ);
        }

        // Broad launch deck, followed by an alternating series of increasingly
        // precise landings. The cyan block clearly identifies the first jump.
        buildRect(canvas, -2, 2, 64, -2, 2, Blocks.QUARTZ_BRICKS);
        buildRect(canvas, -1, 1, 64, -1, 1, Blocks.AMETHYST_BLOCK);
        canvas.set(0, 64, 2, Blocks.CYAN_CONCRETE);
        buildStartGate(canvas, 2, 3,
                new GateStyle(Blocks.QUARTZ_PILLAR, Blocks.AMETHYST_BLOCK, Blocks.SMOOTH_QUARTZ, Blocks.SEA_LANTERN));

        buildRect(canvas, -3, -2, 64, 5, 6, Blocks.PURPUR_BLOCK);
        buildRect(canvas, 0, 0, 65, 9, 10, Blocks.CHISELED_QUARTZ_BLOCK);
        buildRect(canvas, 3, 4, 65, 13, 13, Blocks.AMETHYST_BLOCK);
        buildRect(canvas, 1, 1, 65, 16, 17, Blocks.PURPUR_BLOCK);
        buildRect(canvas, -2, -1, 66, 20, 20, Blocks.CHISELED_QUARTZ_BLOCK);

        // The relay pad is large enough to stabilize, but the route immediately
        // returns to narrow alternating jumps afterward.
        buildRect(canvas, -4, -2, 66, 23, 25, Blocks.QUARTZ_BRICKS);
        canvas.set(-3, 66, 24, Blocks.GOLD_BLOCK);
        set(canvas, V3_CHECKPOINT_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);

        buildRect(canvas, 0, 1, 67, 28, 29, Blocks.AMETHYST_BLOCK);
        buildRect(canvas, 3, 3, 68, 32, 33, Blocks.PURPUR_BLOCK);
        buildRect(canvas, 0, 0, 68, 36, 37, Blocks.CHISELED_QUARTZ_BLOCK);
        buildRect(canvas, -3, -2, 69, 40, 40, Blocks.AMETHYST_BLOCK);
        buildRect(canvas, 0, 0, 69, 43, 44, Blocks.PURPUR_BLOCK);
        buildRect(canvas, 3, 4, 69, 47, 47, Blocks.CHISELED_QUARTZ_BLOCK);

        // A three-block final gap with a broad landing, so success is about
        // carrying momentum rather than hitting one exact pixel.
        buildRect(canvas, 0, 4, 69, 51, 55, Blocks.QUARTZ_BRICKS);
        buildRect(canvas, 1, 3, 69, 52, 54, Blocks.DIAMOND_BLOCK);
        buildFinishBeacon(canvas, 2, 69, 53);
    }

    /**
     * A gate straddling the front edge of the start deck, so the course
     * visibly begins at a doorway. The lintel sits high enough to clear a
     * full jump through it.
     */
    private static void buildStartGate(ArenaCanvas canvas, int z, int halfWidth, GateStyle style) {
        for (int x : new int[]{-halfWidth, halfWidth}) {
            for (int y = 64; y <= 68; y++) {
                canvas.set(x, y, z, y == 68 ? style.cap() : style.pillar());
            }
        }
        for (int x = -halfWidth; x <= halfWidth; x++) {
            canvas.set(x, 69, z, x == 0 ? style.keystone() : style.lintel());
        }
    }

    /**
     * Turns the middle of the finish pad into a beacon on an iron base. Its
     * beam is visible from the start, so the goal is never in doubt.
     */
    private static void buildFinishBeacon(ArenaCanvas canvas, int x, int y, int z) {
        buildRect(canvas, x - 1, x + 1, y - 1, z - 1, z + 1, Blocks.IRON_BLOCK);
        canvas.set(x, y, z, Blocks.BEACON);
        canvas.set(x, y + 1, z, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    private static void buildResetBasin(ArenaCanvas canvas) {
        int minX = -10, maxX = 10;
        int minZ = -4, maxZ = 54;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean boundary = x == minX || x == maxX || z == minZ || z == maxZ;
                canvas.set(x, 50, z, Blocks.DARK_PRISMARINE);
                if (boundary) {
                    for (int y = 51; y <= 54; y++) {
                        canvas.set(x, y, z, (y == 54 && Math.floorMod(x + z, 6) == 0)
                                ? Blocks.SEA_LANTERN
                                : Blocks.PRISMARINE_BRICKS);
                    }
                } else {
                    canvas.set(x, 51, z, Blocks.WATER);
                }
            }
        }
    }

    private static void buildFoundryFrame(ArenaCanvas canvas, int z) {
        for (int y = 55; y <= 74; y++) {
            Block columnBlock = y % 5 == 0 ? Blocks.COPPER_BLOCK : Blocks.DEEPSLATE_BRICKS;
            canvas.set(-10, y, z, columnBlock);
            canvas.set(10, y, z, columnBlock);
        }
        for (int x = -10; x <= 10; x++) {
            canvas.set(x, 74, z, (x == -6 || x == 0 || x == 6)
                    ? Blocks.SEA_LANTERN
                    : Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
    }

    private static void buildRelayBasin(ArenaCanvas canvas) {
        int minX = -10, maxX = 10;
        int minZ = -4, maxZ = 58;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean boundary = x == minX || x == maxX || z == minZ || z == maxZ;
                canvas.set(x, 50, z, Blocks.QUARTZ_BRICKS);
                if (boundary) {
                    for (int y = 51; y <= 54; y++) {
                        canvas.set(x, y, z, (y == 54 && Math.floorMod(x + z, 5) == 0)
                                ? Blocks.SEA_LANTERN
                                : Blocks.PURPUR_BLOCK);
                    }
                } else {
                    canvas.set(x, 51, z, Blocks.WATER);
                }
            }
        }
    }

    private static void buildRelayArch(ArenaCanvas canvas, int z) {
        for (int y = 55; y <= 75; y++) {
            Block columnBlock = y % 4 == 0 ? Blocks.AMETHYST_BLOCK : Blocks.QUARTZ_PILLAR;
            canvas.set(-9, y, z, columnBlock);
            canvas.set(9, y, z, columnBlock);
        }
        for (int x = -9; x <= 9; x++) {
            canvas.set(x, 75, z, Math.floorMod(x, 4) == 0
                    ? Blocks.SEA_LANTERN
                    : Blocks.SMOOTH_QUARTZ);
        }
    }

    private static List<ArenaLabel> getLabels(int version) {
        return switch (version) {
            case 1 -> List.of(
                    new ArenaLabel(0.5, 70.6, 1.5, Component.literal("§a§lSTART"), 1.6F),
                    ArenaLabel.of(0.5, 70.0, 42.5, Component.literal("§b§lFinish!")));
            case 3 -> List.of(
                    new ArenaLabel(0.5, 71.0, 2.5, Component.literal("§d§lPRISM RELAY"), 1.8F),
                    ArenaLabel.of(0.5, 68.2, 2.5, Component.literal("§7Commit to the diagonals")),
                    ArenaLabel.of(-2.5, 69.5, 24.5, Component.literal("§e§lCheckpoint")),
                    new ArenaLabel(2.5, 73.0, 53.5, Component.literal("§b§lFinish!"), 1.6F));
            default -> List.of(
                    new ArenaLabel(0.5, 71.0, 2.5, Component.literal("§6§lFOUNDRY SPRINT"), 1.8F),
                    ArenaLabel.of(0.5, 68.2, 2.5, Component.literal("§7Keep your momentum")),
                    ArenaLabel.of(0.5, 69.5, 23.5, Component.literal("§e§lCheckpoint")),
                    new ArenaLabel(0.5, 73.0, 48.5, Component.literal("§b§lFinish!"), 1.6F));
        };
    }

    private static void set(ArenaCanvas canvas, BlockPos pos, Block block) {
        canvas.set(pos.getX(), pos.getY(), pos.getZ(), block);
    }

    private static void buildRect(ArenaCanvas canvas, int minX, int maxX, int y,
                                  int minZ, int maxZ, Block block) {
        canvas.fill(minX, y, minZ, maxX, y, maxZ, block);
    }

    /**
     * Builds a size x size platform centered on (centerX, centerZ) at height y.
     * size=1 -> 1x1, size=2 -> 2x2, size=3 -> 3x3 (correctly centered).
     */
    private static void buildPlatform(ArenaCanvas canvas, int centerX, int y, int centerZ, int size, Block block) {
        int offset = (size - 1) / 2;
        canvas.fill(centerX - offset, y, centerZ - offset,
                centerX - offset + size - 1, y, centerZ - offset + size - 1, block);
    }
}
