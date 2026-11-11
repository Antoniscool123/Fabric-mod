package com.aetherius.block;

import com.aetherius.block.entity.AetherAltarBlockEntity;
import com.aetherius.registry.ModBlockEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Aetherial Altar of Purification - 5x5x5 controller.
 *
 * <p>Interaction order: validate structure -> offer rite -> sanity-check payment -> purge
 * blight in a 3-chunk radius -> advance the Act II quest chain. All state lives in the
 * block entity.</p>
 */
public class AetherAltarBlock extends BlockWithEntity {

    private static final VoxelShape SHAPE = createCuboidShape(0, 0, 0, 16, 12, 16);

    public AetherAltarBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AetherAltarBlockEntity(pos, state);
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
        if (world.getBlockEntity(pos) instanceof AetherAltarBlockEntity altar) {
            return altar.performRite(player, hand);
        }
        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!world.isClient() && !state.isOf(newState.getBlock())
                && world.getBlockEntity(pos) instanceof AetherAltarBlockEntity altar) {
            altar.onControllerBroken();
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                 BlockEntityType<T> type) {
        return world.isClient ? null
                : checkType(type, ModBlockEntities.AETHER_ALTAR, AetherAltarBlockEntity::tick);
    }

    /** Helper for the quest system / debug commands. */
    public static boolean isPlayerInsideChamber(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        for (BlockPos candidate : BlockPos.iterate(pos.add(-2, -2, -2), pos.add(2, 2, 2))) {
            if (player.getWorld().getBlockEntity(candidate) instanceof AetherAltarBlockEntity) {
                return true;
            }
        }
        return false;
    }
}
