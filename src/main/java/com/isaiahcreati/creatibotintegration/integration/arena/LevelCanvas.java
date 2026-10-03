package com.isaiahcreati.creatibotintegration.integration.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Writes straight into a level without neighbor updates, so water stays where
 * it is placed, lamps stay lit, and nothing pops off during a build.
 */
public final class LevelCanvas implements ArenaCanvas {

    private final ServerLevel level;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public LevelCanvas(ServerLevel level) {
        this.level = level;
    }

    @Override
    public BlockState get(int x, int y, int z) {
        return level.getBlockState(cursor.set(x, y, z));
    }

    @Override
    public void set(int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS);
    }
}
