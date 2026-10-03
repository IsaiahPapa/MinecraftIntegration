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
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * A demolition pit: stacked checkerboard floors inside a dark blast wall,
 * with a glowing magma pit at the bottom. Each floor has its own colors so
 * both the holes and the floor the player is on are easy to read on stream.
 */
public class TntRunArena {

    private static final int CENTER_X = 200;
    private static final int CENTER_Z = 0;

    // Fixed floor Y levels (top, middle, bottom) — up to 3 floors. The number
    // actually used is driven by Config.TNT_RUN_FLOOR_COUNT.
    public static final int FLOOR_1_Y = 64;
    public static final int FLOOR_2_Y = 59;
    public static final int FLOOR_3_Y = 54;
    public static final int[] FLOOR_Y_LEVELS_ALL = {FLOOR_1_Y, FLOOR_2_Y, FLOOR_3_Y};

    // Two-tone checkerboard per floor, top to bottom.
    private static final Block[][] FLOOR_PALETTES = {
            {Blocks.RED_CONCRETE, Blocks.WHITE_CONCRETE},
            {Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE},
            {Blocks.LIGHT_BLUE_CONCRETE, Blocks.CYAN_CONCRETE},
    };

    private static final int PIT_Y = 49;
    private static final int WALL_TOP_Y = FLOOR_1_Y + 8;
    private static final int HAZARD_STRIPES = 40;

    // Clear box is deliberately larger than any possible arena to flush stale
    // geometry from previous builds and other floor sizes.
    private static final int CLEAR_HALF = 20;
    private static final int CLEAR_BOTTOM_Y = 49;
    private static final int CLEAR_TOP_Y = 92; // covers the largest configurable dome roof

    // Geometry captured when the arena is built, so changing the config while
    // a run is in progress can't desync the game logic from the blocks.
    private int builtFloorSize = 0;
    private int builtFloorCount = 0;

    public int getFloorSize() {
        return builtFloorSize > 0 ? builtFloorSize : getConfiguredFloorSize();
    }

    public int getFloorCount() {
        return builtFloorCount > 0 ? builtFloorCount : getConfiguredFloorCount();
    }

    private static int getConfiguredFloorSize() {
        return Math.max(8, Config.TNT_RUN_FLOOR_SIZE.get());
    }

    private static int getConfiguredFloorCount() {
        return Math.max(1, Math.min(3, Config.TNT_RUN_FLOOR_COUNT.get()));
    }

    public int[] getFloorYLevels() {
        int count = getFloorCount();
        int[] levels = new int[count];
        System.arraycopy(FLOOR_Y_LEVELS_ALL, 0, levels, 0, count);
        return levels;
    }

    public int getHalf() { return getFloorSize() / 2; }

    /**
     * Radius of floor the player can actually stand on. The outermost ring of
     * each floor sits inside the perimeter wall.
     */
    public int getWalkableRadius() { return getHalf() - 2; }

    public int getLowestFloorY() {
        int[] floorYs = getFloorYLevels();
        return floorYs[floorYs.length - 1];
    }

    public boolean isInCircle(int x, int z, int radius) {
        double dx = x - CENTER_X;
        double dz = z - CENTER_Z;
        return dx * dx + dz * dz <= radius * radius;
    }

    public BlockPos getStartPosition() {
        return new BlockPos(CENTER_X, FLOOR_1_Y + 1, CENTER_Z);
    }

    public AABB getBounds() {
        return new AABB(
                CENTER_X - CLEAR_HALF, CLEAR_BOTTOM_Y - 1, CENTER_Z - CLEAR_HALF,
                CENTER_X + CLEAR_HALF + 1, CLEAR_TOP_Y + 1, CENTER_Z + CLEAR_HALF + 1);
    }

    public void buildArena(ServerLevel level) {
        builtFloorSize = getConfiguredFloorSize();
        builtFloorCount = getConfiguredFloorCount();
        CreatiIntegration.LOGGER.info("Building TNT Run arena ({}x{}, {} floors)...",
                getFloorSize(), getFloorSize(), getFloorCount());
        build(new LevelCanvas(level), builtFloorSize, builtFloorCount);
        ArenaLabels.replace(level, getBounds(), getLabels());
        CreatiIntegration.LOGGER.info("TNT Run arena built!");
    }

    /** Places every block of the arena. Separate from the level so tests can build it. */
    public static void build(ArenaCanvas canvas, int floorSize, int floorCount) {
        int half = floorSize / 2;

        canvas.clear(CENTER_X - CLEAR_HALF, CLEAR_BOTTOM_Y, CENTER_Z - CLEAR_HALF,
                CENTER_X + CLEAR_HALF, CLEAR_TOP_Y, CENTER_Z + CLEAR_HALF);

        int[] floorYs = new int[floorCount];
        System.arraycopy(FLOOR_Y_LEVELS_ALL, 0, floorYs, 0, floorCount);

        for (int x = CENTER_X - half - 1; x <= CENTER_X + half + 1; x++) {
            for (int z = CENTER_Z - half - 1; z <= CENTER_Z + half + 1; z++) {
                int dx = x - CENTER_X;
                int dz = z - CENTER_Z;
                double distance = Math.sqrt(dx * dx + dz * dz);

                if (ArenaDome.isWallRing(x, z, CENTER_X, CENTER_Z, half)) {
                    for (int y = PIT_Y + 1; y <= WALL_TOP_Y; y++) {
                        canvas.set(x, y, z, wallBlock(dx, dz, y, floorYs));
                    }
                    continue;
                }
                if (distance > half) continue;

                // Glowing magma far below the last floor sells the drop. Players
                // are pulled out a few blocks under the last floor, well before it.
                canvas.set(x, PIT_Y, z, Blocks.MAGMA_BLOCK);

                for (int f = 0; f < floorYs.length; f++) {
                    Block[] palette = FLOOR_PALETTES[f % FLOOR_PALETTES.length];
                    canvas.set(x, floorYs[f], z, palette[Math.floorMod(x + z, 2)]);
                }
            }
        }

        ArenaDome.build(canvas, CENTER_X, CENTER_Z, WALL_TOP_Y + 1, half,
                Blocks.GLASS.defaultBlockState(),
                Blocks.DEEPSLATE_TILES.defaultBlockState(),
                Blocks.OCHRE_FROGLIGHT.defaultBlockState());
    }

    private static Block wallBlock(int dx, int dz, int y, int[] floorYs) {
        for (int floorY : floorYs) {
            if (y == floorY) {
                // A trim line marks where each floor meets the wall.
                return Blocks.POLISHED_DEEPSLATE;
            }
            if (y == floorY + 1) {
                // Hazard stripes at ankle height on every floor.
                double angle = Math.atan2(dz, dx) / (Math.PI * 2) + 0.5;
                int stripe = (int) Math.floor(angle * HAZARD_STRIPES);
                return stripe % 2 == 0 ? Blocks.YELLOW_CONCRETE : Blocks.BLACK_CONCRETE;
            }
        }
        if (dx == 0 || dz == 0) {
            // Cardinal columns of (inert) TNT give the pit its theme.
            return Blocks.TNT;
        }
        if (Math.abs(dx) == Math.abs(dz)) {
            return Blocks.OCHRE_FROGLIGHT;
        }
        return Blocks.DEEPSLATE_TILES;
    }

    private static List<ArenaLabel> getLabels() {
        double x = CENTER_X + 0.5;
        double z = CENTER_Z + 0.5;
        return List.of(
                new ArenaLabel(x, FLOOR_1_Y + 5.5, z,
                        Component.literal("§c§lTNT RUN"), 2.5F),
                new ArenaLabel(x, FLOOR_1_Y + 4.6, z,
                        Component.literal("§7Don't stop moving!"), 1.2F));
    }
}
