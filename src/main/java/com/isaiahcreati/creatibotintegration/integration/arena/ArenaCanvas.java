package com.isaiahcreati.creatibotintegration.integration.arena;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Where arena builders place blocks. In game this writes to the minigame
 * level; in tests it records into memory so layouts can be checked and
 * rendered without a running server.
 */
public interface ArenaCanvas {

    BlockState get(int x, int y, int z);

    void set(int x, int y, int z, BlockState state);

    default void set(int x, int y, int z, Block block) {
        set(x, y, z, block.defaultBlockState());
    }

    default boolean isAir(int x, int y, int z) {
        return get(x, y, z).isAir();
    }

    /** Fills the inclusive box between the two corners. */
    default void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    default void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        fill(x1, y1, z1, x2, y2, z2, block.defaultBlockState());
    }

    default void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.defaultBlockState());
    }
}
