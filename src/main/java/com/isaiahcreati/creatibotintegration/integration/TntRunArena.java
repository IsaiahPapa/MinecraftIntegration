package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;

public class TntRunArena {

    private static final int CENTER_X = 200;
    private static final int CENTER_Z = 0;
    private static final int ARENA_SIZE = 32;
    private static final int HALF = ARENA_SIZE / 2;

    public static final int FLOOR_1_Y = 64;
    public static final int FLOOR_2_Y = 59;
    public static final int FLOOR_3_Y = 54;
    public static final int[] FLOOR_Y_LEVELS = {FLOOR_1_Y, FLOOR_2_Y, FLOOR_3_Y};
    public static final int ARENA_BOTTOM_Y = 53;
    public static final int ARENA_TOP_Y = 65;

    public BlockPos getStartPosition() {
        return new BlockPos(CENTER_X, FLOOR_1_Y + 1, CENTER_Z);
    }

    public int getCenterX() { return CENTER_X; }
    public int getCenterZ() { return CENTER_Z; }
    public int getHalf() { return HALF; }
    public int getMinX() { return CENTER_X - HALF; }
    public int getMaxX() { return CENTER_X + HALF; }
    public int getMinZ() { return CENTER_Z - HALF; }
    public int getMaxZ() { return CENTER_Z + HALF; }

    public void buildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Building TNT Run arena...");

        for (int x = getMinX(); x <= getMaxX(); x++) {
            for (int z = getMinZ(); z <= getMaxZ(); z++) {
                for (int y = ARENA_BOTTOM_Y; y <= ARENA_TOP_Y; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        for (int x = getMinX(); x <= getMaxX(); x++) {
            for (int z = getMinZ(); z <= getMaxZ(); z++) {
                level.setBlock(new BlockPos(x, FLOOR_1_Y, z), Blocks.YELLOW_CONCRETE.defaultBlockState(), 2);
                level.setBlock(new BlockPos(x, FLOOR_2_Y, z), Blocks.GRAY_CONCRETE.defaultBlockState(), 2);
                level.setBlock(new BlockPos(x, FLOOR_3_Y, z), Blocks.ORANGE_CONCRETE.defaultBlockState(), 2);
            }
        }

        spawnFloatingText(level, new BlockPos(CENTER_X, FLOOR_1_Y + 3, CENTER_Z),
                Component.literal("\u00A7c\u00A7lTNT Run").withStyle(style -> style.withBold(true)));
        spawnFloatingText(level, new BlockPos(CENTER_X, FLOOR_1_Y + 2, CENTER_Z),
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
    }
}