package com.aetherius.block;

import com.aetherius.registry.ModParticles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Structural pillar for the Cosmic Forge and Aetherial Altar. Emits slow drifting motes. */
public class CelestialPillarBlock extends Block {

    public CelestialPillarBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(14) == 0) {
            world.addParticle(ModParticles.STAR_DUST,
                    pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                    pos.getZ() + random.nextDouble(), 0.0, 0.01, 0.0);
        }
    }
}
