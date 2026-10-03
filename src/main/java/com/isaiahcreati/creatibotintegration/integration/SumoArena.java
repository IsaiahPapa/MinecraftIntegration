package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

    public int getWallRadius() {
        return getRadius() + WALL_INNER_RADIUS_OFFSET;
    }


    public AABB getBounds() {
        return new AABB(
                CENTER_X - CLEAR_HALF, CLEAR_BOTTOM_Y - 1, CENTER_Z - CLEAR_HALF,
                CENTER_X + CLEAR_HALF + 1, CLEAR_TOP_Y + 1, CENTER_Z + CLEAR_HALF + 1);
    }

    private boolean isInCircle(int x, int z, int radius) {
        double dx = x - CENTER_X;
        double dz = z - CENTER_Z;
        return dx * dx + dz * dz <= radius * radius;
    }

    private boolean isWallRing(int x, int z, int radius) {
        return ArenaDome.isWallRing(x, z, CENTER_X, CENTER_Z, radius);
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
        builtRadius = getConfiguredRadius();
        CreatiIntegration.LOGGER.info("Building Arena (platform radius {}, wall radius {})...", getRadius(), getWallRadius());

        clearFloatingText(level);

        int platformRadius = getRadius();
        int wallRadius = getWallRadius();
        int waterRadius = wallRadius + 2;
        BlockState litLamp = Blocks.REDSTONE_LAMP.defaultBlockState()
                .setValue(BlockStateProperties.LIT, true);

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
                    int dx = x - CENTER_X;
                    int dz = z - CENTER_Z;
                    boolean onLightAnchor = dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz);
                    boolean onLightRow = y == FLOOR_Y + 2 || y == FLOOR_Y + 6;
                    BlockState wallState = onLightAnchor && onLightRow
                            ? litLamp
                            : Blocks.STONE_BRICKS.defaultBlockState();
                    level.setBlock(new BlockPos(x, y, z), wallState, 2);
                }
            }
        }

        // 4. Floating platform — single layer, centered. The gap between the
        //    platform edge and the inner wall is open so things fall through.
        for (int x = CENTER_X - platformRadius; x <= CENTER_X + platformRadius; x++) {
            for (int z = CENTER_Z - platformRadius; z <= CENTER_Z + platformRadius; z++) {
                if (!isInCircle(x, z, platformRadius)) continue;
                double dx = x - CENTER_X;
                double dz = z - CENTER_Z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                int floorLightRadius = Math.max(2, platformRadius / 2);
                boolean isFloorLight = (Math.abs(dx) == floorLightRadius && dz == 0)
                        || (Math.abs(dz) == floorLightRadius && dx == 0);
                if (isFloorLight) {
                    level.setBlock(new BlockPos(x, FLOOR_Y, z), litLamp, 2);
                    continue;
                }
                Block platformBlock;
                if (distance >= platformRadius - 1.0) {
                    platformBlock = Blocks.POLISHED_ANDESITE;
                } else if (x == CENTER_X && z == CENTER_Z) {
                    platformBlock = Blocks.CHISELED_STONE_BRICKS;
                } else {
                    platformBlock = Blocks.SMOOTH_STONE;
                }
                level.setBlock(new BlockPos(x, FLOOR_Y, z), platformBlock.defaultBlockState(), 2);
            }
        }

        // 5. Hollow stone-brick dome roof so the arena is fully enclosed and
        //    sunlight can't get in (undead won't burn). Dome sits on top of the
        //    outer walls.
        ArenaDome.build(level, CENTER_X, CENTER_Z, wallTopY + 1, wallRadius,
                Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState());

        // 6. Floating labels.
        spawnFloatingText(level, new BlockPos(CENTER_X, FLOOR_Y + 6, CENTER_Z),
                Component.literal("\u00A7b\u00A7lArena").withStyle(s -> s.withBold(true)));
        spawnFloatingText(level, new BlockPos(CENTER_X, FLOOR_Y + 5, CENTER_Z),
                Component.literal("\u00A77Knock them off!").withStyle(s -> s.withBold(false)));

        CreatiIntegration.LOGGER.info("Arena built!");
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
