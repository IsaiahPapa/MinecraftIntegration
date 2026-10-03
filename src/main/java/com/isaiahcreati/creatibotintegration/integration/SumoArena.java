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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * A sandstone colosseum: a round fighting platform on a pedestal above a
 * moat, ringed by a low parapet and tiered stands, all under a striped
 * canvas roof (which also keeps sunlight off the undead mobs).
 */
public class SumoArena {

    public static final int CENTER_X = 300;
    public static final int CENTER_Z = 300;
    public static final int FLOOR_Y = 64;
    public static final int WATER_Y = 56;

    // The parapet sits this far beyond the platform edge, leaving an open gap
    // that anything knocked off falls through into the moat.
    public static final int WALL_INNER_RADIUS_OFFSET = 3;

    private static final int PARAPET_TOP_Y = FLOOR_Y + 2;
    private static final int STAND_ROWS = 5;
    private static final int COLONNADE_TOP_Y = PARAPET_TOP_Y + STAND_ROWS + 5;
    private static final double ROOF_SLOPE = 0.5;
    private static final int ROOF_STRIPES = 16;

    // Covers the largest configurable arena, including the stands and roof.
    private static final int CLEAR_HALF = 28;
    private static final int CLEAR_BOTTOM_Y = 49;
    private static final int CLEAR_TOP_Y = 96;

    private int builtRadius = 0;

    public BlockPos getStartPosition() {
        return new BlockPos(CENTER_X, FLOOR_Y + 1, CENTER_Z);
    }

    public int getRadius() {
        return builtRadius > 0 ? builtRadius : getConfiguredRadius();
    }

    private static int getConfiguredRadius() {
        return Config.SUMO_ARENA_RADIUS.get();
    }

    /** True once the arena has been built with the currently configured radius. */
    public boolean isBuiltForCurrentConfig() {
        return builtRadius == getConfiguredRadius();
    }

    /**
     * Anything that drops this far below the platform has been knocked off.
     * Checking here, rather than at the water, counts a knock-off as soon as
     * it's certain instead of after an eight-block fall.
     */
    public double getKnockoutY() { return FLOOR_Y - 2; }

    public AABB getBounds() {
        return new AABB(
                CENTER_X - CLEAR_HALF, CLEAR_BOTTOM_Y - 1, CENTER_Z - CLEAR_HALF,
                CENTER_X + CLEAR_HALF + 1, CLEAR_TOP_Y + 1, CENTER_Z + CLEAR_HALF + 1);
    }

    public void buildArena(ServerLevel level) {
        builtRadius = getConfiguredRadius();
        CreatiIntegration.LOGGER.info("Building Arena (platform radius {})...", builtRadius);
        build(new LevelCanvas(level), builtRadius);
        ArenaLabels.replace(level, getBounds(), getLabels());
        CreatiIntegration.LOGGER.info("Arena built!");
    }

    /** Places every block of the arena. Separate from the level so tests can build it. */
    public static void build(ArenaCanvas canvas, int platformRadius) {
        int wallRadius = platformRadius + WALL_INNER_RADIUS_OFFSET;
        double standsOuter = wallRadius + 0.75 + STAND_ROWS;
        double roofRadius = standsOuter + 1.5;
        int roofBaseY = COLONNADE_TOP_Y + 1;

        canvas.clear(CENTER_X - CLEAR_HALF, CLEAR_BOTTOM_Y, CENTER_Z - CLEAR_HALF,
                CENTER_X + CLEAR_HALF, CLEAR_TOP_Y, CENTER_Z + CLEAR_HALF);

        int extent = (int) Math.ceil(roofRadius) + 1;
        for (int x = CENTER_X - extent; x <= CENTER_X + extent; x++) {
            for (int z = CENTER_Z - extent; z <= CENTER_Z + extent; z++) {
                int dx = x - CENTER_X;
                int dz = z - CENTER_Z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                double angle = Math.atan2(dz, dx);

                if (distance <= roofRadius) {
                    buildRoof(canvas, x, z, distance, angle, roofRadius, roofBaseY);
                }

                if (ArenaDome.isWallRing(x, z, CENTER_X, CENTER_Z, wallRadius)) {
                    buildParapet(canvas, x, z, dx, dz);
                } else if (distance < wallRadius - 1) {
                    // Sandy moat floor under the whole inner ring.
                    canvas.set(x, WATER_Y - 1, z, Blocks.SAND);
                    canvas.set(x, WATER_Y, z, Blocks.WATER);
                    if (distance <= platformRadius) {
                        buildPlatformColumn(canvas, x, z, dx, dz, distance, platformRadius);
                    }
                } else if (distance <= standsOuter) {
                    buildStands(canvas, x, z, dx, dz, distance, wallRadius);
                } else if (distance <= standsOuter + 1.5) {
                    buildColonnade(canvas, x, z, angle);
                }
            }
        }
    }

    private static void buildPlatformColumn(ArenaCanvas canvas, int x, int z, int dx, int dz,
                                            double distance, int platformRadius) {
        // Central pedestal rising out of the moat.
        if (distance <= 2.5) {
            for (int y = WATER_Y; y < FLOOR_Y; y++) {
                canvas.set(x, y, z, y % 3 == 0 ? Blocks.CUT_SANDSTONE : Blocks.SMOOTH_SANDSTONE);
            }
        } else if (distance <= 4.0) {
            canvas.set(x, FLOOR_Y - 1, z, Blocks.CUT_SANDSTONE);
        }
        if (distance >= platformRadius - 1.0) {
            // A thicker red lip makes the edge read as the edge.
            canvas.set(x, FLOOR_Y - 1, z, Blocks.CUT_RED_SANDSTONE);
        }

        int floorLightRadius = Math.max(2, platformRadius / 2);
        boolean isFloorLight = (Math.abs(dx) == floorLightRadius && dz == 0)
                || (Math.abs(dz) == floorLightRadius && dx == 0);

        Block top;
        if (isFloorLight) {
            top = Blocks.SHROOMLIGHT;
        } else if (distance >= platformRadius - 1.0) {
            top = Blocks.SMOOTH_RED_SANDSTONE;
        } else if (distance < 1.5) {
            top = Blocks.CHISELED_SANDSTONE;
        } else if (((int) Math.floor(distance)) % 3 == 0) {
            top = Blocks.CUT_SANDSTONE;
        } else {
            top = Blocks.SMOOTH_SANDSTONE;
        }
        canvas.set(x, FLOOR_Y, z, top);
    }

    private static void buildParapet(ArenaCanvas canvas, int x, int z, int dx, int dz) {
        boolean anchor = dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz);
        for (int y = WATER_Y - 1; y <= PARAPET_TOP_Y; y++) {
            Block block;
            if (y == PARAPET_TOP_Y) {
                block = anchor ? Blocks.SHROOMLIGHT : Blocks.CUT_SANDSTONE;
            } else if (y == FLOOR_Y) {
                block = Blocks.CHISELED_SANDSTONE;
            } else {
                block = Blocks.SANDSTONE;
            }
            canvas.set(x, y, z, block);
        }
    }

    private static void buildStands(ArenaCanvas canvas, int x, int z, int dx, int dz,
                                    double distance, int wallRadius) {
        int row = Math.min(STAND_ROWS - 1, (int) Math.floor(distance - (wallRadius + 0.75)));
        int seatY = PARAPET_TOP_Y + 1 + row;
        for (int y = WATER_Y; y < seatY; y++) {
            canvas.set(x, y, z, Blocks.SANDSTONE);
        }
        // Alternate seat colors per row, with lit aisles on the compass lines.
        boolean aisle = dx == 0 || dz == 0;
        Block seat = aisle ? Blocks.CUT_SANDSTONE
                : row % 2 == 0 ? Blocks.SMOOTH_SANDSTONE : Blocks.RED_TERRACOTTA;
        canvas.set(x, seatY, z, seat);
        if (aisle && row == STAND_ROWS - 1) {
            canvas.set(x, seatY + 1, z, Blocks.LANTERN);
        }
    }

    private static void buildColonnade(ArenaCanvas canvas, int x, int z, double angle) {
        int baseY = PARAPET_TOP_Y + STAND_ROWS;
        for (int y = WATER_Y; y <= baseY; y++) {
            canvas.set(x, y, z, Blocks.SANDSTONE);
        }
        // Pillars every 30 degrees, open arches between them.
        double sector = (Math.toDegrees(angle) + 360) % 30;
        boolean pillar = sector < 4 || sector > 26;
        for (int y = baseY + 1; y <= COLONNADE_TOP_Y; y++) {
            if (pillar) {
                canvas.set(x, y, z, Blocks.SMOOTH_SANDSTONE);
            } else if (y == COLONNADE_TOP_Y) {
                canvas.set(x, y, z, Blocks.CUT_SANDSTONE);
            }
        }
    }

    private static void buildRoof(ArenaCanvas canvas, int x, int z, double distance, double angle,
                                  double roofRadius, int roofBaseY) {
        // One block per column is enough to shade the whole arena, and the
        // gentle slope keeps neighboring columns within a block of each other.
        int roofY = roofBaseY + (int) Math.round((roofRadius - distance) * ROOF_SLOPE);
        int stripe = (int) Math.floor((angle / (Math.PI * 2) + 0.5) * ROOF_STRIPES);
        canvas.set(x, roofY, z, stripe % 2 == 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL);

        // A ring of lanterns hangs over the platform.
        if (Math.abs(distance - roofRadius * 0.45) < 0.5 && (Math.round(Math.toDegrees(angle)) + 360) % 45 < 8) {
            canvas.set(x, roofY - 1, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
    }

    public void clearMobs(ServerLevel level) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, getBounds())) {
            mob.discard();
        }
    }

    private static List<ArenaLabel> getLabels() {
        double x = CENTER_X + 0.5;
        double z = CENTER_Z + 0.5;
        return List.of(
                new ArenaLabel(x, FLOOR_Y + 6.0, z, Component.literal("§b§lARENA"), 2.5F),
                new ArenaLabel(x, FLOOR_Y + 5.1, z, Component.literal("§7Knock them into the water!"), 1.2F));
    }
}
