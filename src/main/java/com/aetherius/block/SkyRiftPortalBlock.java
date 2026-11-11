package com.aetherius.block;

import com.aetherius.dimension.SkyRiftTeleporter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The Sky-Rift portal surface. Walking into it (or clicking it) moves you between the
 * Overworld and the Shattered Sky. Uses the vanilla portal-cooldown field so players do
 * not ping-pong between dimensions.
 */
public class SkyRiftPortalBlock extends Block {

    public SkyRiftPortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient() && entity instanceof ServerPlayerEntity player && player.getPortalCooldown() <= 0) {
            SkyRiftTeleporter.travel(player);
        }
        super.onEntityCollision(state, world, pos, entity);
    }

    @Override
    public net.minecraft.util.ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                                net.minecraft.util.Hand hand, net.minecraft.util.hit.BlockHitResult hit) {
        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            SkyRiftTeleporter.travel(serverPlayer);
            return net.minecraft.util.ActionResult.CONSUME;
        }
        return net.minecraft.util.ActionResult.SUCCESS;
    }
}
