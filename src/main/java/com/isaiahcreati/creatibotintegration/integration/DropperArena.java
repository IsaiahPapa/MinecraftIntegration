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

    public BlockPos getStartPosition() {
        // Spawn on top of the single center floating block.
        return new BlockPos(CENTER_X, TOP_Y + 1, CENTER_Z);
    }

    public float getStartYaw() {
        // Face the water target (west / -X) so the player sees where to steer.
        return 90f;
    }

    public BlockPos getHoleCenter() {
        // Kept for compatibility; the "hole" is now just the open shaft center.
        return new BlockPos(CENTER_X, TOP_Y, CENTER_Z);
    }

    public BlockPos getWaterCenter() {
        return new BlockPos(CENTER_X + WATER_OFFSET_X, WATER_Y, CENTER_Z + WATER_OFFSET_Z);
    }

    public int getWaterSize() {
        return Math.max(1, Config.DROPPER_WATER_SIZE.get());
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
        int size = getWaterSize();
        // Square pad from waterCenter spanning [0, size-1] in x and z.
        int dx = x - waterCenter.getX();
        int dz = z - waterCenter.getZ();
        return dx >= 0 && dx < size && dz >= 0 && dz < size;
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

        clearFloatingText(level);

        int clearMin = OUTER_RADIUS + 2;
        int clearTopY = TOP_Y + OUTER_RADIUS + 3; // covers the dome ceiling
        for (int x = CENTER_X - clearMin; x <= CENTER_X + clearMin; x++) {
            for (int z = CENTER_Z - clearMin; z <= CENTER_Z + clearMin; z++) {
                for (int y = BOTTOM_Y - 3; y <= clearTopY; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        for (int y = BOTTOM_Y; y <= TOP_Y + WALL_EXTEND_ABOVE_TOP; y++) {
            for (int x = CENTER_X - OUTER_RADIUS; x <= CENTER_X + OUTER_RADIUS; x++) {
                for (int z = CENTER_Z - OUTER_RADIUS; z <= CENTER_Z + OUTER_RADIUS; z++) {
                    if (!isInCircle(x, z, OUTER_RADIUS)) continue;

                    if (isWallAt(x, z)) {
                        if (y > TOP_Y) {
                            // Extension above the gradient shaft — stone bricks
                            // to match the dome base.
                            level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                        } else {
                            Block wallBlock = getGradientBlock(y);
                            if ((TOP_Y - y) % 5 == 0 && isInnerEdge(x, z)) {
                                level.setBlock(new BlockPos(x, y, z),
                                        Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true), 2);
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
                level.setBlock(new BlockPos(x, FLOOR_Y, z), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 2);
            }
        }

        int waterSize = getWaterSize();
        BlockPos waterCenter = getWaterCenter();
        for (int dx = 0; dx < waterSize; dx++) {
            for (int dz = 0; dz < waterSize; dz++) {
                level.setBlock(waterCenter.offset(dx, 0, dz), Blocks.WATER.defaultBlockState(), 2);
            }
        }

        // Top of shaft: a single floating block at the center for the player to
        // stand on. The rest of the top is open — the player jumps off when
        // ready.
        level.setBlock(new BlockPos(CENTER_X, TOP_Y, CENTER_Z), Blocks.YELLOW_CONCRETE.defaultBlockState(), 2);

        // Dome ceiling enclosing the top of the shaft so the player can't see
        // the sky. Built as a half-sphere from the outer radius curving upward,
        // with lit redstone lamps embedded for a warm glow.
        buildDomeCeiling(level);

        // Floating labels offset to the side so the player isn't standing
        // inside them.
        spawnFloatingText(level, new BlockPos(CENTER_X + 3, TOP_Y + 2, CENTER_Z),
                Component.literal("\u00A79\u00A7lDropper").withStyle(style -> style.withBold(true)));
        spawnFloatingText(level, new BlockPos(CENTER_X + 3, TOP_Y + 1, CENTER_Z),
                Component.literal("\u00A77Land in the water!").withStyle(style -> style.withBold(false)));

        CreatiIntegration.LOGGER.info("Dropper arena built!");
    }

    /**
     * Builds a hollow dome ceiling above the top of the shaft. The dome base
     * (dy=0) is a full disk that sits flush on top of the extended walls,
     * fully closing the shaft. For dy>0 only the shell is placed so the player
     * has headroom inside an enclosed space. The shell is primarily glass with
     * stone-brick arches along the X and Z axes ("+" pattern from above), lit
     * redstone lamps embedded at intervals, and a solid glass cap at the top.
     */
    private void buildDomeCeiling(ServerLevel level) {
        int domeBaseY = TOP_Y + WALL_EXTEND_ABOVE_TOP + 1;
        int domeRadius = OUTER_RADIUS;
        int domeHeight = OUTER_RADIUS; // half-sphere
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

                    // Near the peak, fill the disk solidly instead of just the
                    // shell ring so the dome doesn't have an open hole below the
                    // single top block.
                    boolean fillSolid = sliceRadius <= shellThickness + 2.5;
                    boolean inShell = Math.abs(dist - sliceRadius) <= shellThickness;

                    if (!fillSolid && !inShell) continue;

                    if (isLamp) {
                        level.setBlock(new BlockPos(x, y, z),
                                Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true), 2);
                    } else if (onAxis) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                    } else {
                        level.setBlock(new BlockPos(x, y, z), Blocks.GLASS.defaultBlockState(), 2);
                    }
                }
            }
        }
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
        floatingTextStands.add(armorStand);
    }
}
