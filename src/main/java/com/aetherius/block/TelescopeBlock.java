package com.aetherius.block;

import com.aetherius.block.entity.TelescopeBlockEntity;
import com.aetherius.registry.ModBlockEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The broken Telescope of the Ancients, found inside naturally generated Starlight
 * Observatories. Repairing it (4 Aether Shards + 1 Aether Blood) turns it into the
 * moon-phase rune vendor: it teaches one tier-III rune per Minecraft day, chosen by the
 * current moon phase.
 */
public class TelescopeBlock extends BlockWithEntity {

    private static final VoxelShape SHAPE = createCuboidShape(2, 0, 2, 14, 16, 14);

    public TelescopeBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TelescopeBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                              BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof TelescopeBlockEntity telescope) {
            return telescope.interact(player);
        }
        return ActionResult.PASS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                 BlockEntityType<T> type) {
        return world.isClient ? null
                : checkType(type, ModBlockEntities.TELESCOPE, TelescopeBlockEntity::tick);
    }
}
