package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.integration.arena.ArenaCanvas;
import com.isaiahcreati.creatibotintegration.integration.arena.ArenaLabel;
import com.isaiahcreati.creatibotintegration.integration.arena.ArenaLabels;
import com.isaiahcreati.creatibotintegration.integration.arena.LevelCanvas;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * "Into the Depths": the player starts on a grassy floating island, looking
 * down a well, and falls through layers of the earth (stone, tuff, deepslate,
 * then a lush cave) toward a glowing pool. A spiral of lights runs down the
 * shaft wall so the speed of the fall reads on stream.
 */
public class DropperArena {

    private static final int CENTER_X = 200;
    private static final int CENTER_Z = 200;
    private static final int OUTER_RADIUS = 8;
    private static final int TOP_Y = 140;
    private static final int FLOOR_Y = 55;
    private static final int WATER_Y = 55;

    // The surface island around the top of the well.
    private static final int ISLAND_RADIUS = 13;
    private static final int RIM_TOP_Y = TOP_Y + 1;

    // The water target is offset from the center so a straight drop misses it.
    // The player must steer laterally during the fall to land in the water.
    private static final int WATER_OFFSET_X = -5;
    private static final int WATER_OFFSET_Z = 0;

    // Large enough to also clear the domed shaft built by older versions.
    private static final int CLEAR_RADIUS = 15;
    private static final int CLEAR_BOTTOM_Y = FLOOR_Y - 3;
    private static final int CLEAR_TOP_Y = TOP_Y + 14;

    // Depth bands, measured down from the top of the shaft.
    private static final int STONE_DEPTH = 22;
    private static final int TUFF_DEPTH = 44;
    private static final int DEEPSLATE_DEPTH = 66;

    private int builtWaterSize = 0;

    public BlockPos getStartPosition() {
        // Spawn on top of the single center floating block.
        return new BlockPos(CENTER_X, TOP_Y + 1, CENTER_Z);
    }

    public BlockPos getLaunchBlockPos() {
        return new BlockPos(CENTER_X, TOP_Y, CENTER_Z);
    }

    public float getStartYaw() {
        // Face the water target (west / -X) so the player sees where to steer.
        return 90f;
    }

    public float getStartPitch() {
        // Tilt the view down the shaft so the glowing target is on screen.
        return 40f;
    }

    public int getTopY() { return TOP_Y; }

    /** True once the arena has been built with the currently configured pool size. */
    public boolean isBuiltForCurrentConfig() {
        return builtWaterSize == getWaterSize();
    }

    public int getWaterSize() {
        return Math.max(1, Config.DROPPER_WATER_SIZE.get());
    }

    public AABB getBounds() {
        return new AABB(
                CENTER_X - CLEAR_RADIUS, CLEAR_BOTTOM_Y, CENTER_Z - CLEAR_RADIUS,
                CENTER_X + CLEAR_RADIUS + 1, CLEAR_TOP_Y + 1, CENTER_Z + CLEAR_RADIUS + 1);
    }

    /**
     * The pool's footprint, extended a couple of blocks up. A player whose
     * hitbox is over any part of the water when they come down has landed it,
     * even if their feet catch the rim beside it.
     */
    public AABB getWaterTargetBounds() {
        int size = getWaterSize();
        int x = CENTER_X + WATER_OFFSET_X;
        int z = CENTER_Z + WATER_OFFSET_Z;
        return new AABB(x, WATER_Y, z, x + size, WATER_Y + 2, z + size);
    }

    public void buildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Building Dropper arena...");
        builtWaterSize = getWaterSize();
        build(new LevelCanvas(level), builtWaterSize);
        ArenaLabels.replace(level, getBounds(), getLabels());
        CreatiIntegration.LOGGER.info("Dropper arena built!");
    }

    /**
     * Single-block launch point: the player must keep their footing and
     * deliberately step off into the shaft. It crumbles if they wait too long,
     * so it is re-placed at the start of every run.
     */
    public void placeLaunchBlock(ServerLevel level) {
        new LevelCanvas(level).set(CENTER_X, TOP_Y, CENTER_Z, Blocks.GOLD_BLOCK);
    }

    /** Places every block of the arena. Separate from the level so tests can build it. */
    public static void build(ArenaCanvas canvas, int waterSize) {
        canvas.clear(CENTER_X - CLEAR_RADIUS, CLEAR_BOTTOM_Y, CENTER_Z - CLEAR_RADIUS,
                CENTER_X + CLEAR_RADIUS, CLEAR_TOP_Y, CENTER_Z + CLEAR_RADIUS);

        for (int x = CENTER_X - ISLAND_RADIUS; x <= CENTER_X + ISLAND_RADIUS; x++) {
            for (int z = CENTER_Z - ISLAND_RADIUS; z <= CENTER_Z + ISLAND_RADIUS; z++) {
                int dx = x - CENTER_X;
                int dz = z - CENTER_Z;
                double distance = Math.sqrt(dx * dx + dz * dz);

                if (ArenaDome.isWallRing(x, z, CENTER_X, CENTER_Z, OUTER_RADIUS)) {
                    buildShaftColumn(canvas, x, z, dx, dz, distance);
                } else if (distance < OUTER_RADIUS - 1) {
                    canvas.set(x, FLOOR_Y - 1, z, Blocks.DEEPSLATE);
                    buildCaveFloor(canvas, x, z, dx, dz, waterSize);
                } else {
                    buildIslandColumn(canvas, x, z, distance);
                }
            }
        }

        decorateIsland(canvas);
        canvas.set(CENTER_X, TOP_Y, CENTER_Z, Blocks.GOLD_BLOCK);
    }

    private static void buildShaftColumn(ArenaCanvas canvas, int x, int z, int dx, int dz, double distance) {
        boolean innerFace = distance < OUTER_RADIUS - 0.1;
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        for (int y = FLOOR_Y - 1; y <= RIM_TOP_Y; y++) {
            if (y >= TOP_Y) {
                // The well's stone rim sits a block proud of the island's grass.
                canvas.set(x, y, z, noise(x, y, z, 3) == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS);
                continue;
            }
            int depth = TOP_Y - y;
            if (innerFace && isOnLightSpiral(angle, y)) {
                canvas.set(x, y, z, lightFor(depth));
            } else {
                canvas.set(x, y, z, persistent(rockFor(x, y, z, depth)));
            }
        }
    }

    /** Two opposing spirals, one full turn every 24 blocks of drop. */
    private static boolean isOnLightSpiral(double angleDegrees, int y) {
        double spiral = Math.floorMod(y * 15, 360);
        for (double offset : new double[]{0, 180}) {
            double diff = Math.abs(((angleDegrees - spiral - offset) % 360 + 540) % 360 - 180);
            if (diff < 6) return true;
        }
        return false;
    }

    private static Block lightFor(int depth) {
        return switch (bandFor(depth)) {
            case 0 -> Blocks.SEA_LANTERN;
            case 1 -> Blocks.OCHRE_FROGLIGHT;
            case 2 -> Blocks.PEARLESCENT_FROGLIGHT;
            default -> Blocks.SHROOMLIGHT;
        };
    }

    private static int bandFor(int depth) {
        if (depth < STONE_DEPTH) return 0;
        if (depth < TUFF_DEPTH) return 1;
        if (depth < DEEPSLATE_DEPTH) return 2;
        return 3;
    }

    private static Block rockFor(int x, int y, int z, int depth) {
        // Jitter the band edges so the layers blend instead of meeting in rings.
        int band = bandFor(depth + noise(x, y, z, 7) - 3);
        int roll = noise(x * 3, y, z * 3, 100);
        int patch = noise(Math.floorDiv(x, 2), Math.floorDiv(y, 3), Math.floorDiv(z, 2), 5);
        return switch (band) {
            case 0 -> roll < 3 ? Blocks.COAL_ORE
                    : roll < 5 ? Blocks.IRON_ORE
                    : patch == 0 ? Blocks.ANDESITE
                    : patch == 1 ? Blocks.DIORITE
                    : Blocks.STONE;
            case 1 -> roll < 4 ? Blocks.COPPER_ORE
                    : patch == 0 ? Blocks.DRIPSTONE_BLOCK
                    : patch == 1 ? Blocks.CALCITE
                    : Blocks.TUFF;
            case 2 -> roll < 2 ? Blocks.DEEPSLATE_DIAMOND_ORE
                    : roll < 4 ? Blocks.DEEPSLATE_REDSTONE_ORE
                    : roll < 6 ? Blocks.DEEPSLATE_LAPIS_ORE
                    : patch == 0 ? Blocks.AMETHYST_BLOCK
                    : patch == 1 ? Blocks.COBBLED_DEEPSLATE
                    : Blocks.DEEPSLATE;
            // Solid blocks only: leaves in a wall this thin would let the void
            // show through.
            default -> patch == 0 ? Blocks.MOSSY_COBBLESTONE
                    : patch == 1 ? Blocks.ROOTED_DIRT
                    : Blocks.MOSS_BLOCK;
        };
    }

    /** Leaves placed as decoration must be persistent or they decay away. */
    private static BlockState persistent(Block block) {
        BlockState state = block.defaultBlockState();
        return state.hasProperty(LeavesBlock.PERSISTENT) ? state.setValue(LeavesBlock.PERSISTENT, true) : state;
    }

    private static void buildCaveFloor(ArenaCanvas canvas, int x, int z, int dx, int dz, int waterSize) {
        int px = dx - WATER_OFFSET_X;
        int pz = dz - WATER_OFFSET_Z;
        boolean isWater = px >= 0 && px < waterSize && pz >= 0 && pz < waterSize;
        boolean isRim = !isWater && px >= -1 && px <= waterSize && pz >= -1 && pz <= waterSize;

        if (isWater) {
            canvas.set(x, WATER_Y, z, Blocks.WATER);
            return;
        }
        if (isRim) {
            // Flush with the water, so there is no lip to land on, and bright
            // enough to read as the target from the top of the shaft.
            canvas.set(x, FLOOR_Y, z, Blocks.GLOWSTONE);
            return;
        }

        int heightRoll = noise(x, 0, z, 29);
        int rise = heightRoll == 0 ? 3 : heightRoll <= 4 ? 2 : heightRoll <= 13 ? 1 : 0;
        for (int dy = 0; dy < rise; dy++) {
            canvas.set(x, FLOOR_Y + dy, z, Blocks.DIRT);
        }
        int topY = FLOOR_Y + rise;
        // Deliberately no blue/cyan tops: from the top of the shaft they were
        // indistinguishable from the water.
        int variant = noise(x, 1, z, 10);
        canvas.set(x, topY, z, variant < 7 ? Blocks.MOSS_BLOCK : variant < 9 ? Blocks.ROOTED_DIRT : Blocks.CLAY);

        int decoration = noise(x, 2, z, 12);
        if (decoration == 0) {
            canvas.set(x, topY + 1, z, Blocks.FIREFLY_BUSH);
        } else if (decoration == 1) {
            canvas.set(x, topY + 1, z, Blocks.AZALEA);
        } else if (decoration <= 4) {
            canvas.set(x, topY + 1, z, Blocks.MOSS_CARPET);
        }
    }

    private static void buildIslandColumn(ArenaCanvas canvas, int x, int z, double distance) {
        if (distance > ISLAND_RADIUS + 0.4) return;
        canvas.set(x, TOP_Y, z, Blocks.GRASS_BLOCK);
        canvas.set(x, TOP_Y - 1, z, Blocks.DIRT);
        // The underside tapers like a floating island.
        int depth = 2 + (int) Math.round((ISLAND_RADIUS - distance) * 0.9) + noise(x, 0, z, 3);
        for (int dy = 2; dy <= depth; dy++) {
            canvas.set(x, TOP_Y - dy, z, dy <= 3 ? Blocks.DIRT : noise(x, dy, z, 4) == 0 ? Blocks.ANDESITE : Blocks.STONE);
        }
    }

    private static void decorateIsland(ArenaCanvas canvas) {
        // Lantern posts at the four compass points around the well.
        for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int x = CENTER_X + dir[0] * 10;
            int z = CENTER_Z + dir[1] * 10;
            canvas.set(x, TOP_Y + 1, z, Blocks.OAK_FENCE);
            canvas.set(x, TOP_Y + 2, z, Blocks.OAK_FENCE);
            canvas.set(x, TOP_Y + 3, z, Blocks.LANTERN);
        }

        // A few leafy bushes (persistent, so they never decay).
        for (int[] bush : new int[][]{{7, 8}, {-9, -6}, {-4, 11}}) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int height = (dx == 0 && dz == 0) ? 2 : 1;
                    for (int dy = 1; dy <= height; dy++) {
                        canvas.set(CENTER_X + bush[0] + dx, TOP_Y + dy, CENTER_Z + bush[1] + dz,
                                persistent(Blocks.OAK_LEAVES));
                    }
                }
            }
        }

        Block[] flowers = {Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.POPPY, Blocks.DANDELION,
                Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY};
        for (int x = CENTER_X - ISLAND_RADIUS; x <= CENTER_X + ISLAND_RADIUS; x++) {
            for (int z = CENTER_Z - ISLAND_RADIUS; z <= CENTER_Z + ISLAND_RADIUS; z++) {
                if (!canvas.get(x, TOP_Y, z).is(Blocks.GRASS_BLOCK) || !canvas.isAir(x, TOP_Y + 1, z)) continue;
                if (noise(x, 5, z, 9) < 4) {
                    canvas.set(x, TOP_Y + 1, z, flowers[noise(x, 6, z, flowers.length)]);
                }
            }
        }
    }

    /** Deterministic pseudo-random value in [0, bound) for a block position. */
    private static int noise(int x, int y, int z, int bound) {
        long h = x * 73856093L ^ y * 19349663L ^ z * 83492791L;
        h ^= (h >>> 13);
        h *= 0x5bd1e995L;
        h ^= (h >>> 15);
        return (int) Math.floorMod(h, (long) bound);
    }

    private static List<ArenaLabel> getLabels() {
        // In front of the player (who spawns facing -X), above the far rim.
        double x = CENTER_X - 9.5;
        double z = CENTER_Z + 0.5;
        return List.of(
                new ArenaLabel(x, TOP_Y + 5.2, z, Component.literal("§9§lDROPPER"), 2.5F),
                new ArenaLabel(x, TOP_Y + 4.2, z, Component.literal("§7Land in the glowing pool!"), 1.2F));
    }
}
