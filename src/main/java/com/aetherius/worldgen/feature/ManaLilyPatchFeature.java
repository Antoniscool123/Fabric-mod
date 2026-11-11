package com.aetherius.worldgen.feature;

import com.aetherius.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Starlit Meadow patch: converts a small disc of grass into Starlit Grass and scatters Mana
 * Lilies across it. Used by the Starlit Meadows biome JSON and as a rare overworld easter egg
 * near observatories.
 */
public class ManaLilyPatchFeature extends Feature<DefaultFeatureConfig> {

    public ManaLilyPatchFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG,
                context.getOrigin());
        if (origin.getY() < world.getBottomY() + 2) {
            return false;
        }
        int radius = 2 + random.nextInt(3);
        boolean placedAny = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                BlockPos floor = origin.add(dx, -1, dz);
                if (world.getBlockState(floor).isOf(Blocks.GRASS_BLOCK)
                        || world.getBlockState(floor).isOf(Blocks.DIRT)) {
                    world.setBlockState(floor, ModBlocks.STARLIT_GRASS_BLOCK.getDefaultState(),
                            Block.NOTIFY_LISTENERS);
                    BlockPos above = floor.up();
                    if (world.getBlockState(above).isAir() && random.nextFloat() < 0.45f) {
                        world.setBlockState(above, ModBlocks.MANA_LILY.getDefaultState(),
                                Block.NOTIFY_LISTENERS);
                        placedAny = true;
                    }
                }
            }
        }
        if (placedAny && random.nextFloat() < 0.12f) {
            world.setBlockState(origin, ModBlocks.AETHER_FOUNTAIN.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
        return placedAny;
    }
}
