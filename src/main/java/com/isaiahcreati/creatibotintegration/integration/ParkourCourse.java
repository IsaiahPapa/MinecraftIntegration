package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class ParkourCourse {

    private final List<ArmorStand> floatingTextStands = new ArrayList<>();

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
        clearFloatingText(level);

        // 1. Start platform (3x3 gold)
        buildPlatform(level, 0, 64, 0, 3, Blocks.GOLD_BLOCK);

        // 2. Flat warmup gap (1x1 stone)
        buildPlatform(level, 0, 64, 3, 1, Blocks.STONE);

        // 3. Lateral movement (1x1 stone)
        buildPlatform(level, 1, 64, 6, 1, Blocks.STONE);

        // 4. Step up +1 (1x1 andesite)
        buildPlatform(level, 0, 65, 9, 1, Blocks.POLISHED_ANDESITE);

        // 5. Slime block bounce (1x1) — landing on slime bounces the player;
        //    they must control the bounce to reach the next platform.
        buildPlatform(level, 0, 65, 13, 1, Blocks.SLIME_BLOCK);

        // 6. Landing after slime bounce (1x1 stone)
        buildPlatform(level, 0, 65, 16, 1, Blocks.STONE);

        // 7. Packed ice slide platform (3x3) — the player must control their
        //    momentum to avoid sliding off the edge.
        buildPlatform(level, 0, 65, 19, 3, Blocks.PACKED_ICE);

        // 8. Step up +1 from ice (1x1 cobblestone)
        buildPlatform(level, 0, 66, 22, 1, Blocks.COBBLESTONE);

        // 9. Ladder climb segment.
        //    Cobblestone pillar (support) with ladders on the -Z face.
        int ladderZ = 25;
        for (int dy = 1; dy <= 3; dy++) {
            setBlock(level, 0, 66 + dy, ladderZ, Blocks.COBBLESTONE);
            level.setBlockAndUpdate(new BlockPos(0, 66 + dy, ladderZ - 1),
                    Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        }
        // Top of the pillar — landing before the next jump.
        buildPlatform(level, 0, 70, 25, 1, Blocks.STONE_BRICKS);

        // 10. Jump from ladder top (1x1 granite)
        buildPlatform(level, 0, 70, 28, 1, Blocks.POLISHED_GRANITE);

        // 11. Step down -1 (1x1 smooth stone)
        buildPlatform(level, 0, 69, 31, 1, Blocks.SMOOTH_STONE);

        // 12. Slime block bounce landing (1x1) — the player bounces on landing,
        //     making the takeoff for the next jump require timing.
        buildPlatform(level, 0, 69, 34, 1, Blocks.SLIME_BLOCK);

        // 13. Jump from honey (1x1 stone) — harder because honey slows momentum.
        buildPlatform(level, 0, 69, 37, 1, Blocks.STONE);

        // 14. Water hazard gap (1x1 iron) — a 3-block gap over a water pit.
        //     Falling into the water resets the player to the start.
        buildPlatform(level, 0, 69, 40, 1, Blocks.IRON_BLOCK);
        // Water pit below the gap (visual hazard + early-reset trigger).
        for (int dx = -1; dx <= 1; dx++) {
            level.setBlockAndUpdate(new BlockPos(dx, 63, 38), Blocks.WATER.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(dx, 63, 39), Blocks.WATER.defaultBlockState());
        }

        // 15. End platform (3x3 diamond) with pressure plate — step down -3.
        buildPlatform(level, 0, 66, 42, 3, Blocks.DIAMOND_BLOCK);
        level.setBlockAndUpdate(new BlockPos(0, 67, 42), Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState());

        // Floating text markers
        spawnFloatingText(level, new BlockPos(0, 68, 0), Component.literal("\u00A7a\u00A7lStart").withStyle(style -> style.withBold(true)));
        spawnFloatingText(level, new BlockPos(0, 69, 42), Component.literal("\u00A7b\u00A7lFinish!").withStyle(style -> style.withBold(true)));
    }

    private void clearFloatingText(ServerLevel level) {
        for (ArmorStand stand : floatingTextStands) {
            stand.discard();
        }
        floatingTextStands.clear();
        AABB box = new AABB(-4, 62, -4, 4, 72, 46);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, box)) {
            stand.discard();
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

    private void setBlock(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlockAndUpdate(new BlockPos(x, y, z), block.defaultBlockState());
    }

    /**
     * Builds a size x size platform centered on (centerX, centerZ) at height y.
     * size=1 -> 1x1, size=2 -> 2x2, size=3 -> 3x3 (correctly centered).
     */
    private void buildPlatform(ServerLevel level, int centerX, int y, int centerZ, int size, Block block) {
        int offset = (size - 1) / 2;
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                level.setBlockAndUpdate(new BlockPos(centerX - offset + dx, y, centerZ - offset + dz), block.defaultBlockState());
            }
        }
    }
}
