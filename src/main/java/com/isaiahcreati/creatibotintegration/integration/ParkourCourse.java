package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;

public class ParkourCourse {

    private static final int START_X = 0;
    private static final int START_Y = 64;
    private static final int START_Z = 0;

    private static final int END_X = 0;
    private static final int END_Y = 66;
    private static final int END_Z = 42;

    private boolean courseBuilt = false;

    public BlockPos getStartPosition() {
        return new BlockPos(START_X, START_Y + 1, START_Z);
    }

    public BlockPos getEndPosition() {
        return new BlockPos(END_X, END_Y, END_Z);
    }

    public void buildIfNeeded(ServerLevel parkourLevel) {
        if (courseBuilt) return;
        CreatiIntegration.LOGGER.info("Building parkour course...");
        generateCourse(parkourLevel);
        courseBuilt = true;
        CreatiIntegration.LOGGER.info("Parkour course built!");
    }

    public void forceRebuild(ServerLevel parkourLevel) {
        courseBuilt = false;
        buildIfNeeded(parkourLevel);
    }

    private void generateCourse(ServerLevel level) {
        // Start platform (2x2 gold block)
        buildPlatform(level, 0, 64, 0, 2, Blocks.GOLD_BLOCK);

        // Flat section - easy warmup jumps (3 block gaps)
        buildPlatform(level, 0, 64, 3, 1, Blocks.STONE);
        buildPlatform(level, 2, 64, 6, 1, Blocks.STONE);

        // Slight elevation - step up 1 block (2 block gap for height)
        buildPlatform(level, 0, 65, 8, 2, Blocks.POLISHED_ANDESITE);

        // Lateral movement (3 block gap)
        buildPlatform(level, -2, 65, 11, 1, Blocks.COBBLESTONE);

        // Step up 1 block (2 block gap due to height)
        buildPlatform(level, 0, 66, 13, 2, Blocks.STONE_BRICKS);

        // Narrow landing (3 block gap)
        buildPlatform(level, 2, 66, 16, 1, Blocks.POLISHED_GRANITE);

        // Step down (3 block gap)
        buildPlatform(level, 1, 65, 19, 1, Blocks.SMOOTH_STONE);

        // Step back up (2 block gap with height)
        buildPlatform(level, 0, 66, 21, 2, Blocks.POLISHED_DIORITE);

        // Challenging 1x1 (3 block gap)
        buildPlatform(level, -1, 66, 24, 1, Blocks.STONE);

        // Step up to peak (2 block gap, 1 block height)
        buildPlatform(level, 0, 67, 26, 2, Blocks.STONE_BRICKS);

        // 1x1 at peak (3 block gap)
        buildPlatform(level, 1, 67, 29, 1, Blocks.COBBLESTONE);

        // Step down
        buildPlatform(level, 0, 66, 32, 1, Blocks.POLISHED_ANDESITE);

        // Final approach (3 block gap)
        buildPlatform(level, 0, 66, 35, 1, Blocks.SMOOTH_STONE);

        // Landing pad before end (2x2)
        buildPlatform(level, 0, 66, 38, 2, Blocks.IRON_BLOCK);

        // End platform (3x3 diamond block with pressure plate)
        buildPlatform(level, 0, 66, 42, 3, Blocks.DIAMOND_BLOCK);
        level.setBlockAndUpdate(new BlockPos(0, 67, 42), Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState());

        // Floating text markers
        spawnFloatingText(level, new BlockPos(0, 68, 0), Component.literal("\u00A7a\u00A7lStart").withStyle(style -> style.withBold(true)));
        spawnFloatingText(level, new BlockPos(0, 69, 42), Component.literal("\u00A7b\u00A7lFinish!").withStyle(style -> style.withBold(true)));
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

    private void buildPlatform(ServerLevel level, int centerX, int y, int centerZ, int size, net.minecraft.world.level.block.Block block) {
        int half = size / 2;
        for (int x = centerX - half; x <= centerX + half; x++) {
            for (int z = centerZ - half; z <= centerZ + half; z++) {
                level.setBlockAndUpdate(new BlockPos(x, y, z), block.defaultBlockState());
            }
        }
    }
}