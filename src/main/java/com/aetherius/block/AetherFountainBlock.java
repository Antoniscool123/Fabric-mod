package com.aetherius.block;

import com.aetherius.api.magic.MagicComponents;
import com.aetherius.registry.ModParticles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Aether Fountain - the mana anchor of the mod.
 *
 * <p>Passive effect: standing within 6 blocks triples mana regeneration (the capability
 * scans for this block once every 5 ticks). Active effect: touching it tops up a chunk of
 * mana instantly and vents a little Tension, giving players a reason to route exploration
 * through fountains instead of chugging elixirs.</p>
 */
public class AetherFountainBlock extends Block {

    public AetherFountainBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                              BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        var magic = MagicComponents.get(player);
        magic.addMana(12f);
        magic.setAstralTension(Math.max(0f, magic.getAstralTension() - 2f));
        if (player instanceof ServerPlayerEntity serverPlayer) {
            com.aetherius.network.AetherNetworking.syncMagic(serverPlayer, magic);
            serverPlayer.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    com.aetherius.registry.ModStatusEffects.MANA_VOLATILITY, 20, 0, false, false, false));
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient() && entity instanceof ServerPlayerEntity player && world.getTime() % 10 == 0) {
            MagicComponents.get(player).addMana(1.5f);
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        for (int i = 0; i < 3; i++) {
            world.addParticle(ModParticles.ASTRAL_SPARK,
                    pos.getX() + 0.25 + random.nextDouble() * 0.5,
                    pos.getY() + 0.9 + random.nextDouble() * 0.4,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                    0.0, 0.06, 0.0);
        }
    }
}
