package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class DropperArena {

    private static final int CENTER_X = 200;
    private static final int CENTER_Z = 200;
    private static final int INNER_RADIUS = 5;
    private static final int OUTER_RADIUS = 7;
    private static final int TOP_Y = 140;
    private static final int BOTTOM_Y = 55;
    private static final int FLOOR_Y = 55;
    private static final int WATER_Y = 55;

    private static final int HOLE_OFFSET_X = 3;
    private static final int HOLE_OFFSET_Z = 0;

    private static final Block[] GRADIENT_BLOCKS = {
            Blocks.WHITE_CONCRETE,
            Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.CYAN_CONCRETE,
            Blocks.BLUE_CONCRETE,
            Blocks.PURPLE_CONCRETE,
            Blocks.MAGENTA_CONCRETE,
            Blocks.PINK_CONCRETE,
            Blocks.RED_CONCRETE,
            Blocks.ORANGE_CONCRETE,
            Blocks.YELLOW_CONCRETE,
            Blocks.LIME_CONCRETE,
            Blocks.GREEN_CONCRETE,
            Blocks.GREEN_CONCRETE,
            Blocks.GRAY_CONCRETE,
            Blocks.GRAY_CONCRETE,
            Blocks.GRAY_CONCRETE,
    };

    public BlockPos getStartPosition() {
        return new BlockPos(CENTER_X - HOLE_OFFSET_X, TOP_Y + 1, CENTER_Z);
    }

    public float getStartYaw() {
        return -90f;
    }

    public BlockPos getHoleCenter() {
        return new BlockPos(CENTER_X + HOLE_OFFSET_X, TOP_Y, CENTER_Z + HOLE_OFFSET_Z);
    }

    public BlockPos getWaterCenter() {
        return new BlockPos(CENTER_X - HOLE_OFFSET_X, WATER_Y, CENTER_Z);
    }

    public int getWaterY() { return WATER_Y; }
    public int getFloorY() { return FLOOR_Y; }

    private Block getGradientBlock(int y) {
        int totalHeight = TOP_Y - BOTTOM_Y;
        int index = (int) ((double) (TOP_Y - y) / totalHeight * GRADIENT_BLOCKS.length);
        index = Math.max(0, Math.min(GRADIENT_BLOCKS.length - 1, index));
        return GRADIENT_BLOCKS[index];
    }

    private boolean isInCircle(int x, int z, int radius) {
        double dx = x - CENTER_X;
        double dz = z - CENTER_Z;
        return dx * dx + dz * dz <= radius * radius;
    }

    private boolean isWallAt(int x, int z) {
        double dx = x - CENTER_X;
        double dz = z - CENTER_Z;
        double distSq = dx * dx + dz * dz;
        double innerSq = INNER_RADIUS * INNER_RADIUS;
        double outerSq = OUTER_RADIUS * OUTER_RADIUS;
        return distSq >= innerSq && distSq <= outerSq;
    }

    private boolean isInnerEdge(int x, int z) {
        double dx = x - CENTER_X;
        double dz = z - CENTER_Z;
        double distSq = dx * dx + dz * dz;
        double innerSq = (INNER_RADIUS - 1) * (INNER_RADIUS - 1);
        double outerSq = INNER_RADIUS * INNER_RADIUS;
        return distSq >= innerSq && distSq <= outerSq + 2;
    }

    private boolean isHoleBlock(int x, int z) {
        BlockPos holeCenter = getHoleCenter();
        int dx = Math.abs(x - holeCenter.getX());
        int dz = Math.abs(z - holeCenter.getZ());
        return dx <= 1 && dz <= 1;
    }

    private boolean isWaterBlock(int x, int z) {
        BlockPos waterCenter = getWaterCenter();
        int dx = Math.abs(x - waterCenter.getX());
        int dz = Math.abs(z - waterCenter.getZ());
        return dx <= 1 && dz <= 1;
    }

    public void buildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Building Dropper arena...");

        int clearMin = OUTER_RADIUS + 2;
        for (int x = CENTER_X - clearMin; x <= CENTER_X + clearMin; x++) {
            for (int z = CENTER_Z - clearMin; z <= CENTER_Z + clearMin; z++) {
                for (int y = BOTTOM_Y - 3; y <= TOP_Y + 3; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        for (int y = BOTTOM_Y; y <= TOP_Y; y++) {
            for (int x = CENTER_X - OUTER_RADIUS; x <= CENTER_X + OUTER_RADIUS; x++) {
                for (int z = CENTER_Z - OUTER_RADIUS; z <= CENTER_Z + OUTER_RADIUS; z++) {
                    if (!isInCircle(x, z, OUTER_RADIUS)) continue;

                    if (isWallAt(x, z)) {
                        Block wallBlock = getGradientBlock(y);
                        if ((TOP_Y - y) % 5 == 0 && isInnerEdge(x, z)) {
                            wallBlock = Blocks.GLOWSTONE;
                        }
                        level.setBlock(new BlockPos(x, y, z), wallBlock.defaultBlockState(), 2);
                    }
                }
            }
        }

        for (int x = CENTER_X - OUTER_RADIUS; x <= CENTER_X + OUTER_RADIUS; x++) {
            for (int z = CENTER_Z - OUTER_RADIUS; z <= CENTER_Z + OUTER_RADIUS; z++) {
                if (!isInCircle(x, z, OUTER_RADIUS)) continue;
                level.setBlock(new BlockPos(x, BOTTOM_Y - 1, z), Blocks.DEEPSLATE.defaultBlockState(), 2);
            }
        }

        for (int x = CENTER_X - OUTER_RADIUS; x <= CENTER_X + OUTER_RADIUS; x++) {
            for (int z = CENTER_Z - OUTER_RADIUS; z <= CENTER_Z + OUTER_RADIUS; z++) {
                if (!isInCircle(x, z, INNER_RADIUS)) continue;
                if (isWaterBlock(x, z)) continue;
                level.setBlock(new BlockPos(x, FLOOR_Y, z), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 2);
            }
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos waterPos = getWaterCenter().offset(dx, 0, dz);
                level.setBlock(waterPos, Blocks.WATER.defaultBlockState(), 2);
            }
        }

        for (int x = CENTER_X - OUTER_RADIUS; x <= CENTER_X + OUTER_RADIUS; x++) {
            for (int z = CENTER_Z - OUTER_RADIUS; z <= CENTER_Z + OUTER_RADIUS; z++) {
                if (!isInCircle(x, z, INNER_RADIUS)) continue;
                if (isHoleBlock(x, z)) continue;
                level.setBlock(new BlockPos(x, TOP_Y, z), Blocks.YELLOW_CONCRETE.defaultBlockState(), 2);
            }
        }

        BlockPos holeCenter = getHoleCenter();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(holeCenter.offset(dx, TOP_Y, dz), Blocks.AIR.defaultBlockState(), 2);
            }
        }

        spawnFloatingText(level, new BlockPos(CENTER_X, TOP_Y + 2, CENTER_Z),
                Component.literal("\u00A79\u00A7lDropper").withStyle(style -> style.withBold(true)));
        spawnFloatingText(level, new BlockPos(CENTER_X, TOP_Y + 1, CENTER_Z),
                Component.literal("\u00A77Land in the water!").withStyle(style -> style.withBold(false)));

        CreatiIntegration.LOGGER.info("Dropper arena built!");
    }

    public void rebuildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Rebuilding Dropper arena...");
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
    }
}