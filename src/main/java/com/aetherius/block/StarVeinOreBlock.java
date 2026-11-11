package com.aetherius.block;

import com.aetherius.api.chronicle.ChronicleManager;
import com.aetherius.world.StarfallManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Star-Vein Ore - the inciting block of Act I.
 *
 * <p>Breaking it on the server triggers a localized <b>Star-fall</b>: a short meteor shower
 * over the surrounding 48 blocks, a shockwave, and a guaranteed Aether-Blood scatter.
 * The ore itself also glows faintly so players find it while caving.</p>
 */
public class StarVeinOreBlock extends Block {

    public StarVeinOreBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            ChronicleManager.get().onStarVeinBroken(serverPlayer);
            StarfallManager.get().triggerStarfall(serverPlayer.getServerWorld(), pos, 48);
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(8) == 0) {
            world.addParticle(com.aetherius.registry.ModParticles.STAR_DUST,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 1.05,
                    pos.getZ() + random.nextDouble(),
                    0.0, 0.02, 0.0);
        }
    }
}
