package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class TntRunArena {

    private final List<ArmorStand> floatingTextStands = new ArrayList<>();

    private static final int CENTER_X = 200;
    private static final int CENTER_Z = 0;

    // Fixed floor Y levels (top, middle, bottom) — up to 3 floors. The number
    // actually used is driven by Config.TNT_RUN_FLOOR_COUNT.
    public static final int FLOOR_1_Y = 64;
    public static final int FLOOR_2_Y = 59;
    public static final int FLOOR_3_Y = 54;
    public static final int[] FLOOR_Y_LEVELS_ALL = {FLOOR_1_Y, FLOOR_2_Y, FLOOR_3_Y};

    // Per-floor color (yellow/gray/orange), indexed by floor position.
    private static final net.minecraft.world.level.block.Block[] FLOOR_BLOCKS = {
            Blocks.YELLOW_CONCRETE,
            Blocks.GRAY_CONCRETE,
            Blocks.ORANGE_CONCRETE,
    };

    // Clear box is deliberately larger than any possible arena to flush stale
    // geometry from previous builds (e.g. old 32x32 arena, old 3rd floor at
    // Y=54). This prevents the "floor extends past walls" and "old floor
    // doesn't decay" issues caused by a stale minigame dimension.
    private static final int CLEAR_HALF = 20;
    private static final int CLEAR_BOTTOM_Y = 49;
    private static final int CLEAR_TOP_Y = 92; // covers the largest configurable dome roof

    public static final int[] FLOOR_Y_LEVELS = FLOOR_Y_LEVELS_ALL;

    public int getFloorSize() {
        return Math.max(8, Config.TNT_RUN_FLOOR_SIZE.get());
    }

    public int getFloorCount() {
        return Math.max(1, Math.min(3, Config.TNT_RUN_FLOOR_COUNT.get()));
    }

    public int[] getFloorYLevels() {
        int count = getFloorCount();
        int[] levels = new int[count];
        for (int i = 0; i < count; i++) {
            levels[i] = FLOOR_Y_LEVELS_ALL[i];
        }
        return levels;
    }

    public int getHalf() { return getFloorSize() / 2; }
    public int getMinX() { return CENTER_X - getHalf(); }
    public int getMaxX() { return CENTER_X + getHalf(); }
    public int getMinZ() { return CENTER_Z - getHalf(); }
    public int getMaxZ() { return CENTER_Z + getHalf(); }

    public boolean isInCircle(int x, int z, int radius) {
        double dx = x - CENTER_X;
        double dz = z - CENTER_Z;
        return dx * dx + dz * dz <= radius * radius;
    }

    public boolean isWallRing(int x, int z) {
        return ArenaDome.isWallRing(x, z, CENTER_X, CENTER_Z, getHalf());
    }

    public BlockPos getStartPosition() {
        return new BlockPos(CENTER_X, FLOOR_1_Y + 1, CENTER_Z);
    }

    public int getCenterX() { return CENTER_X; }
    public int getCenterZ() { return CENTER_Z; }

    public AABB getBounds() {
        return new AABB(
                CENTER_X - CLEAR_HALF, CLEAR_BOTTOM_Y - 1, CENTER_Z - CLEAR_HALF,
                CENTER_X + CLEAR_HALF + 1, CLEAR_TOP_Y + 1, CENTER_Z + CLEAR_HALF + 1);
    }

    private void clearFloatingText(ServerLevel level) {
        for (ArmorStand stand : floatingTextStands) {
            stand.discard();
        }
        floatingTextStands.clear();
        // Also sweep for stray stands from old builds.
        AABB box = new AABB(
                CENTER_X - CLEAR_HALF, CLEAR_BOTTOM_Y - 1, CENTER_Z - CLEAR_HALF,
                CENTER_X + CLEAR_HALF, CLEAR_TOP_Y + 4, CENTER_Z + CLEAR_HALF);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, box)) {
            stand.discard();
        }
    }

    public void buildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Building TNT Run arena ({}x{}, {} floors)...",
                getFloorSize(), getFloorSize(), getFloorCount());

        clearFloatingText(level);

        int minX = getMinX(), maxX = getMaxX();
        int minZ = getMinZ(), maxZ = getMaxZ();
        int[] floorYs = getFloorYLevels();

        // 1. Clear a large bounding box that covers any stale geometry from
        //    previous builds (old larger arenas, old 3rd floor, etc.).
        for (int x = CENTER_X - CLEAR_HALF; x <= CENTER_X + CLEAR_HALF; x++) {
            for (int z = CENTER_Z - CLEAR_HALF; z <= CENTER_Z + CLEAR_HALF; z++) {
                for (int y = CLEAR_BOTTOM_Y; y <= CLEAR_TOP_Y; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // 2. Build each floor as a circular disk, plus a circular wall ring
        //    that extends all the way up to the floor above (so there are no
        //    gaps between floors — the player can't see through or slip
        //    between wall sections). The top floor's walls extend 8 blocks up
        //    to give headroom below the dome roof.
        int half = getHalf();
        int wallOuter = half + 1;
        for (int f = 0; f < floorYs.length; f++) {
            int floorY = floorYs[f];
            net.minecraft.world.level.block.Block floorBlock = FLOOR_BLOCKS[f % FLOOR_BLOCKS.length];

            // Floor sheet — circular disk.
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!isInCircle(x, z, half)) continue;
                    level.setBlock(new BlockPos(x, floorY, z), floorBlock.defaultBlockState(), 2);
                }
            }

            // Wall height: extend up to the floor above, or 8 blocks if top
            // (taller headroom for the top floor so the dome roof isn't right
            // above the player's head).
            int wallTopY;
            if (f > 0) {
                wallTopY = floorYs[f - 1] - 1;
            } else {
                wallTopY = floorY + 8;
            }

            // Circular perimeter walls (ring at radius half to half+1).
            for (int x = CENTER_X - wallOuter; x <= CENTER_X + wallOuter; x++) {
                for (int z = CENTER_Z - wallOuter; z <= CENTER_Z + wallOuter; z++) {
                    if (!isWallRing(x, z)) continue;
                    for (int y = floorY + 1; y <= wallTopY; y++) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                    }
                }
            }

            // Enclose the top floor with a hollow dome roof so the player
            // can't see the sky. The dome is a half-sphere shell flush with
            // the walls. Primarily glass with stone-brick arches along the
            // axes ("+" from above) and lit redstone lamps embedded for a
            // warm glow.
            if (f == 0) {
                ArenaDome.build(level, CENTER_X, CENTER_Z, wallTopY + 1, half,
                        Blocks.GLASS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState());
            }
        }

        // 3. Floating labels above the top floor (below the dome).
        spawnFloatingText(level, new BlockPos(CENTER_X, FLOOR_1_Y + 6, CENTER_Z),
                Component.literal("\u00A7c\u00A7lTNT Run").withStyle(style -> style.withBold(true)));
        spawnFloatingText(level, new BlockPos(CENTER_X, FLOOR_1_Y + 5, CENTER_Z),
                Component.literal("\u00A77Keep moving!").withStyle(style -> style.withBold(false)));

        CreatiIntegration.LOGGER.info("TNT Run arena built!");
    }

    public void rebuildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Rebuilding TNT Run arena...");
        buildArena(level);
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
