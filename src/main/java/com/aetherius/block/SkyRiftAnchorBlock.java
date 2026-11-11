package com.aetherius.block;

import com.aetherius.block.entity.SkyRiftAnchorBlockEntity;
import com.aetherius.registry.ModBlockEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * One of the four Sky-Rift anchors. Right-click with an Elemental Heart to socket it.
 * When all four anchors within a 12-block radius hold four <i>distinct</i> hearts, the
 * portal opens in the middle of the ring (Act III trigger).
 */
public class SkyRiftAnchorBlock extends BlockWithEntity {

    public SkyRiftAnchorBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SkyRiftAnchorBlockEntity(pos, state);
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
        ItemStack held = player.getStackInHand(hand);
        if (world.getBlockEntity(pos) instanceof SkyRiftAnchorBlockEntity anchor) {
            if (anchor.hasHeart()) {
                player.sendMessage(net.minecraft.text.Text.translatable("aetherius.anchor.filled")
                        .formatted(net.minecraft.util.Formatting.GRAY), true);
                return ActionResult.CONSUME;
            }
            if (anchor.insertHeart(held, player)) {
                if (!player.getAbilities().creativeMode) {
                    held.decrement(1);
                }
                return ActionResult.CONSUME;
            }
            player.sendMessage(net.minecraft.text.Text.translatable("aetherius.anchor.needs_heart")
                    .formatted(net.minecraft.util.Formatting.GRAY), true);
        }
        return ActionResult.PASS;
    }
}
