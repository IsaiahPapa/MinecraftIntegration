package com.isaiahcreati.creatibotintegration.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Shared geometry for the enclosed minigame arena roofs. */
public final class ArenaDome {

    private static final double EDGE_PADDING = 0.75;

    private ArenaDome() {}

    /**
     * Builds a hollow, watertight half-dome. Each horizontal slice bridges to
     * the next slice instead of drawing an isolated ring. That overlap is what
     * keeps the steep upper curve sealed and gives the roof a real cap.
     */
    public static void build(
            ServerLevel level,
            int centerX,
            int centerZ,
            int baseY,
            int radius,
            BlockState panelState,
            BlockState ribState
    ) {
        if (radius < 2) {
            throw new IllegalArgumentException("Arena dome radius must be at least 2");
        }

        BlockState lampState = Blocks.REDSTONE_LAMP.defaultBlockState()
                .setValue(BlockStateProperties.LIT, true);

        for (int dy = 0; dy <= radius; dy++) {
            double outerRadius = sliceRadius(radius, dy);
            double innerRadius = dy >= radius - 1 ? 0.0 : sliceRadius(radius, dy + 1);
            int bounds = (int) Math.ceil(outerRadius + EDGE_PADDING);
            boolean isCollar = dy == 0;
            boolean isCap = dy >= radius - 1;

            for (int dx = -bounds; dx <= bounds; dx++) {
                for (int dz = -bounds; dz <= bounds; dz++) {
                    double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                    if (distance > outerRadius + EDGE_PADDING
                            || distance < Math.max(0.0, innerRadius - EDGE_PADDING)) {
                        continue;
                    }

                    boolean onRib = dx == 0 || dz == 0;
                    boolean isLamp = dx % 4 == 0 && dz % 4 == 0 && !onRib;

                    BlockState state;
                    if (isCollar) {
                        // A full masonry collar visibly seats the dome on the walls.
                        state = ribState;
                    } else if (isCap) {
                        // Keep the apex uniform and fully sealed.
                        state = panelState;
                    } else if (isLamp) {
                        state = lampState;
                    } else if (onRib) {
                        state = ribState;
                    } else {
                        state = panelState;
                    }

                    level.setBlock(new BlockPos(centerX + dx, baseY + dy, centerZ + dz), state, 2);
                }
            }
        }
    }

    /** Matches the dome collar's footprint so arena walls meet it cleanly. */
    public static boolean isWallRing(int x, int z, int centerX, int centerZ, int radius) {
        double dx = x - centerX;
        double dz = z - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        return distance >= radius - 1.0 && distance <= radius + EDGE_PADDING;
    }

    private static double sliceRadius(int radius, int dy) {
        double normalizedY = (double) dy / radius;
        return radius * Math.sqrt(Math.max(0.0, 1.0 - normalizedY * normalizedY));
    }
}
