package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class SumoArena {

    private final List<ArmorStand> floatingTextStands = new ArrayList<>();

    public static final int CENTER_X = 300;
    public static final int CENTER_Z = 300;
    public static final int FLOOR_Y = 64;
    public static final int WATER_Y = 56;

    // The outer wall radius is larger than the platform radius, creating a gap
    // between the platform edge and the wall so mobs can be knocked off into
    // the water below.
    public static final int WALL_INNER_RADIUS_OFFSET = 3;

    private static final int CLEAR_HALF = 24;
    private static final int CLEAR_BOTTOM_Y = 49;
    private static final int CLEAR_TOP_Y = 92;

    public BlockPos getStartPosition() {
        return new BlockPos(CENTER_X, FLOOR_Y + 1, CENTER_Z);
    }

    public int getRadius() {
        return Config.SUMO_ARENA_RADIUS.get();
    }

    public int getWallRadius() {
        return getRadius() + WALL_INNER_RADIUS_OFFSET;
    }

    public int getWaterY() { return WATER_Y; }

    private boolean isInCircle(int x, int z, int radius) {
        double dx = x - CENTER_X;
        double dz = z - CENTER_Z;
        return dx * dx + dz * dz <= radius * radius;
    }

    private boolean isWallRing(int x, int z, int radius) {
        double dx = x - CENTER_X;
        double dz = z - CENTER_Z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return dist >= radius - 0.5 && dist < radius + 0.5;
    }

    private void clearFloatingText(ServerLevel level) {
        for (ArmorStand stand : floatingTextStands) {
            if (stand.isAlive()) stand.discard();
        }
        floatingTextStands.clear();
        AABB box = new AABB(
                CENTER_X - CLEAR_HALF, CLEAR_BOTTOM_Y - 1, CENTER_Z - CLEAR_HALF,
                CENTER_X + CLEAR_HALF, CLEAR_TOP_Y + 4, CENTER_Z + CLEAR_HALF);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, box)) {
            stand.discard();
        }
    }

    public void buildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Building Arena (platform radius {}, wall radius {})...", getRadius(), getWallRadius());

        clearFloatingText(level);

        int platformRadius = getRadius();
        int wallRadius = getWallRadius();
        int waterRadius = wallRadius + 2;

        // 1. Clear a large bounding box covering stale geometry.
        for (int x = CENTER_X - CLEAR_HALF; x <= CENTER_X + CLEAR_HALF; x++) {
            for (int z = CENTER_Z - CLEAR_HALF; z <= CENTER_Z + CLEAR_HALF; z++) {
                for (int y = CLEAR_BOTTOM_Y; y <= CLEAR_TOP_Y; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // 2. Water pool at the bottom of the enclosed arena. Floor + water
        //    layer. The pool fills the entire interior footprint so anything
        //    knocked off the platform lands in water.
        for (int x = CENTER_X - waterRadius; x <= CENTER_X + waterRadius; x++) {
            for (int z = CENTER_Z - waterRadius; z <= CENTER_Z + waterRadius; z++) {
                double dx = x - CENTER_X;
                double dz = z - CENTER_Z;
                if (dx * dx + dz * dz > waterRadius * waterRadius) continue;
                level.setBlock(new BlockPos(x, WATER_Y - 1, z), Blocks.STONE.defaultBlockState(), 2);
                level.setBlock(new BlockPos(x, WATER_Y, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        // 3. Outer walls — fully enclose the arena from the water level up to
        //    the dome base. The platform sits INSIDE these walls with a gap
        //    (WALL_INNER_RADIUS_OFFSET blocks) so mobs knocked off the platform
        //    edge fall through the gap into the water.
        int wallTopY = FLOOR_Y + 8;
        for (int x = CENTER_X - wallRadius - 1; x <= CENTER_X + wallRadius + 1; x++) {
            for (int z = CENTER_Z - wallRadius - 1; z <= CENTER_Z + wallRadius + 1; z++) {
                if (!isWallRing(x, z, wallRadius)) continue;
                for (int y = WATER_Y; y <= wallTopY; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                }
            }
        }

        // 4. Floating platform — single layer, centered. The gap between the
        //    platform edge and the inner wall is open so things fall through.
        for (int x = CENTER_X - platformRadius; x <= CENTER_X + platformRadius; x++) {
            for (int z = CENTER_Z - platformRadius; z <= CENTER_Z + platformRadius; z++) {
                if (!isInCircle(x, z, platformRadius)) continue;
                level.setBlock(new BlockPos(x, FLOOR_Y, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
            }
        }

        // 5. Hollow stone-brick dome roof so the arena is fully enclosed and
        //    sunlight can't get in (undead won't burn). Dome sits on top of the
        //    outer walls.
        buildDomeRoof(level, wallTopY + 1, wallRadius);

        // 6. Floating labels.
        spawnFloatingText(level, new BlockPos(CENTER_X, FLOOR_Y + 6, CENTER_Z),
                Component.literal("\u00A7b\u00A7lArena").withStyle(s -> s.withBold(true)));
        spawnFloatingText(level, new BlockPos(CENTER_X, FLOOR_Y + 5, CENTER_Z),
                Component.literal("\u00A77Knock them off!").withStyle(s -> s.withBold(false)));

        CreatiIntegration.LOGGER.info("Arena built!");
    }

    private void buildDomeRoof(ServerLevel level, int domeBaseY, int domeRadius) {
        int domeHeight = domeRadius;
        double shellThickness = 1.2;

        for (int dy = 0; dy <= domeHeight; dy++) {
            double t = (double) dy / domeHeight;
            double sliceRadius = domeRadius * Math.sqrt(Math.max(0, 1 - t * t));
            int r = (int) Math.ceil(sliceRadius);

            int y = domeBaseY + dy;
            for (int x = CENTER_X - r; x <= CENTER_X + r; x++) {
                for (int z = CENTER_Z - r; z <= CENTER_Z + r; z++) {
                    double dx = x - CENTER_X;
                    double dz = z - CENTER_Z;
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    boolean onAxis = (x == CENTER_X || z == CENTER_Z);
                    boolean isLamp = ((x - CENTER_X) % 4 == 0) && ((z - CENTER_Z) % 4 == 0)
                            && (x != CENTER_X || z != CENTER_Z) && !onAxis;

                    // Near the peak, sliceRadius is small and the ring placement
                    // leaves the center hollow. Fill the whole disk solidly when
                    // the slice is tight enough that the shell ring wouldn't
                    // cover the center.
                    boolean fillSolid = sliceRadius <= shellThickness + 2.5;
                    boolean inShell = Math.abs(dist - sliceRadius) <= shellThickness;

                    if (!fillSolid && !inShell) continue;

                    if (isLamp) {
                        level.setBlock(new BlockPos(x, y, z),
                                Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true), 2);
                    } else if (onAxis) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    public void rebuildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Rebuilding Arena...");
        buildArena(level);
    }

    public void clearMobs(ServerLevel level) {
        int wallRadius = getWallRadius();
        AABB box = new AABB(
                CENTER_X - wallRadius - 4, WATER_Y, CENTER_Z - wallRadius - 4,
                CENTER_X + wallRadius + 4, FLOOR_Y + 8, CENTER_Z + wallRadius + 4);
        for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box)) {
            mob.discard();
        }
    }

    private void spawnFloatingText(ServerLevel level, BlockPos pos, Component text) {
        ArmorStand armorStand = new ArmorStand(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        armorStand.setCustomName(text);
        armorStand.setCustomNameVisible(true);
        armorStand.setInvisible(true);
        armorStand.setNoGravity(true);
        armorStand.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, (byte) (armorStand.getEntityData().get(ArmorStand.DATA_CLIENT_FLAGS) | 0x10));
        armorStand.setInvulnerable(true);
        armorStand.setSilent(true);
        level.addFreshEntity(armorStand);
        floatingTextStands.add(armorStand);
    }
}