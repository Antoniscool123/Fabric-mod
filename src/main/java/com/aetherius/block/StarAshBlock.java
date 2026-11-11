package com.aetherius.block;

import com.aetherius.registry.ModParticles;
import com.aetherius.world.blight.CelestialBlightManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Star-Ash: what grass becomes inside a blighted chunk.
 *
 * <p>Mechanically it is a hazard floor - it slows and poisons anything standing on it - and
 * it is the visible "spread" vector of Celestial Blight. Once a chunk is purified, star ash
 * slowly reverts to starlit grass instead of spreading.</p>
 */
public class StarAshBlock extends Block {

    public StarAshBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient() && entity instanceof LivingEntity living && world.getTime() % 20 == 0) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, false, false, false));
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 30, 0, false, false, false));
            living.damage(living.getDamageSources().magic(), 1.0f);
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        CelestialBlightManager.get().tickStarAsh(world, pos, random);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(10) == 0) {
            world.addParticle(ModParticles.BLIGHT_SPORE,
                    pos.getX() + random.nextDouble(), pos.getY() + 0.1,
                    pos.getZ() + random.nextDouble(), 0.0, 0.01, 0.0);
        }
    }
}
