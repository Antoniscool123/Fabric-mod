package com.aetherius.block;

import com.aetherius.registry.ModBlocks;
import com.aetherius.world.blight.CelestialBlightManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Starlit Grass: the Starlit Meadows surface block.
 *
 * <p>Two behaviours: in a blighted chunk it rots into Star-Ash, and in a clean chunk it
 * occasionally seeds a Mana Lily on top of itself. This makes the biome visually dynamic
 * without needing per-chunk block palettes.</p>
 */
public class StarlitGrassBlock extends Block {

    public StarlitGrassBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (CelestialBlightManager.get().isChunkBlighted(world, pos)) {
            if (random.nextFloat() < 0.35f) {
                world.setBlockState(pos, ModBlocks.STAR_ASH.getDefaultState());
            }
            return;
        }
        if (random.nextFloat() < 0.02f && world.isAir(pos.up())) {
            world.setBlockState(pos.up(), ModBlocks.MANA_LILY.getDefaultState());
        }
    }
}
