package com.isaiahcreati.creatibotintegration.integration.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/** Records an arena build in memory so tests can inspect and render it. */
final class MemoryCanvas implements ArenaCanvas {

    final Map<BlockPos, BlockState> blocks = new HashMap<>();

    @Override
    public BlockState get(int x, int y, int z) {
        return blocks.getOrDefault(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
    }

    @Override
    public void set(int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        if (state.isAir()) {
            blocks.remove(pos);
        } else {
            blocks.put(pos, state);
        }
    }
}
