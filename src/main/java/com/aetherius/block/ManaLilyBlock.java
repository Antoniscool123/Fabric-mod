package com.aetherius.block;

import com.aetherius.api.magic.MagicComponents;
import com.aetherius.registry.ModParticles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Mana Lily: a cross-shaped flora that tops up a small amount of mana when stepped on. */
public class ManaLilyBlock extends PlantBlock {

    private static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public ManaLilyBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(com.aetherius.registry.ModBlocks.STARLIT_GRASS_BLOCK)
                || floor.isOf(Blocks.GRASS_BLOCK) || floor.isOf(Blocks.DIRT)
                || floor.isOf(Blocks.COARSE_DIRT) || floor.isOf(Blocks.MOSS_BLOCK);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient() && entity instanceof ServerPlayerEntity player && world.getTime() % 40 == 0) {
            MagicComponents.get(player).addMana(3f);
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(12) == 0) {
            world.addParticle(ModParticles.STAR_DUST,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                    pos.getY() + 0.6,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                    0.0, 0.02, 0.0);
        }
    }
}
