package com.aetherius.block;

import com.aetherius.registry.ModParticles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Rune Lantern: full-bright light source whose only real purpose is Nox'Thal Phase 3.
 * When the Supernova darkness lands, these are the only navigational beacons players can
 * place in advance - and the boss's star-strikes target them, so spacing matters.
 */
public class RuneLanternBlock extends Block {

    public RuneLanternBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(6) == 0) {
            world.addParticle(ModParticles.ASTRAL_SPARK,
                    pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                    pos.getZ() + random.nextDouble(), 0.0, 0.03, 0.0);
        }
    }
}
