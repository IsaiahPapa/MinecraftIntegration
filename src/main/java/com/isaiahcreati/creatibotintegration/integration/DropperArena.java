package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class DropperArena {

    private final List<ArmorStand> floatingTextStands = new ArrayList<>();

    private static final int CENTER_X = 200;
    private static final int CENTER_Z = 200;
    private static final int INNER_RADIUS = 7;
    private static final int OUTER_RADIUS = 8;
    private static final int TOP_Y = 140;
    private static final int BOTTOM_Y = 55;
    private static final int FLOOR_Y = 55;
    private static final int WATER_Y = 55;
    private static final int WALL_EXTEND_ABOVE_TOP = 3; // headroom above the standing block before the dome

    // The top of the shaft is fully open. A single floating block sits at the
    // center for the player to stand on, look around, and jump off when ready.
    // This avoids the "instant fall on load" problem and accommodates slower
    // computers / fog render distances.

    // The water target is offset from the center so a straight drop misses it.
    // The player must steer laterally during the fall to land in the water.
    private static final int WATER_OFFSET_X = -5;
    private static final int WATER_OFFSET_Z = 0;

    // Clean 16-entry gradient (top -> bottom), no duplicates.
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
            Blocks.LIGHT_GRAY_CONCRETE,
            Blocks.GRAY_CONCRETE,
            Blocks.BLACK_CONCRETE,
            Blocks.OBSIDIAN,
    };

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

    public BlockPos getWaterCenter() {
        return new BlockPos(CENTER_X + WATER_OFFSET_X, WATER_Y, CENTER_Z + WATER_OFFSET_Z);
    }

    public int getWaterSize() {
        return Math.max(1, Config.DROPPER_WATER_SIZE.get());
    }


    public AABB getBounds() {
        int clearRadius = OUTER_RADIUS + 2;
        int clearTopY = TOP_Y + WALL_EXTEND_ABOVE_TOP + OUTER_RADIUS + 2;
        return new AABB(
                CENTER_X - clearRadius, BOTTOM_Y - 3, CENTER_Z - clearRadius,
                CENTER_X + clearRadius + 1, clearTopY + 1, CENTER_Z + clearRadius + 1);
    }

    /**
     * The pool's footprint, extended a couple of blocks up. A player whose
     * hitbox is over any part of the water when they come down has landed it,
     * even if their feet catch the rim beside it.
     */
    public AABB getWaterTargetBounds() {
        BlockPos center = getWaterCenter();
        int size = getWaterSize();
        return new AABB(
                center.getX(), WATER_Y, center.getZ(),
                center.getX() + size, WATER_Y + 2, center.getZ() + size);
    }

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
        return ArenaDome.isWallRing(x, z, CENTER_X, CENTER_Z, OUTER_RADIUS);
    }

    private boolean isInnerEdge(int x, int z) {
        double dx = x - CENTER_X;
        double dz = z - CENTER_Z;
        double distSq = dx * dx + dz * dz;
        double innerSq = (INNER_RADIUS - 1) * (INNER_RADIUS - 1);
        double outerSq = INNER_RADIUS * INNER_RADIUS;
        return distSq >= innerSq && distSq <= outerSq + 2;
    }

    private boolean isWaterBlock(int x, int z) {
        BlockPos waterCenter = getWaterCenter();
        int size = getWaterSize();
        // Square pad from waterCenter spanning [0, size-1] in x and z.
        int dx = x - waterCenter.getX();
        int dz = z - waterCenter.getZ();
        return dx >= 0 && dx < size && dz >= 0 && dz < size;
    }

    private boolean isWaterBorder(int x, int z) {
        BlockPos waterCenter = getWaterCenter();
        int size = getWaterSize();
        int dx = x - waterCenter.getX();
        int dz = z - waterCenter.getZ();
        return dx >= -1 && dx <= size && dz >= -1 && dz <= size && !isWaterBlock(x, z);
    }

    private int getTerrainRise(int x, int z) {
        // The pool is framed by glowstone flush with the water, so it reads as
        // the target from 85 blocks up without a lip for players to land on.
        if (isWaterBorder(x, z)) return 0;
        int dx = x - CENTER_X;
        int dz = z - CENTER_Z;
        int noise = Math.floorMod(dx * 37 + dz * 57 + dx * dz * 11, 29);
        return noise == 0 ? 3 : noise <= 4 ? 2 : noise <= 13 ? 1 : 0;
    }

    private Block getTerrainBlock(int x, int z, int dy, int rise) {
        if (dy == rise && isWaterBorder(x, z)) {
            return Blocks.GLOWSTONE;
        }
        int variant = Math.floorMod((x - CENTER_X) * 17 + (z - CENTER_Z) * 31 + dy * 7, 10);
        if (dy == rise) {
            // Deliberately no blue/cyan tops (prismarine, cyan terracotta):
            // from the top of the shaft they were indistinguishable from water.
            return switch (variant) {
                case 0, 1, 2 -> Blocks.MOSS_BLOCK;
                case 3, 4 -> Blocks.COARSE_DIRT;
                case 5 -> Blocks.PACKED_MUD;
                default -> Blocks.MOSSY_COBBLESTONE;
            };
        }
        return variant % 3 == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS;
    }

    private void clearFloatingText(ServerLevel level) {
        // Remove previously-spawned label armor stands by reference so they
        // don't accumulate across rebuilds.
        for (ArmorStand stand : floatingTextStands) {
            stand.discard();
        }
        floatingTextStands.clear();
        // Also sweep for any stray stands from old builds / version migrations.
        int clearMin = OUTER_RADIUS + 2;
        int clearTopY = TOP_Y + OUTER_RADIUS + 4;
        AABB box = new AABB(
                CENTER_X - clearMin, BOTTOM_Y - 3, CENTER_Z - clearMin,
                CENTER_X + clearMin, clearTopY, CENTER_Z + clearMin);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, box)) {
            stand.discard();
        }
    }

    public void buildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Building Dropper arena...");
        builtWaterSize = getWaterSize();

        clearFloatingText(level);

        int clearMin = OUTER_RADIUS + 2;
        int clearTopY = TOP_Y + WALL_EXTEND_ABOVE_TOP + OUTER_RADIUS + 2;
        for (int x = CENTER_X - clearMin; x <= CENTER_X + clearMin; x++) {
            for (int z = CENTER_Z - clearMin; z <= CENTER_Z + clearMin; z++) {
                for (int y = BOTTOM_Y - 3; y <= clearTopY; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        for (int y = BOTTOM_Y; y <= TOP_Y + WALL_EXTEND_ABOVE_TOP; y++) {
            for (int x = CENTER_X - OUTER_RADIUS - 1; x <= CENTER_X + OUTER_RADIUS + 1; x++) {
                for (int z = CENTER_Z - OUTER_RADIUS - 1; z <= CENTER_Z + OUTER_RADIUS + 1; z++) {
                    if (isWallAt(x, z)) {
                        if (y > TOP_Y) {
                            // Extension above the gradient shaft — stone bricks
                            // to match the dome base.
                            level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                        } else {
                            Block wallBlock = getGradientBlock(y);
                            if ((TOP_Y - y) % 5 == 0 && isInnerEdge(x, z)) {
                                level.setBlock(new BlockPos(x, y, z),
                                        Blocks.SEA_LANTERN.defaultBlockState(), 2);
                                continue;
                            }
                            level.setBlock(new BlockPos(x, y, z), wallBlock.defaultBlockState(), 2);
                        }
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
                int rise = getTerrainRise(x, z);
                for (int dy = 0; dy <= rise; dy++) {
                    Block terrainBlock = getTerrainBlock(x, z, dy, rise);
                    level.setBlock(new BlockPos(x, FLOOR_Y + dy, z),
                            terrainBlock.defaultBlockState(), 2);
                }
            }
        }

        int waterSize = getWaterSize();
        BlockPos waterCenter = getWaterCenter();
        for (int dx = 0; dx < waterSize; dx++) {
            for (int dz = 0; dz < waterSize; dz++) {
                level.setBlock(waterCenter.offset(dx, 0, dz), Blocks.WATER.defaultBlockState(), 2);
            }
        }

        placeLaunchBlock(level);

        // Dome ceiling enclosing the top of the shaft so the player can't see
        // the sky. Built as a half-sphere from the outer radius curving upward,
        // with lit redstone lamps embedded for a warm glow.
        ArenaDome.build(level, CENTER_X, CENTER_Z,
                TOP_Y + WALL_EXTEND_ABOVE_TOP + 1, OUTER_RADIUS,
                Blocks.GLASS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState());

        // Floating labels in front of the player (who spawns facing -X), but
        // above eye level so they don't block the view down the shaft.
        spawnFloatingText(level, new BlockPos(CENTER_X - 3, TOP_Y + 3, CENTER_Z),
                Component.literal("\u00A79\u00A7lDropper").withStyle(style -> style.withBold(true)));
        spawnFloatingText(level, new BlockPos(CENTER_X - 3, TOP_Y + 2, CENTER_Z),
                Component.literal("\u00A77Land in the glowing pool!").withStyle(style -> style.withBold(false)));

        CreatiIntegration.LOGGER.info("Dropper arena built!");
    }

    /**
     * Single-block launch point: the player must keep their footing and
     * deliberately step off into the shaft. It crumbles if they wait too long,
     * so it is re-placed at the start of every run.
     */
    public void placeLaunchBlock(ServerLevel level) {
        level.setBlock(getLaunchBlockPos(), Blocks.YELLOW_CONCRETE.defaultBlockState(), 2);
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
